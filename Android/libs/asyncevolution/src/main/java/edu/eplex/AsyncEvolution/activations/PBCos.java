package edu.eplex.AsyncEvolution.activations;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/19/14.
 */
public class PBCos implements ActivationFunction {
    @Override
    public String functionID() {
        return "PBCos";
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
