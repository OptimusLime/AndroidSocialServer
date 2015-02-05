package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class NullFn implements ActivationFunction {
    @Override
    public String functionID() {
        return "NullFn";
    }

    @Override
    public double calculate(double val) {
        return 0.0;
    }
}
