package Ex1;

public class MatrixCalculatorConcurent {
    // Method to perform matrix multiplication
    public static int[][] multiplyMatrices(int[][] A, int[][] B) {
        // Dimensions of the matrices
        int M = A.length; // Number of rows in A
        int N = A[0].length; // Number of columns in A
        int O = B[0].length; // Number of columns in B

        // Initialize the result matrix C with zeros (MxO)
        int[][] C = new int[M][O];

        // Perform matrix multiplication
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < O; j++) {
                for (int k = 0; k < N; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        return C;
    }

    // Method to print a matrix
    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        // Example matrices
        int[][] A = new int[1000][2000];
        int[][] B = new int[2000][1000];

        // Preencher as matrizes com números aleatórios entre 1 e 10
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 2000; j++) {
                A[i][j] = (int) (Math.random() * 10) + 1;
            }
        }

        for (int i = 0; i < 2000; i++) {
            for (int j = 0; j < 1000; j++) {
                B[i][j] = (int) (Math.random() * 10) + 1;
            }
        }


        long StartTime = System.nanoTime();

        int[][] result = multiplyMatrices(A, B);

        long EndTime = System.nanoTime();
        long duration = (EndTime - StartTime);
        double durationInSeconds = duration / 1_000_000_000.0;

        System.out.printf("Tempo: ; %4f%n", durationInSeconds);
        //System.out.println("Resultant Matrix:");
        //printMatrix(result);
    }
}
