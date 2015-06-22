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
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Cos;
import eplex.win.FastCPPNJava.activation.functions.Gaussian;
import eplex.win.FastCPPNJava.activation.functions.Linear;
import eplex.win.FastCPPNJava.activation.functions.PlainSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastNEATJava.decode.DecodeToFloatFastConcurrentNetwork;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.winBackbone.Artifact;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3TextureSamplingFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageBoxBlurFilter;

/**
 * Created by paul on 3/13/15.
 */
public class GPUNetworkFilter {

    public GPUNetworkFilter()
    {

    }

    public static final String SHADER_ACTIVATIONS_ADDITIONAL = "" +
            "float PlainSigmoid(float val){ return " + new PlainSigmoid().gpuFunctionString() + " }\n" +
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
        NeatGenome g, g2 = null;

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
            if(fa.genomeFilters.size() > 1)
                g2 = fa.genomeFilters.get(1);
        }

//        NeatGenome g = ((NEATArtifact)artifact).genome;

        //decode

        CPPN decoded;
        CPPN secondaryCPPN = null;
        boolean useHyperNEAT = NEATInitializer.DefaultNEATParameters().useHyperNEAT;

        if(useHyperNEAT) {
            decoded = DecodeToFloatFastConcurrentNetwork.DecodeNeatGenomeToHyperNEATCPPN(g);

            if(g2 != null)
                secondaryCPPN = DecodeToFloatFastConcurrentNetwork.DecodeNeatGenomeToCPPN(g2);

        }
        else {
            decoded = DecodeToFloatFastConcurrentNetwork.DecodeNeatGenomeToCPPN(g);
        }

        //turn into shader code
        String fragShader = decoded.cppnToShader(useHyperNEAT, SHADER_ACTIVATIONS_ADDITIONAL);

        GPUImage3x3TextureSamplingFilter cppnFilter = new GPUImage3x3TextureSamplingFilter(fragShader);
        GPUImage3x3TextureSamplingFilter secondaryFilter;


        //create our image plzzz
        GPUImage mGPUImage = new GPUImage(context);

        mGPUImage.setFilter(cppnFilter);

//        mGPUImage.setFilter(new GPUImageBoxBlurFilter(.4f));
        mGPUImage.setImage(originalImage);

        if(secondaryCPPN != null) {
            secondaryFilter = new GPUImage3x3TextureSamplingFilter(secondaryCPPN.cppnToShader(false, SHADER_ACTIVATIONS_ADDITIONAL));

            Bitmap bm = mGPUImage.getBitmapWithFilterApplied();
            mGPUImage.setFilter(secondaryFilter);
            mGPUImage.setImage(bm);

            Bitmap finalImage = mGPUImage.getBitmapWithFilterApplied();
            bm.recycle();

            return finalImage;
        }
        else
            //create the filtered bitmap object and send it back
            return mGPUImage.getBitmapWithFilterApplied();
    }

}
