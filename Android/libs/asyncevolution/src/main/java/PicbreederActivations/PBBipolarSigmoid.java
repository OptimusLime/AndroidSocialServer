package PicbreederActivations;

import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;

/**
 * Created by paul on 8/19/14.
 */
public class PBBipolarSigmoid extends BipolarSigmoid {

    @Override
    public String functionID() {
        return "PBBipolarSigmoid";
    }
}
