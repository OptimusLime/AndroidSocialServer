package cppn.implementations;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.Callable;

import asynchronous.interfaces.AsyncArtifactToPhenotype;
import bolts.Task;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastNEATJava.decode.DecodeToFloatFastConcurrentNetwork;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.Genome;
import win.eplex.backbone.Connection;
import win.eplex.backbone.FakeArtifact;
import win.eplex.backbone.FakeGenome;
import win.eplex.backbone.NEATArtifact;
import win.eplex.backbone.Node;

/**
 * Created by paul on 8/14/14.
 */
public class AsyncArtifactToCPPN implements AsyncArtifactToPhenotype<Artifact, double[][]> {
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

        NeatGenome g = ((NEATArtifact)offspring).genome;

        CPPN decoded = DecodeToFloatFastConcurrentNetwork.DecodeNeatGenomeToCPPN(g);

        //now convert our genome into a CPPN
//        List<Node> nodes = ((NEATArtifact)g).nodes;
//        List<Connection> conns = ((NEATArtifact)g).connections;

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
        double[][] cppnOutputs = new double[pixelCount][];

        double inSqrt2 = Math.sqrt(2);

        int allX = width, allY = height;

        double startX = -1, startY = -1;
        double dx = 2.0/(allX-1), dy = 2.0/(allY-1);

        double currentX = startX, currentY = startY;

        double[] hsvPull;
        double[] inputs;

        int ix = 0;
        try {
            for (int y = 0; y < allY; y++) {

                for (int x = 0; x < allX; x++) {

                    hsvPull = new double[3];

                    //just like in picbreeder!
                    currentX = ((x << 1) - width + 1.0) / width;
                    currentY = ((y << 1) - height + 1.0) / height;

                    inputs = new double[]{currentX, currentY, Math.sqrt(currentX * currentX + currentY * currentY) * inSqrt2};

                    decoded.clearSignals();
                    decoded.recursiveActivation(inputs);

                    hsvPull[0] = decoded.getOutputSignal(0);
                    hsvPull[1] = decoded.getOutputSignal(1);
                    hsvPull[2] = decoded.getOutputSignal(2);

                    cppnOutputs[ix++] = hsvPull;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return cppnOutputs;
    }

//    function runCPPNAcrossFixedSize(activationFunction, size)
//    {
//        var inSqrt2 = Math.sqrt(2);
//
//        var allX = size.width, allY = size.height;
//        var width = size.width, height= size.height;
//
//        var startX = -1, startY = -1;
//        var dx = 2.0/(allX-1), dy = 2.0/(allY-1);
//
//        var currentX = startX, currentY = startY;
//
//        var newRow;
//        var rows = [];
//
//        var inputs = [];
//        var outputs, rgb;
//
//
//        //we go by the rows
//        for(var y=allY-1; y >=0; y--){
//
//            //init and push new row
//            var newRow = [];
//            rows.push(newRow);
//            for(var x=0; x < allX; x++){
//
//                //just like in picbreeder!
//                var currentX = ((x << 1) - width + 1) / width;
//                var currentY = ((y << 1) - height + 1) / height;
//
//                inputs = [currentX, currentY, Math.sqrt(currentX*currentX + currentY*currentY)*inSqrt2];
//
//                //run the CPPN please! Acyclic cppns only, thank you
//                outputs = activationFunction(inputs);
//
//                //rgb conversion here
//                rgb = FloatToByte(PicHSBtoRGB(outputs[0], clampZeroOne(outputs[1]), Math.abs(outputs[2])));
//
//                //add to list of outputs to return
//                newRow.push(rgb);
//            }
//        }
//
//        return rows;
//    }


}
