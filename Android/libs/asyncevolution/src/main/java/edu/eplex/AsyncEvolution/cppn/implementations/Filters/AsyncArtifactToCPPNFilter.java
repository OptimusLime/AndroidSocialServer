package edu.eplex.AsyncEvolution.cppn.implementations.Filters;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.Callable;

import bolts.Task;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncArtifactToPhenotype;
import edu.eplex.AsyncEvolution.backbone.NEATArtifact;
import edu.eplex.AsyncEvolution.cache.implementations.EvolutionBitmapManager;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastNEATJava.decode.DecodeToFloatFastConcurrentNetwork;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 8/14/14.
 */
public class AsyncArtifactToCPPNFilter implements AsyncArtifactToPhenotype<Artifact, double[][]> {
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

        Bitmap inputImage = EvolutionBitmapManager.getInstance().getBitmap(params.get("image").asText());
        int[] pixels = EvolutionBitmapManager.getInstance().getBitmapPixels(params.get("image").asText());

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

        //width = inputImage.getWidth();
        //height = inputImage.getHeight();


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

        double[] rgbRet;
        double[] inputs;

        int truePixelWidth = inputImage.getWidth();
        int truePixelHeight = inputImage.getHeight();
//        int[] pixels = new int[truePixelWidth*truePixelHeight];

        //retrieved the pixels
//        inputImage.getPixels(pixels, 0, truePixelWidth, 0, 0,
//                truePixelWidth, truePixelHeight);


        double skipWidthPixels = (double)truePixelWidth/width;
        double skipHeightPixels = (double)truePixelHeight/height;

        int pixelColor;

        int ix = 0;
        try {
            for (int y = 0; y < allY; y++) {

                for (int x = 0; x < allX; x++) {

                    rgbRet = new double[3];

                    //lets input this info according to input pixels

                    int pixelIx = (int)Math.floor(Math.floor(x*skipWidthPixels) + Math.floor(y*skipHeightPixels)*width);
                    pixelColor = inputImage.getPixel((int)Math.floor(x*skipWidthPixels), (int)Math.floor(y*skipHeightPixels));
                            //pixels[pixelIx];

                    inputs = new double[]{
                            Color.red(pixelColor)/255.0,
                            Color.green(pixelColor)/255.0,
                            Color.blue(pixelColor)/255.0
                            };


                    //just like in picbreeder!
//                    currentX = ((x << 1) - width + 1.0) / width;
//                    currentY = ((y << 1) - height + 1.0) / height;
//
//                    inputs = new double[]{
//                            Color.red(pixelColor)/255.0,
//                            currentX,
//                            currentY,
//                            Math.sqrt(currentX * currentX + currentY * currentY) * inSqrt2};
//

                    decoded.clearSignals();
                    decoded.recursiveActivation(inputs);

                    rgbRet[0] = decoded.getOutputSignal(0);
                    rgbRet[1] = decoded.getOutputSignal(1);
                    rgbRet[2] = decoded.getOutputSignal(2);

                    cppnOutputs[ix++] = rgbRet;
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
