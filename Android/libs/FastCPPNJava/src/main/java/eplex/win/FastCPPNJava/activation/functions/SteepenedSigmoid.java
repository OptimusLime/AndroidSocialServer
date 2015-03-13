package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class SteepenedSigmoid implements ActivationFunction {
    @Override
    public String functionID() {
        return "SteepenedSigmoid";
    }

    @Override
    public double calculate(double val) {
        return 1.0/(1.0+(Math.exp(-4.9*val)));
    }

    @Override
    public String gpuFunctionString() {
        return "1.0/(1.0+(exp(-4.9*val)));";
    }
}
