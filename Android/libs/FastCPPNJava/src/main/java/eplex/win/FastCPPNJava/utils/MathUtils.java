package eplex.win.FastCPPNJava.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MathUtils
{

    public static double nextDouble()
    {
        return Math.random();
    }
    public static int next(int range)
    {
        return (int)Math.floor(Math.random()*range);
    }

    public static double tanh(double arg)
    {
        // sinh(number)/cosh(number)
        return (Math.exp(arg) - Math.exp(-arg)) / (Math.exp(arg) + Math.exp(-arg));
    }


    public static int sign(double input)
    {
        if (input < 0) {return -1;}
        if (input > 0) {return 1;}
        return 0;
    }
    public static int sign(int input)
    {
        if (input < 0) {return -1;}
        if (input > 0) {return 1;}
        return 0;
    }

    //ROULETTE WHEEL functions

    /// <summary>
    /// A simple single throw routine.
    /// </summary>
    /// <param name="probability">A probability between 0..1 that the throw will result in a true result.</param>
    /// <returns></returns>
    public static boolean singleThrow(double probability)
    {
        return (Math.random() <= probability);
    }

    /// <summary>
    /// Performs a single throw for a given number of outcomes with equal probabilities.
    /// </summary>
    /// <param name="numberOfOutcomes"></param>
    /// <returns>An integer between 0..numberOfOutcomes-1. In effect this routine selects one of the possible outcomes.</returns>

    public static int singleThrowEven(int numberOfOutcomes) {
        double probability = 1.0 / numberOfOutcomes;
        double accumulator = 0;
        double throwValue = nextDouble();

        for (int i = 0; i < numberOfOutcomes; i++) {
            accumulator += probability;
            if (throwValue <= accumulator)
                return i;
        }
        //throw exception in javascript
        throw new RuntimeException("MathUtils.SingleThrowEven() - invalid outcome.");
    }

    public static int singleThrowCubeWeighted(int cnt)
    {
        double pTotal = 0;    // Total probability

        //-----
        for (int i = 0; i < cnt; i++)
            pTotal += (i+1)*(i+1)*(i+1);

        //----- Now throw the ball and return an integer indicating the outcome.
        double throwValue = nextDouble() * pTotal;
        double accumulator = 0;

        for (int j = 0; j < cnt; j++) {

            accumulator += (j+1)*(j+1)*(j+1);

            if (throwValue <= accumulator)
                return j;
        }
        throw new RuntimeException("MathUtils.singleThrowArray() - invalid outcome.");
    }

    /// <summary>
    /// Performs a single thrown onto a roulette wheel where the wheel's space is unevenly divided.
    /// The probabilty that a segment will be selected is given by that segment's value in the 'probabilities'
    /// array. The probabilities are normalised before tossing the ball so that their total is always equal to 1.0.
    /// </summary>
    /// <param name="probabilities"></param>
    /// <returns></returns>
    public static int singleThrowArray(double[] aProbabilities) {

        double pTotal = 0;    // Total probability

        //-----
        for (int i = 0; i < aProbabilities.length; i++)
            pTotal += aProbabilities[i];

        //----- Now throw the ball and return an integer indicating the outcome.
        double throwValue = nextDouble() * pTotal;
        double accumulator = 0;

        for (int j = 0; j < aProbabilities.length; j++) {
            accumulator += aProbabilities[j];

            if (throwValue <= accumulator)
                return j;
        }
        throw new RuntimeException("MathUtils.singleThrowArray() - invalid outcome.");
    }


/// <summary>
/// Similar in functionality to SingleThrow(double[] probabilities). However the 'probabilities' array is
/// not normalised. Therefore if the total goes beyond 1 then we allow extra throws, thus if the total is 10
/// then we perform 10 throws.
/// </summary>
/// <param name="probabilities"></param>
/// <returns></returns>
    public static int[] multipleThrows(double[] aProbabilities) {
        double pTotal = 0;    // Total probability
        int numberOfThrows;

        //----- Determine how many throws of the ball onto the wheel.
        for (int i = 0; i < aProbabilities.length; i++)
            pTotal += aProbabilities[i];

        // If total probabilty is > 1 then we take this as meaning more than one throw of the ball.
        int pTotalInteger = (int) Math.floor(pTotal);
        double pTotalRemainder = pTotal - pTotalInteger;
        numberOfThrows = (int) Math.floor(pTotalInteger);

        if (nextDouble() <= pTotalRemainder)
            numberOfThrows++;

        //----- Now throw the ball the determined number of times. For each throw store an integer indicating the outcome.
        int[] outcomes = new int[numberOfThrows];
        for (int a = 0; a < numberOfThrows; a++)
            outcomes[a] = 0;

        for (int i = 0; i < numberOfThrows; i++) {
            double throwValue = nextDouble() * pTotal;
            double accumulator = 0;

            for (int j = 0; j < aProbabilities.length; j++) {
                accumulator += aProbabilities[j];

                if (throwValue <= accumulator) {
                    outcomes[i] = j;
                    break;
                }
            }
        }

        return outcomes;
    }

    public static <T> int[] selectXFromSmallObject(int x, List<T> objects){

        //works with objects with count or arrays with length
        int gCount = objects.size();

//        int[] ixs = new int[gCount];
        List<String> ixs = new ArrayList<String>();

        int[] finalIxs = new int[x];

        for(int i=0; i<gCount;i++)
            ixs.add(Integer.toString(i));

        //how many do we need back? we need x back. So we must remove (# of objects - x) leaving ... x objects
        for(int i=0; i < gCount -x; i++)
        {
            //remove random index
            int remIx = next(ixs.size());
            ixs.remove(remIx);
        }

        //now we go through and collect the remaining integers!
        int cnt = 0;
        for(String ix : ixs)
        {
           finalIxs[cnt++] = Integer.parseInt(ix);
        }

        //all settled, thanks
        return finalIxs;
    }

    //different strategy for very large array objects (best to guess and remove)
    public static <T> int[] selectXFromLargeObject(int x, List<T> objects) {
        //works with objects with count or arrays with length
        int gCount = objects.size();

        //we make sure the number of requested objects is less than the object indices
        x = Math.min(x, gCount);

        Map<String, String> guesses = new HashMap<String, String>();


        int ixCount = 0;
        int[] finalIxs = new int[x];


        for (int i = 0; i < x; i++) {
            int guessIx = next(gCount);

            while (guesses.containsKey(Integer.toString(guessIx)))
                guessIx = next(gCount);

            guesses.put(Integer.toString(guessIx), "true");

            finalIxs[ixCount++] = guessIx;
        }

        return finalIxs;
    }
}
