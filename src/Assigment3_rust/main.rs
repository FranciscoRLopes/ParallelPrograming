use rand::Rng;
use std::sync::{mpsc, Arc, Mutex};
use std::thread;

const POP_SIZE: usize = 1000;
const GENE_LEN: usize = 64;
const N_WORKERS: usize = 8;
const N_GENERATIONS: usize = 50;

#[derive(Clone, Debug)]
struct Individual {
    genes: Vec<f64>,
    fitness: f64,
}

fn main() {
    // --- inicializar população ---
    let mut population = init_population(POP_SIZE, GENE_LEN);

    for gen in 0..N_GENERATIONS {
        // --- AVALIAÇÃO EM PARALELO ---
        evaluate_population_parallel(&mut population, N_WORKERS);

        // Melhor fitness da geração atual
        let best = population
            .iter()
            .map(|ind| ind.fitness)
            .fold(f64::NEG_INFINITY, f64::max);

        println!("Geração {gen}: melhor fitness = {best:.4}");

        // --- criar próxima geração (super simples, só para exemplo) ---
        population = next_generation(&population);
    }
}

// -----------------------------------------------------------
// Inicialização da população
// -----------------------------------------------------------
fn init_population(pop_size: usize, gene_len: usize) -> Vec<Individual> {
    let mut rng = rand::thread_rng();

    (0..pop_size)
        .map(|_| {
            let genes = (0..gene_len)
                .map(|_| rng.gen_range(0.0..1.0)) // genes ∈ [0, 1)
                .collect();
            Individual { genes, fitness: 0.0 }
        })
        .collect()
}

// -----------------------------------------------------------
// Função de fitness (podes trocar pelo teu problema real)
// Aqui: maximizar a soma dos genes
// -----------------------------------------------------------
fn fitness_function(genes: &[f64]) -> f64 {
    genes.iter().sum()
}

// -----------------------------------------------------------
// Criação da próxima geração (muito simples, só p/ exemplo)
// - Faz seleção por torneio de tamanho 3
// - Crossover de 1 ponto
// - Mutação com pequena probabilidade
// -----------------------------------------------------------
fn next_generation(population: &[Individual]) -> Vec<Individual> {
    let mut rng = rand::thread_rng();
    let mut new_pop = Vec::with_capacity(population.len());

    while new_pop.len() < population.len() {
        // seleção por torneio
        let p1 = tournament_selection(population, 3, &mut rng);
        let p2 = tournament_selection(population, 3, &mut rng);

        let (mut c1_genes, mut c2_genes) = one_point_crossover(&p1.genes, &p2.genes, &mut rng);

        // mutação simples
        mutate(&mut c1_genes, 0.01, &mut rng);
        mutate(&mut c2_genes, 0.01, &mut rng);

        new_pop.push(Individual {
            genes: c1_genes,
            fitness: 0.0,
        });

        if new_pop.len() < population.len() {
            new_pop.push(Individual {
                genes: c2_genes,
                fitness: 0.0,
            });
        }
    }

    new_pop
}

fn tournament_selection<'a, R: Rng>(
    population: &'a [Individual],
    k: usize,
    rng: &mut R,
) -> &'a Individual {
    let mut best: Option<&Individual> = None;

    for _ in 0..k {
        let idx = rng.gen_range(0..population.len());
        let cand = &population[idx];

        best = match best {
            None => Some(cand),
            Some(current_best) => {
                if cand.fitness > current_best.fitness {
                    Some(cand)
                } else {
                    Some(current_best)
                }
            }
        };
    }

    best.unwrap()
}

fn one_point_crossover<R: Rng>(
    g1: &[f64],
    g2: &[f64],
    rng: &mut R,
) -> (Vec<f64>, Vec<f64>) {
    assert_eq!(g1.len(), g2.len());
    let len = g1.len();
    if len == 0 {
        return (Vec::new(), Vec::new());
    }

    let point = rng.gen_range(1..len); // ponto de corte entre 1 e len-1

    let mut c1 = Vec::with_capacity(len);
    let mut c2 = Vec::with_capacity(len);

    c1.extend_from_slice(&g1[..point]);
    c1.extend_from_slice(&g2[point..]);

    c2.extend_from_slice(&g2[..point]);
    c2.extend_from_slice(&g1[point..]);

    (c1, c2)
}

fn mutate<R: Rng>(genes: &mut [f64], prob: f64, rng: &mut R) {
    for g in genes.iter_mut() {
        if rng.gen::<f64>() < prob {
            // pequena perturbação
            let delta = rng.gen_range(-0.1..0.1);
            *g += delta;
        }
    }
}

// -----------------------------------------------------------
// Avaliação em paralelo com pool de workers
// -----------------------------------------------------------
fn evaluate_population_parallel(population: &mut [Individual], n_workers: usize) {
    // Canal de jobs: (index do indivíduo, genes)
    let (job_tx, job_rx) = mpsc::channel::<(usize, Vec<f64>)>();
    let job_rx = Arc::new(Mutex::new(job_rx));

    // Canal de resultados: (index, fitness)
    let (res_tx, res_rx) = mpsc::channel::<(usize, f64)>();

    // Guardar handles para fazermos join no fim
    let mut handles = Vec::with_capacity(n_workers);

    // --- criar workers (task parallelism) ---
    for _ in 0..n_workers {
        let job_rx_clone = Arc::clone(&job_rx);
        let res_tx_clone = res_tx.clone();

        let handle = thread::spawn(move || loop {
            // Cada worker tenta receber um job
            let msg = {
                let rx = job_rx_clone.lock().unwrap();
                rx.recv()
            };

            match msg {
                Ok((idx, genes)) => {
                    let fit = fitness_function(&genes);

                    // se o consumidor tiver morrido, saímos
                    if res_tx_clone.send((idx, fit)).is_err() {
                        break;
                    }
                }
                Err(_) => {
                    // canal fechado: não há mais trabalho
                    break;
                }
            }
        });

        handles.push(handle);
    }

    // este clone do sender de resultados já não é necessário no thread principal
    drop(res_tx);

    // --- enviar jobs ---
    for (idx, ind) in population.iter().enumerate() {
        job_tx
            .send((idx, ind.genes.clone()))
            .expect("worker threads morreram ao enviar job");
    }

    // fechar canal de jobs para sinalizar aos workers que acabou o trabalho
    drop(job_tx);

    // --- receber resultados ---
    let total = population.len();

    for _ in 0..total {
        let (idx, fit) = res_rx
            .recv()
            .expect("worker threads morreram ao enviar resultado");
        population[idx].fitness = fit;
    }

    // --- join das threads ---
    for h in handles {
        h.join().expect("não foi possível fazer join de um worker");
}
}
