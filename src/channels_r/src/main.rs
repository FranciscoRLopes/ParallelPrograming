use rand::{Rng, SeedableRng};
use std::sync::{mpsc, Arc, Mutex};
use std::thread;
use std::time::Instant;

const GENE_SIZE: usize = 1000;
const WEIGHT_LIMIT: i32 = 300;

const POP_SIZE: usize = 100_000;
const N_GENERATIONS: usize = 500;
const PROB_MUTATION: f64 = 0.5;
const TOURNAMENT_SIZE: usize = 3;

const N_WORKERS: usize = 12;

#[derive(Clone)]
struct Individual {
    selected_items: Vec<bool>,
    fitness: i32,
}

// ---------- problema ----------
fn init_problem() -> (Vec<i32>, Vec<i32>) {
    let mut rng = rand::rngs::StdRng::seed_from_u64(1);
    let mut values = Vec::with_capacity(GENE_SIZE);
    let mut weights = Vec::with_capacity(GENE_SIZE);

    for _ in 0..GENE_SIZE {
        values.push(rng.gen_range(0..100));
        weights.push(rng.gen_range(0..100));
    }

    (values, weights)
}

// ---------- criação população ----------
fn create_random_individual<R: Rng>(rng: &mut R) -> Individual {
    let selected_items = (0..GENE_SIZE)
        .map(|_| rng.gen_bool(0.5))
        .collect();

    Individual {
        selected_items,
        fitness: 0,
    }
}

fn init_population() -> Vec<Individual> {
    let mut rng = rand::thread_rng();
    (0..POP_SIZE)
        .map(|_| create_random_individual(&mut rng))
        .collect()
}

// ---------- fitness ----------
fn evaluate_population_parallel_channels(
    population: &mut [Individual],
    values: &[i32],
    weights: &[i32],
) {
    // Canal de jobs: (indice, genes)
    let (job_tx, job_rx) = mpsc::channel::<(usize, Vec<bool>)>();
    let job_rx = Arc::new(Mutex::new(job_rx));

    // Canal de resultados: (indice, fitness)
    let (res_tx, res_rx) = mpsc::channel::<(usize, i32)>();

    let values = Arc::new(values.to_vec());
    let weights = Arc::new(weights.to_vec());

    let mut handles = Vec::with_capacity(N_WORKERS);

    for _ in 0..N_WORKERS {
        let job_rx = Arc::clone(&job_rx);
        let res_tx = res_tx.clone();
        let values = Arc::clone(&values);
        let weights = Arc::clone(&weights);

        let handle = thread::spawn(move || loop {
            let msg = {
                let rx = job_rx.lock().unwrap();
                rx.recv()
            };

            match msg {
                Ok((idx, genes)) => {
                    let mut total_weight = 0;
                    let mut total_value = 0;

                    for i in 0..GENE_SIZE {
                        if genes[i] {
                            total_value += values[i];
                            total_weight += weights[i];
                        }
                    }

                    let fitness = if total_weight > WEIGHT_LIMIT {
                        -(total_weight - WEIGHT_LIMIT)
                    } else {
                        total_value
                    };

                    if res_tx.send((idx, fitness)).is_err() {
                        break;
                    }
                }
                Err(_) => break, // sem mais jobs
            }
        });

        handles.push(handle);
    }

    drop(res_tx);

    
    for (idx, ind) in population.iter().enumerate() {
        job_tx
            .send((idx, ind.selected_items.clone()))
            .expect("erro ao enviar job");
    }

    drop(job_tx);

    
    for _ in 0..population.len() {
        let (idx, fitness) = res_rx
            .recv()
            .expect("erro ao receber resultado");
        population[idx].fitness = fitness;
    }

    for h in handles {
        h.join().expect("erro ao fazer join");
    }
}


fn tournament<'a, R: Rng>(population: &'a [Individual], rng: &mut R) -> &'a Individual {
    let mut best = &population[rng.gen_range(0..population.len())];

    for _ in 0..TOURNAMENT_SIZE {
        let other = &population[rng.gen_range(0..population.len())];
        if other.fitness > best.fitness {
            best = other;
        }
    }

    best
}

fn crossover<R: Rng>(dad: &Individual, mom: &Individual, rng: &mut R) -> Individual {
    let crossover_point = rng.gen_range(0..GENE_SIZE);
    let mut selected_items = Vec::with_capacity(GENE_SIZE);

    for i in 0..GENE_SIZE {
        if i < crossover_point {
            selected_items.push(dad.selected_items[i]);
        } else {
            selected_items.push(mom.selected_items[i]);
        }
    }

    Individual {
        selected_items,
        fitness: 0,
    }
}

fn maybe_mutate<R: Rng>(ind: &mut Individual, rng: &mut R) {
    if rng.gen_range(0.0..1.0) < PROB_MUTATION {
        let mutation_point = rng.gen_range(0..GENE_SIZE);
        ind.selected_items[mutation_point] = !ind.selected_items[mutation_point];
    }
}

fn best_of_population<'a>(population: &'a [Individual]) -> &'a Individual {
    population
        .iter()
        .max_by_key(|ind| ind.fitness)
        .expect("população vazia")
}


fn main() {
    let (values, weights) = init_problem();
    let mut population = init_population();

    let t0 = Instant::now();

    for generation in 0..N_GENERATIONS {
        evaluate_population_parallel_channels(&mut population, &values, &weights);

        let best = best_of_population(&population);
        println!(
            "[CHAN] Geração {}: melhor fitness = {}",
            generation, best.fitness
        );

        let mut rng = rand::thread_rng();
        let mut new_population = Vec::with_capacity(POP_SIZE);
        new_population.push(best.clone());

        for _ in 1..POP_SIZE {
            let p1 = tournament(&population, &mut rng);
            let p2 = tournament(&population, &mut rng);
            let mut child = crossover(p1, p2, &mut rng);
            maybe_mutate(&mut child, &mut rng);
            new_population.push(child);
        }

        population = new_population;
    }

    let elapsed = t0.elapsed().as_secs_f64();
    println!("Tempo total CHAN ({} workers): {:.3} s", N_WORKERS, elapsed);
}
