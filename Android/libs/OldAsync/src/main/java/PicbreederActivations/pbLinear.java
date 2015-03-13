package PicbreederActivations;

import eplex.win.FastCPPNJava.activation.ActivationFunction;
import eplex.win.FastCPPNJava.activation.functions.Linear;

/**
 * Created by paul on 8/19/14.
 */
public class pbLinear extends Linear {
    @Override
    public String functionID() {
        return "pbLinear";
    }
}
