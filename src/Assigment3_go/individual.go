package main

import "math/rand"

const (
	GeneSize    = 1000
	WeightLimit = 300
)

var (
	Values  [GeneSize]int
	Weights [GeneSize]int
)

// Inicializa os valores/pesos (tipo static { } em Java)
func InitProblem() {
	r := rand.New(rand.NewSource(1)) // seed fixa como no Random(1L)
	for i := 0; i < GeneSize; i++ {
		Values[i] = r.Intn(100)  // 0..99
		Weights[i] = r.Intn(100) // 0..99
	}
}

type Individual struct {
	Genes   []bool
	Fitness int
}

func NewRandomIndividual(r *rand.Rand) Individual {
	genes := make([]bool, GeneSize)
	for i := 0; i < GeneSize; i++ {
		genes[i] = r.Intn(2) == 1 // 0 ou 1
	}
	return Individual{Genes: genes, Fitness: 0}
}

// Versão com penalização (para evitar tudo ficar a 0)
func (ind *Individual) MeasureFitness() {
	totalWeight := 0
	totalValue := 0
	for i, selected := range ind.Genes {
		if selected {
			totalValue += Values[i]
			totalWeight += Weights[i]
		}
	}

	if totalWeight > WeightLimit {
		// penaliza excesso de peso
		ind.Fitness = -(totalWeight - WeightLimit)
	} else {
		ind.Fitness = totalValue
	}
}

// ---- Operadores genéticos ----

func Crossover(r *rand.Rand, a, b Individual) Individual {
	childGenes := make([]bool, GeneSize)
	point := r.Intn(GeneSize)
	for i := 0; i < GeneSize; i++ {
		if i < point {
			childGenes[i] = a.Genes[i]
		} else {
			childGenes[i] = b.Genes[i]
		}
	}
	return Individual{Genes: childGenes, Fitness: 0}
}

func Mutate(r *rand.Rand, ind *Individual) {
	idx := r.Intn(GeneSize)
	ind.Genes[idx] = !ind.Genes[idx]
}
