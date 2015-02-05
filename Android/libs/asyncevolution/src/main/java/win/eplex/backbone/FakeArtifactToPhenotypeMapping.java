package win.eplex.backbone;

import com.fasterxml.jackson.databind.JsonNode;
import com.octo.android.robospice.request.SpiceRequest;

import java.util.List;

import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.Genome;
import eplex.win.winBackbone.ArtifactToPhenotypeMapping;

/**
 * Created by paul on 8/13/14.
 */
public class FakeArtifactToPhenotypeMapping extends SpiceRequest<double[][]> {

    private Artifact offspring;
    private JsonNode params;

    public FakeArtifactToPhenotypeMapping(Artifact offspring, JsonNode params) {
        super(double[][].class);
        this.setArtifactToMap(offspring, params);
    }

    public void setArtifactToMap(Artifact offspring, JsonNode params) {
        this.offspring = offspring;
        this.params = params;
    }

    public double[][] syncConvertNetworkToOutputs()
    {
        //let's convert our artifact object into a damn genome
        //then take that genome, and use it to build our outputs

        Genome g = ((FakeArtifact)offspring).genome;

        //now convert our genome into a CPPN
        List<Node> nodes = ((FakeGenome)g).nodes;
        List<Connection> conns = ((FakeGenome)g).connections;

        //then activate our connections!
        //call upon params
        int pixelCount = 5;

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


    @Override
    public double[][] loadDataFromNetwork() throws Exception {

        return syncConvertNetworkToOutputs();
    }

}
