package eplex.win.FastCPPNJava.activation.functions;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/18/14.
 */
public class StepFunction implements ActivationFunction {
    @Override
    public String functionID() {
        return "StepFunction";
    }

    @Override
    public double calculate(double val) {
        return (val<=0 ? 0.0 : 1.0);
    }
}
