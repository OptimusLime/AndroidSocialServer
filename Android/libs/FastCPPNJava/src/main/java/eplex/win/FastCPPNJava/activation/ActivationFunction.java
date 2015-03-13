package eplex.win.FastCPPNJava.activation;

/**
 * Created by paul on 8/17/14.
 */
public interface ActivationFunction {

    public String functionID();
    public double calculate(double val);
    public String gpuFunctionString();

}
