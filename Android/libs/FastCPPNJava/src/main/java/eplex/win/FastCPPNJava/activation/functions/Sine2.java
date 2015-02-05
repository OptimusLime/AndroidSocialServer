package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class Sine2 implements ActivationFunction {
    @Override
    public String functionID() {
        return "Sine2";
    }

    @Override
    public double calculate(double val) {
        return Math.sin(2*val);
    }
}
