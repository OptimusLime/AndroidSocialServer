package edu.eplex.AsyncEvolution.activations;

import eplex.win.FastCPPNJava.activation.ActivationFunction;

/**
 * Created by paul on 8/19/14.
 */
public class PBGaussian implements ActivationFunction {
    @Override
    public String functionID() {
        return "PBGaussian";
    }

    @Override
    public double calculate(double val) {
        return 2 * Math.exp(-Math.pow(val, 2)) - 1;
    }

    @Override
    public String gpuFunctionString() {
        return "2.0 * exp(-pow(val, 2.0)) - 1.0;";
    }
}