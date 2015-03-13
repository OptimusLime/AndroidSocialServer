package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class Cos implements ActivationFunction {

    @Override
    public String functionID() {
        return "Cos";
    }

    @Override
    public double calculate(double val) {
        return Math.cos(val);
    }

    @Override
    public String gpuFunctionString() {
        return "cos(val);";
    }
}
