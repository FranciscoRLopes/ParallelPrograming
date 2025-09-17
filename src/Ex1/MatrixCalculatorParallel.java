package Ex1;

public class MatrixCalculatorParallel {

    public static int[][] multiplyMatrices(int[][] A, int[][] B, int numThreads) throws InterruptedException {

        int M = A.length; // Número de linhas de A (e C)
        int N = A[0].length; // Número de colunas de A (e número de linhas de B)
        int O = B[0].length; // Número de colunas de B

        // Matriz resultante C (MxO), inicializada com zeros
        int[][] C = new int[M][O];

        // Número de threads a serem criadas, limitadas pelo número de linhas de A
        int threadsCount = Math.min(numThreads, M);
        Thread[] threads = new Thread[threadsCount];

        // Divisão do trabalho - atribuindo as linhas às threads
        int linesPerThread = M / threadsCount;
        int remainder = M % threadsCount;

        int currentLine = 0;
        for (int i = 0; i < threadsCount; i++) {
            final int startRow = currentLine;
            final int endRow = (i == threadsCount - 1) ? M : currentLine + linesPerThread + (i < remainder ? 1 : 0);

            // Criação da thread para calcular a linha (ou o conjunto de linhas)
            threads[i] = new Thread(() -> {
                for (int row = startRow; row < endRow; row++) {
                    for (int j = 0; j < O; j++) {
                        int sum = 0;
                        for (int k = 0; k < N; k++) {
                            sum += A[row][k] * B[k][j];
                        }
                        C[row][j] = sum;
                    }
                }
            });

            threads[i].start(); // Inicia a thread
            currentLine = endRow; // Atualiza a linha de início para a próxima thread
        }

        // Espera todas as threads terminarem
        for (int i = 0; i < threadsCount; i++) {
            threads[i].join(); // Espera cada thread terminar
        }

        return C;
    }

    // Função para imprimir uma matriz
    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        int sizelinhas = 1000;
        int sizecolunas = 2000;
         // Matrizes de exemplo
        int[][] A = new int[sizelinhas][sizecolunas];
        int[][] B = new int[sizecolunas][sizelinhas];

        // Preencher as matrizes com números aleatórios entre 1 e 10
        for (int i = 0; i < sizelinhas; i++) {
            for (int j = 0; j < sizecolunas; j++) {
                A[i][j] = (int) (Math.random() * 10) + 1;
            }
        }

        for (int i = 0; i < sizecolunas; i++) {
            for (int j = 0; j < sizelinhas; j++) {
                B[i][j] = (int) (Math.random() * 10) + 1;
            }
        }

        long StartTime = System.nanoTime();

        int[][] result = multiplyMatrices(A, B, 15);

        long EndTime = System.nanoTime();
        long duration = (EndTime - StartTime);
        String finalduration = String.valueOf(duration);
        System.out.println("Tempo de execução (em nanossegundos): " + finalduration);


        System.out.println("Matriz Resultante:");
        //printMatrix(result);
    }
}
