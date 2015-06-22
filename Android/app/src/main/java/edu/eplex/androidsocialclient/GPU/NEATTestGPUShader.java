package edu.eplex.androidsocialclient.GPU;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.eplex.androidsocialclient.R;
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
import eplex.win.FastNEATJava.genome.NeuronGeneStruct;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.FastNEATJava.utils.cuid;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3TextureSamplingFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

/**
 * Created by paul on 3/12/15.
 */
public class NEATTestGPUShader {

    public static void TestShader(FragmentActivity mContext, Bitmap originalImage)
    {
        //load up a neat thingy
        NeatGenome ng = GenomeFromFile(mContext, "testseeds/basicSeed.json");

        //lets mutate the hell out of it
        int mutationCount = 25;
        NeatParameters np = new NeatParameters();

        np.pMutateAddConnection = .3;
        np.pMutateAddNode = .4;
        np.pMutateChangeActivations = .1;
        np.pMutateConnectionWeights = .2;
        np.pNodeMutateActivationRate = .1;

        np.allowSelfConnections = false;
        np.disallowRecurrence = true;

        Map<String, NeuronGeneStruct> newNodes = new HashMap<String, NeuronGeneStruct>();
        Map<String, String> newConnectionMap = new HashMap<String, String>();
        for(int i=0 ; i < mutationCount; i++)
            ng.mutate(newNodes, newConnectionMap, np);

        //now turn it into a network
        CPPN decoded = DecodeToFloatFastConcurrentNetwork.DecodeNeatGenomeToCPPN(ng);

        //now decode please!
        String buildShader = decoded.cppnToShader(false, null);

        GPUImage3x3TextureSamplingFilter cppnFilter = new GPUImage3x3TextureSamplingFilter(buildShader);

        //now we have a filter! Let's try and do things with it!
        GPUImageView gpuView = null;// (GPUImageView)mContext.findViewById(R.id.gpuimage);

//        gpuView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        gpuView.setScaleType(GPUImage.ScaleType.CENTER_CROP);
//        gpuView.setImage(bitmapLocation);
        gpuView.setImage(originalImage);
        gpuView.setFilter(cppnFilter);

        gpuView.requestRender();

        gpuView.saveToPictures("GPUImage", "gpuTest.jpg", new GPUImageView.OnPictureSavedListener() {
            @Override
            public void onPictureSaved(Uri uri) {

            }
        });

    }

    static String activationToClassName(String actFun)
    {
        if(actFun.equals(Linear.class.getSimpleName()))
        {
            return Linear.class.getName();
        }
        else if(actFun.equals(BipolarSigmoid.class.getSimpleName()))
        {
            return BipolarSigmoid.class.getName();
        }
        else if(actFun.equals(Gaussian.class.getSimpleName())) {
            return Gaussian.class.getName();
        }
        else if(actFun.equals(Cos.class.getSimpleName())) {
            return Cos.class.getName();
        }
        else if(actFun.equals(Sine.class.getSimpleName())) {
            return Sine.class.getName();
        }
        else
            throw new RuntimeException("Unknown Activation Function in seed loading");
    }

    static NeatGenome GenomeFromFile(Context mContext, String fileName)
    {
        NeatGenome ng = null;

        try {

            InputStream fileStream = mContext.getAssets().open(fileName);

            ObjectMapper mapper= new ObjectMapper();

            //loading the seed from file first
            JsonNode loadedSeed = null;

            loadedSeed = mapper.readTree(fileStream);

            //we pull the seed identifier as our identifier
            int id = loadedSeed.get("seedID").asInt();

            //now that we have the seed, we go through and pull the info!
            JsonNode genome = loadedSeed.get("genome");

            //we grab our nodes, and connections
            JsonNode nodes = genome.get("nodes");
            JsonNode connections = genome.get("connections");


            int inCount = 0;
            int outCount = 0;
            List<NeatNode> artifactNodes = new ArrayList<NeatNode>();
            //loop through nodes first
            for(JsonNode node : nodes)
            {

                NeatNode nn;

                String clazz = activationToClassName(node.get("activationFunction").asText());
//                String clazz = node.get("activationFunction").asText();
//
//                try {
//                    //test if the class exists in the default CPPN activation function package
//                    Class.forName(NullFn.class.getPackage().getName() + "." + clazz);
//                } catch( ClassNotFoundException e ) {
//                    //my class isn't there!
//                    //it's our custom class for these activation functions
//                    clazz = pbLinear.class.getPackage().getName() + "." + node.get("activationFunction").asText();
//                }

                nn = new NeatNode(
                        node.get("gid").asText(),
                        //though we use pblinear here, it's really only important that we target a sing
                        clazz,
                        node.get("layer").asDouble(),
                        NodeType.valueOf(node.get("nodeType").asText().toLowerCase()));

                nn.bias = node.get("bias").asDouble();

                if(nn.nodeType == NodeType.input)
                    inCount++;

                if(nn.nodeType == NodeType.output)
                    outCount++;

                artifactNodes.add(nn);
            }

            List<NeatConnection> artifactConnections = new ArrayList<NeatConnection>();
            for(JsonNode conn : connections)
            {
                NeatConnection nc = new NeatConnection(
                        conn.get("gid").asText(),
                        conn.get("weight").asDouble(),
                        conn.get("sourceID").asText(),
                        conn.get("targetID").asText()
                );
                artifactConnections.add(nc);
            }

            ng = new NeatGenome(
                    cuid.getInstance().generate(),
                    artifactNodes,
                    artifactConnections,
                    inCount,
                    outCount
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ng;
    }
}
