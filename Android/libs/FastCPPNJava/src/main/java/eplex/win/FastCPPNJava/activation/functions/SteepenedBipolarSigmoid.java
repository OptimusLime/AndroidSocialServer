package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class SteepenedBipolarSigmoid implements ActivationFunction {

    @Override
    public String functionID() {
        return "SteepenedBipolarSigmoid";
    }

    @Override
    public double calculate(double val) {
        return (2.0 / (1.0 + Math.exp(-4.9 * val))) - 1.0;
    }

    @Override
    public String gpuFunctionString() {
        return "(2.0 / (1.0 + exp(-4.9 * val))) - 1.0;";
    }
}
