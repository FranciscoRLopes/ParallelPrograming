package main

import (
    "fmt"
    "time"
)

func main() {
	start := time.Now()
	runGA()
	elapsed := time.Since(start)
	fmt.Println("Tempo total: %v ms\n", elapsed.Milliseconds())
}
