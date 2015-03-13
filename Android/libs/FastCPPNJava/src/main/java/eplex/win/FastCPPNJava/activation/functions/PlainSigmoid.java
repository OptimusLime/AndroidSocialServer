package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class PlainSigmoid implements ActivationFunction {
    @Override
    public String functionID() {
        return "PlainSigmoid";
    }

    @Override
    public double calculate(double val) {
        return 1.0/(1.0+(Math.exp(-val)));
    }

    @Override
    public String gpuFunctionString() {
        return "1.0/(1.0+(exp(-val)));";
    }
}
