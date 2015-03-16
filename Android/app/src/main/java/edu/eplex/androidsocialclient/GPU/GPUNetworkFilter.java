package edu.eplex.androidsocialclient.GPU;

import android.content.Context;
import android.graphics.Bitmap;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.Callable;

import bolts.Task;
import edu.eplex.AsyncEvolution.activations.PBBipolarSigmoid;
import edu.eplex.AsyncEvolution.activations.PBCos;
import edu.eplex.AsyncEvolution.activations.PBGaussian;
import edu.eplex.AsyncEvolution.activations.pbLinear;
import edu.eplex.AsyncEvolution.backbone.NEATArtifact;
import edu.eplex.AsyncEvolution.cache.implementations.EvolutionBitmapManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Cos;
import eplex.win.FastCPPNJava.activation.functions.Gaussian;
import eplex.win.FastCPPNJava.activation.functions.Linear;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastNEATJava.decode.DecodeToFloatFastConcurrentNetwork;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.winBackbone.Artifact;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3TextureSamplingFilter;

/**
 * Created by paul on 3/13/15.
 */
public class GPUNetworkFilter {

    public GPUNetworkFilter()
    {

    }

    public static final String SHADER_ACTIVATIONS_ADDITIONAL = "" +
            "float pbLinear(float val){ return " + new pbLinear().gpuFunctionString() + " }\n" +
            "float PBBipolarSigmoid(float val){ return " + new PBBipolarSigmoid().gpuFunctionString() + " }\n" +
            "float PBGaussian(float val){ return " + new PBGaussian().gpuFunctionString() + " }\n" +
            "float PBCos(float val){ return " + new PBCos().gpuFunctionString() + " }\n";

    public Task<Bitmap> AsyncFilterBitmapGPU(final Context context, final Bitmap originalImage, final Artifact artifact, final JsonNode params)
    {
       return Task.callInBackground(new Callable<Bitmap>() {
           @Override
           public Bitmap call() throws Exception {
               return GPUNetworkToImage(context, originalImage, artifact, params);
           }
       });
    }

    //should be called on another thread
    Bitmap GPUNetworkToImage(Context context, Bitmap originalImage, Artifact artifact, JsonNode params)
    {
        //grab genome
        NeatGenome g;

        //check which type of artifact
        try {
            NEATArtifact n = ((NEATArtifact) artifact);
            if (n == null) {
                throw new Exception("Null artfact");
            }
            g = n.genome;
        }
        catch (Exception e)
        {
            FilterArtifact fa = (FilterArtifact) artifact;
            g = fa.genomeFilters.get(0);
        }

//        NeatGenome g = ((NEATArtifact)artifact).genome;

        //decode
        CPPN decoded = DecodeToFloatFastConcurrentNetwork.DecodeNeatGenomeToCPPN(g);

        //turn into shader code
        String fragShader = decoded.cppnToShader(SHADER_ACTIVATIONS_ADDITIONAL);

        GPUImage3x3TextureSamplingFilter cppnFilter = new GPUImage3x3TextureSamplingFilter(fragShader);

        //create our image plzzz
        GPUImage mGPUImage = new GPUImage(context);

        mGPUImage.setFilter(cppnFilter);
        mGPUImage.setImage(originalImage);

        //create the filtered bitmap object and send it back
        return mGPUImage.getBitmapWithFilterApplied();
    }

}
