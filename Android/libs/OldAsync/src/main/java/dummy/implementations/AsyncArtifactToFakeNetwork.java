package dummy.implementations;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.Callable;

import asynchronous.interfaces.AsyncArtifactToPhenotype;
import bolts.Task;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.Genome;
import win.eplex.backbone.Connection;
import win.eplex.backbone.FakeArtifact;
import win.eplex.backbone.FakeGenome;
import win.eplex.backbone.Node;

/**
 * Created by paul on 8/14/14.
 */
public class AsyncArtifactToFakeNetwork implements AsyncArtifactToPhenotype<Artifact, double[][]> {
    @Override
    public Task<double[][]> asyncPhenotypeToUI(final Artifact artifact, final JsonNode params) {

        return Task.callInBackground(new Callable<double[][]>() {
            @Override
            public double[][] call() throws Exception {
                return syncConvertNetworkToOutputs(artifact,params);
            }
        });
    }

    //synchronously convert artifacts into double[][] netowrk outputs, yo
    public double[][] syncConvertNetworkToOutputs(Artifact offspring, JsonNode params)
    {
        //let's convert our artifact object into a damn genome
        //then take that genome, and use it to build our outputs

        Genome g = ((FakeArtifact)offspring).genome;

        //now convert our genome into a CPPN
        List<Node> nodes = ((FakeGenome)g).nodes;
        List<Connection> conns = ((FakeGenome)g).connections;

        int width = 25;
        int height = 25;

        if(params != null)
        {
            width = params.get("width").asInt();
            height = params.get("height").asInt();
        }

        //then activate our connections!
        //call upon params
        int pixelCount = width*height;

        //now we have our outputs, hoo-ray!
        double[][] fakeOutputs = new double[pixelCount][];

        double[] fakeRGB;
        for(int i=0; i < pixelCount; i++)
        {
            fakeRGB = new double[3];
            fakeRGB[0] = Math.random();
            fakeRGB[1] = Math.random();
            fakeRGB[2] = Math.random();
            fakeOutputs[i] = fakeRGB;
        }

        return fakeOutputs;
    }
}
