package eplex.win.FastCPPNJava.activation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eplex.win.FastCPPNJava.utils.MathUtils;

/**
 * Created by paul on 8/17/14.
 */
public class CPPNActivationFactory {

    public static ArrayList<ActivationFunction> functions = new ArrayList<ActivationFunction>();
    public static double[] probabilities;
    public static Map<String, ActivationFunction> functionTable = new HashMap<String, ActivationFunction>();

    public static ActivationFunction createActivationFunction(String functionID)
    {
        if(functionTable.containsKey(functionID))
            return functionTable.get(functionID);

        try {

            Class<?> clazz;

            if(functionID.contains("."))
                clazz = Class.forName(functionID);
            else
                clazz = Class.forName("eplex.win.FastCPPNJava.activation.functions." + functionID);

//            Class<?> clazz = Class.forName("eplex.win.FastCPPNJava.activation.functions." + functionID);
            ActivationFunction aFunction = (ActivationFunction)clazz.newInstance();

            // For now the function ID is the name of a class that implements IActivationFunction.
            functionTable.put(functionID, aFunction);
            functionTable.put(aFunction.getClass().getSimpleName(), aFunction);

            return aFunction;

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Activation Function doesn't exist: " + functionID);
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException("Activation Function doesn't exist: " + functionID);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Activation Function doesn't exist: " + functionID);
        }
    }

    //basically just a pass through -- get or create activaiton funciton
    public static ActivationFunction getActivationFunction(String functionID)
    {
        return createActivationFunction(functionID);
    }

    public static void setProbabilities(Map<String, Double> oProbs)
    {
        int probSize = oProbs.keySet().size();
        probabilities = new double[probSize];

        int ix = 0;
        for(String key : oProbs.keySet())
        {
            probabilities[ix++] = oProbs.get(key);
            functions.add(getActivationFunction(key));
        }
    }
    static void defaultProbabilities()
    {
        HashMap<String, Double> oProbs = new HashMap<String, Double>();
        oProbs.put("BipolarSigmoid", .25);
        oProbs.put("Sine", .25);
        oProbs.put("Gaussian", .25);
        oProbs.put("Linear", .25);
        setProbabilities(oProbs);
    }

    public static ActivationFunction getRandomActivationObject()
    {
        if(probabilities == null)
            defaultProbabilities();

        return functions.get(MathUtils.singleThrowArray(probabilities));
    }

    public static String getRandomActivationFunction()
    {
        if(probabilities == null)
           defaultProbabilities();

        return functions.get(MathUtils.singleThrowArray(probabilities)).functionID();
    }
}
