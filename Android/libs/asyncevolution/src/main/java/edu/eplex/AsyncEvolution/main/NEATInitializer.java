package edu.eplex.AsyncEvolution.main;

import java.util.HashMap;
import java.util.Map;

import edu.eplex.AsyncEvolution.activations.PBBipolarSigmoid;
import edu.eplex.AsyncEvolution.activations.PBCos;
import edu.eplex.AsyncEvolution.activations.PBGaussian;
import edu.eplex.AsyncEvolution.activations.pbLinear;
import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastNEATJava.utils.NeatParameters;

/**
 * Created by paul on 10/20/14.
 */
public class NEATInitializer {

    static Map<String, Double> probs = new HashMap<String, Double>();
    public static void InitializeActivationFunctions()
    {
        //only initialize once
        if(probs.size() == 0) {
            probs.put(PBBipolarSigmoid.class.getName(), .22);
            probs.put(PBGaussian.class.getName(), .22);
            probs.put(Sine.class.getName(), .22);
            probs.put(PBCos.class.getName(), .22);
            probs.put(pbLinear.class.getName(), .12);

            //now we set up our probabilities of generating particular activation functions
            CPPNActivationFactory.setProbabilities(probs);
        }
    }
    public static NeatParameters DefaultNEATParameters()
    {
        //no recurrent networks please!
        NeatParameters np = new NeatParameters();
        //set up the defaults here
        np.pMutateAddConnection = .13;
        np.pMutateAddNode = .12;
        np.pMutateDeleteSimpleNeuron = .005;
        np.pMutateDeleteConnection = .005;
        np.pMutateConnectionWeights = .72;
        np.pMutateChangeActivations = .02;
        np.pNodeMutateActivationRate = 0.2;
        np.postSexualMutations = 5;
        np.postAsexualMutations = 5;
        np.connectionWeightRange = .8;
        np.disallowRecurrence = true;
        return  np;
    }

}
