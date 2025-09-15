package Ex1;

public class TrapezoidConcurrent {

    // Interface funcional para receber a função f(x)
    @FunctionalInterface
    interface Function {
        double apply(double x);
    }

    // Método que calcula a integral pelo método do trapézio
    public static double trapezoidIntegral(Function f, double a, double b, double resolution) {
        int n = (int) ((b - a) / resolution); // número de subintervalos
        double total = 0.0;

        for (int i = 0; i < n; i++) {
            double x0 = a + i * resolution;
            double x1 = x0 + resolution;
            total += 0.5 * (f.apply(x0) + f.apply(x1)) * resolution;
        }

        return total;
    }

    // Exemplo de uso
    public static void main(String[] args) {
        // f(x) = x * (x - 1)
        Function f = (x) -> x * (x - 1);

        double result = trapezoidIntegral(f, 0.0, 1.0, 1e-7);

        System.out.println("Integral aproximada: " + result);
    }
}