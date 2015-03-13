package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class Gaussian implements ActivationFunction {
    @Override
    public String functionID() {
        return "Gaussian";
    }

    @Override
    public double calculate(double val) {
        return 2 * Math.exp(-Math.pow(val * 2.5, 2)) - 1;
    }

    @Override
    public String gpuFunctionString() {
        return "2.0 * exp(-pow(val * 2.5, 2.0)) - 1.0;";
    }
}
