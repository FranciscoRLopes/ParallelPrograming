package main

import (
    "fmt"
    "math/rand"
    "time"
)

const (
    PopSize        = 100000
    NGenerations   = 500
    ProbMutation   = 0.5
    TournamentSize = 3
)

// Ator: só avalia fitness
func fitnessWorker(jobs <-chan Individual, results chan<- Individual) {
    for ind := range jobs {
        ind.MeasureFitness()
        results <- ind
    }
}

func tournamentSelect(r *rand.Rand, pop []Individual) Individual {
    best := pop[r.Intn(len(pop))]
    for i := 1; i < TournamentSize; i++ {
        candidate := pop[r.Intn(len(pop))]
        if candidate.Fitness > best.Fitness {
            best = candidate
        }
    }
    return best
}

func bestOfPopulation(pop []Individual) Individual {
    best := pop[0]
    for _, ind := range pop {
        if ind.Fitness > best.Fitness {
            best = ind
        }
    }
    return best
}

func runGA() {
    // 1) problema
    InitProblem()

    r := rand.New(rand.NewSource(time.Now().UnixNano()))

    // 2) população inicial
    population := make([]Individual, PopSize)
    for i := 0; i < PopSize; i++ {
        population[i] = NewRandomIndividual(r)
    }

    // 3) channels + workers
    fitnessJobs := make(chan Individual)
    fitnessResults := make(chan Individual)

    nWorkers := 8 // ou runtime.NumCPU()
    for i := 0; i < nWorkers; i++ {
        go fitnessWorker(fitnessJobs, fitnessResults)
    }

    // 4) gerações
    for gen := 0; gen < NGenerations; gen++ {
        // ---- avaliação paralela ----

        // Enviar jobs numa goroutine separada
        go func(pop []Individual, jobs chan<- Individual) {
            for _, ind := range pop {
                jobs <- ind
            }
        }(population, fitnessJobs)

        // Receber resultados na goroutine principal
        evaluated := make([]Individual, 0, PopSize)
        for i := 0; i < PopSize; i++ {
            evaluated = append(evaluated, <-fitnessResults)
        }
        population = evaluated

        best := bestOfPopulation(population)
        fmt.Printf("Generation %d: best fitness = %d\n", gen, best.Fitness)

        // ---- reprodução (sequencial) ----
        newPop := make([]Individual, PopSize)
        for i := 0; i < PopSize; i++ {
            p1 := tournamentSelect(r, population)
            p2 := tournamentSelect(r, population)
            child := Crossover(r, p1, p2)
            if r.Float64() < ProbMutation {
                Mutate(r, &child)
            }
            newPop[i] = child
        }

        population = newPop
    }

    // 5) avaliação final
    for i := range population {
        population[i].MeasureFitness()
    }
    best := bestOfPopulation(population)
    fmt.Printf("Best final fitness: %d\n", best.Fitness)
}

