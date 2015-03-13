package edu.eplex.AsyncEvolution.asynchronous.implementation;

import android.content.res.AssetManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import edu.eplex.AsyncEvolution.activations.PBBipolarSigmoid;
import edu.eplex.AsyncEvolution.activations.PBCos;
import edu.eplex.AsyncEvolution.activations.PBGaussian;
import edu.eplex.AsyncEvolution.activations.pbLinear;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncSeedLoader;
import bolts.Task;
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Cos;
import eplex.win.FastCPPNJava.activation.functions.Gaussian;
import eplex.win.FastCPPNJava.activation.functions.Linear;
import eplex.win.FastCPPNJava.activation.functions.NullFn;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastCPPNJava.network.NodeType;
import eplex.win.FastNEATJava.genome.NeatConnection;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.FastNEATJava.genome.NeatNode;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.FastNEATJava.utils.cuid;
import eplex.win.winBackbone.Artifact;
import edu.eplex.AsyncEvolution.backbone.Connection;
import edu.eplex.AsyncEvolution.backbone.FakeArtifact;
import edu.eplex.AsyncEvolution.backbone.FakeGenome;
import edu.eplex.AsyncEvolution.backbone.NEATArtifact;
import edu.eplex.AsyncEvolution.backbone.Node;

/**
 * Created by paul on 8/14/14.
 */
public class AsyncLocalRandomSeedLoader implements AsyncSeedLoader{

    public AssetManager assetManager;
    public List<NEATArtifact> customSeeds;
    public String seedFileLocation = "seeds/basicSeed.json";

    @Override
    public Task<List<Artifact>> asyncLoadSeeds(JsonNode params) {

        if(customSeeds != null)
            return Task.callInBackground(new Callable<List<Artifact>>() {
                @Override
                public List<Artifact> call() throws Exception {
                    return seedsFromList();
                }
            });
        else
            //now we need to create some fake seeds, and promise their return
            //we can use a convenience method -- since we are doing a local random version here
            return Task.callInBackground(new Callable<List<Artifact>>() {
            @Override
            public List<Artifact> call() throws Exception {
                return seedsFromFile();
            }
        });
    }

    private ArrayList<Node> fakeNodes()
    {
        ArrayList<Node> nodes = new ArrayList<Node>();

        int fakeNodeCount = 1 + ((int)Math.floor(Math.random()*1000))%10;

        for(int i=0; i < fakeNodeCount; i++) {
            Node n = new Node();
            n.gid = "node-" + i + "-" +  (int) (Math.random() * 100000);
            nodes.add(n);
        }

        return nodes;
    }

    private ArrayList<Connection> fakeConnections()
    {
        ArrayList<Connection> conns = new ArrayList<Connection>();

        int fakeConnCount = 3 + ((int)Math.floor(Math.random()*1000))%18;

        for(int i=0; i < fakeConnCount; i++) {

            Connection c = new Connection();
            c.gid = "conn-" + i + "-" + (int) (Math.random() * 100000);
            c.sourceID = "node-" + (int) (Math.random() * 100000);
            c.targetID = "node-" + (int) (Math.random() * 100000);
            c.weight = Math.random() * 2 - 1;
            conns.add(c);
        }

        return conns;
    }

    List<Artifact> fakeSeeds()
    {
        ArrayList<Artifact> seeds =  new ArrayList<Artifact>();

        int fakeSeedCount = 1 + ((int)Math.floor(Math.random()*1000))%4;

        for(int i=0; i < fakeSeedCount; i++)
        {
            FakeArtifact fa = new FakeArtifact();
            FakeGenome g = new FakeGenome();

            //grab random fake nodes
            g.setNodes(fakeNodes());
            //random fake conns
            g.setConnections(fakeConnections());
            fa.genome = g;

            fa.setWID("wonky-seed" + i);

            seeds.add(fa);
        }

        return seeds;
    }

    List<Artifact> seedsFromList()
    {
        ArrayList<Artifact> seeds =  new ArrayList<Artifact>();
        for(int i=0; i < customSeeds.size(); i++)
        {
            seeds.add((Artifact)customSeeds.get(i));
        }
        return seeds;
    }
    List<Artifact> seedsFromFile()
    {
        ArrayList<Artifact> seeds =  new ArrayList<Artifact>();

        Artifact a = loadSeed(seedFileLocation);

        seeds.add(a);

        return seeds;
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
        else if(actFun.equals(pbLinear.class.getSimpleName()))
        {
            return pbLinear.class.getName();
        }
        else if(actFun.equals(PBBipolarSigmoid.class.getSimpleName()))
        {
            return PBBipolarSigmoid.class.getName();
        }
        else if(actFun.equals(PBGaussian.class.getSimpleName())) {
            return PBGaussian.class.getName();
        }
        else if(actFun.equals(PBCos.class.getSimpleName())) {
            return PBCos.class.getName();
        }
        else if(actFun.equals(Sine.class.getSimpleName())) {
            return Sine.class.getName();
        }
        else
            throw new RuntimeException("Unknown Activation Function in seed loading");
    }

    Artifact loadSeed(String fileName)
    {
        ObjectMapper mapper = new ObjectMapper();

        NEATArtifact loadedArtifact = null;

        try
        {

            InputStream fileStream = assetManager.open(fileName);
            //loading the seed from file first
            JsonNode loadedSeed = mapper.readTree(fileStream);

            //we pull the seed identifier as our identifier
            int id = loadedSeed.get("seedID").asInt();

            //now that we have the seed, we go through and pull the info!
            JsonNode genome = loadedSeed.get("genome");

            //we grab our nodes, and connections
            JsonNode nodes = genome.get("nodes");
            JsonNode connections = genome.get("connections");

            loadedArtifact = new NEATArtifact();


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

                if(nn.type == NodeType.input)
                    inCount++;

                if(nn.type == NodeType.output)
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

            loadedArtifact.genome = new NeatGenome(
                    cuid.getInstance().generate(),
                    artifactNodes,
                    artifactConnections,
                    inCount,
                    outCount
            );

            NeatParameters np = NEATInitializer.DefaultNEATParameters();

            //use default params to issue mutations on connection weights originally
            for(int i=0; i < np.seedMutateConnectionCount; i++)
                loadedArtifact.genome.mutate_ConnectionWeights(np);

            //check our genome to see if it was seeded or not
            JsonNode gParents = genome.get("parents");

            if(gParents == null)
                //no parents! We're the seed derrrr
                loadedArtifact.genome.parents = (new ArrayList<String>());
            else
            {
                List<String> pars = new ArrayList<String>();
                for(JsonNode p : gParents)
                    pars.add(p.asText());
                loadedArtifact.genome.parents = pars;
            }

            //set our own wid for the seed, thanks!
            loadedArtifact.setWID(cuid.getInstance().generate(id));


            //now check for artifact level parents
            JsonNode parents = loadedSeed.get("parents");

            if(parents == null)
                //no parents! We're the seed derrrr
                loadedArtifact.setParents(new ArrayList<String>());
            else
            {
                List<String> pars = new ArrayList<String>();
                for(JsonNode p : parents)
                    pars.add(p.asText());
                loadedArtifact.setParents(pars);
            }

            //all done!

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return loadedArtifact;


    }

}
