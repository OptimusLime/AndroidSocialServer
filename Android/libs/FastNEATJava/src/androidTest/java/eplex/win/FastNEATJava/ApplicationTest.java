package eplex.win.FastNEATJava;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Cos;
import eplex.win.FastCPPNJava.activation.functions.Gaussian;
import eplex.win.FastCPPNJava.activation.functions.Linear;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastCPPNJava.network.NodeType;
import eplex.win.FastNEATJava.decode.DecodeToFloatFastConcurrentNetwork;
import eplex.win.FastNEATJava.genome.NeatConnection;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.FastNEATJava.genome.NeatNode;
import eplex.win.FastNEATJava.utils.cuid;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
        //lets do this thang!



    }


}