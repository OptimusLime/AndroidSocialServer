package edu.eplex.androidsocialclient.MainUI.Filters.Evolution;

import android.content.res.AssetManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;
import edu.eplex.AsyncEvolution.activations.PBBipolarSigmoid;
import edu.eplex.AsyncEvolution.activations.PBCos;
import edu.eplex.AsyncEvolution.activations.PBGaussian;
import edu.eplex.AsyncEvolution.activations.pbLinear;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncSeedLoader;
import edu.eplex.AsyncEvolution.backbone.Connection;
import edu.eplex.AsyncEvolution.backbone.Node;
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Cos;
import eplex.win.FastCPPNJava.activation.functions.Gaussian;
import eplex.win.FastCPPNJava.activation.functions.Linear;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastCPPNJava.network.NodeType;
import eplex.win.FastNEATJava.genome.NeatConnection;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.FastNEATJava.genome.NeatNode;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.FastNEATJava.utils.cuid;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 8/14/14.
 */
public class FilterSeedLoader implements AsyncSeedLoader{

    public AssetManager assetManager;
    public List<FilterArtifact> customSeeds;
    public String seedFileLocation = "seeds/basicSeed.json";
    public NeatParameters neatParameters;

    @Override
    public Task<List<Artifact>> asyncLoadSeeds(JsonNode params) {

        if(customSeeds != null && customSeeds.size() != 0)
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

    public ArrayList<FilterArtifact> CreateRandomSeeds(int count)
    {
        ArrayList<FilterArtifact> artifactArrayList = new ArrayList<>();
        for(int i=0; i < count; i++) {

            if(neatParameters.useHyperNEAT)
                artifactArrayList.add(createHyperNEATArtifactSeed());
            else
                artifactArrayList.add(createNEATArtifactSeed());
        }
        return artifactArrayList;
    }

    private FilterArtifact createNEATArtifactSeed()
    {
        int inputCount = 9;
        int outputCount = 9;

        NeatGenome ng = createNEATGenome(inputCount, outputCount);

        FilterArtifact loadedArtifact =  new FilterArtifact(ng);

        //set our own wid for the seed, thanks!
        loadedArtifact.setWID(cuid.getInstance().generate());

        loadedArtifact.meta = FilterArtifact.createEmptyMeta();
        loadedArtifact.meta.user = "Seed Filter";

        //now we have the neatgenome set -- there are no parents -- we are good to go!
        return loadedArtifact;
//
//        return  createArtifactSeed(inputCount, outputCount);

    }
    private FilterArtifact createHyperNEATArtifactSeed()
    {
        //HyperNEAT requires 4 inputs (not including bias - x1,y1 - x2,y2)
        //And also 7 outputs - Bias, HSV (input - hidden), HSV (hidden - output)
        int inputCount = 4;
        int outputCount = 7;

        boolean addAnotherFilter = false;

        NeatGenome ng = createNEATGenome(inputCount, outputCount);

        FilterArtifact loadedArtifact =  new FilterArtifact(ng);

        if(addAnotherFilter) {
            //create another object for different secondary filter please
            NeatGenome secondGenome = createNEATGenome(9, 9);
            loadedArtifact.genomeFilters.add(secondGenome);
        }

        //set our own wid for the seed, thanks!
        loadedArtifact.setWID(cuid.getInstance().generate());

        loadedArtifact.meta = FilterArtifact.createEmptyMeta();
        loadedArtifact.meta.user = "Seed Filter";

        //now we have the neatgenome set -- there are no parents -- we are good to go!
        return loadedArtifact;

//        return  createArtifactSeed(inputCount, outputCount);
    }
    private NeatGenome createNEATGenome(int inputCount, int outputCount)
    {

        ArrayList<NeatNode> nodes = new ArrayList<NeatNode>();

        int g = 0;
        NeatNode bias = new NeatNode();
        bias.gid = "" + g++;
        bias.nodeType = NodeType.bias;
        bias.activationFunction = Linear.class.getName();
        bias.layer = 0;
        bias.step = 0;

        nodes.add(bias);

        for(int i=0; i < inputCount; i++)
        {
            NeatNode inNode = new NeatNode();
            inNode.gid = "" + g++;
            inNode.nodeType = NodeType.input;
            inNode.activationFunction = Linear.class.getName();
            inNode.layer = 0;
            inNode.step = 0;
            nodes.add(inNode);
        }

        for(int i=0; i < outputCount; i++){

            NeatNode outNode = new NeatNode();
            outNode.gid = "" + g++;
            outNode.nodeType = NodeType.output;
            outNode.activationFunction = CPPNActivationFactory.getRandomActivationFunction();
            outNode.layer = 10;
            outNode.step = 0;
            nodes.add(outNode);
        }

        //now we fully connnect everything

        //where do the output nodes start?
        int outputStart = inputCount + 1;

        g = 0;
        ArrayList<NeatConnection> connections = new ArrayList<NeatConnection>();

        //for every input/bias node, wire to all the output nodes
        for(int i=0; i < outputStart; i++)
        {
            for(int j=outputStart; j < outputStart + outputCount; j++)
            {
                //lets create a connection plz
                NeatConnection nc = new NeatConnection();
                nc.gid = "" + g++;
                nc.weight = neatParameters.connectionWeightRange*(2*Math.random() -1);
                nc.sourceID = "" + i;
                nc.targetID = "" + j;
                connections.add(nc);
            }
        }
        return new NeatGenome(cuid.getInstance().generate(), nodes, connections, inputCount, outputCount);

    }

    private FilterArtifact createArtifactSeed(int inputCount, int outputCount)
    {
        NeatGenome ng = createNEATGenome(inputCount, outputCount);

        FilterArtifact loadedArtifact =  new FilterArtifact(ng);

        //set our own wid for the seed, thanks!
        loadedArtifact.setWID(cuid.getInstance().generate());

        loadedArtifact.meta = FilterArtifact.createEmptyMeta();
        loadedArtifact.meta.user = "Seed Filter";

        //now we have the neatgenome set -- there are no parents -- we are good to go!
        return loadedArtifact;
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

        FilterArtifact loadedArtifact = null;

        try
        {
            InputStream fileStream = assetManager.open(fileName);
            //loading the seed from file first
            JsonNode loadedSeed = mapper.readTree(fileStream);

            //we pull the seed identifier as our identifier
            int id = loadedSeed.get("seedID").asInt();

            //now that we have the seed, we go through and pull the info!
            JsonNode genomeList = loadedSeed.get("genomeFilters");

            ArrayList<NeatGenome> genomeFilters = new ArrayList<>();

            //create an empty artifact -- we will fill with genomes
            loadedArtifact = new FilterArtifact();

            for(JsonNode genome : genomeList) {

                //we grab our nodes, and connections
                JsonNode nodes = genome.get("nodes");
                JsonNode connections = genome.get("connections");



                int inCount = 0;
                int outCount = 0;
                List<NeatNode> artifactNodes = new ArrayList<NeatNode>();
                //loop through nodes first
                for (JsonNode node : nodes) {

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

                    if (nn.nodeType == NodeType.input)
                        inCount++;

                    if (nn.nodeType == NodeType.output)
                        outCount++;

                    artifactNodes.add(nn);
                }

                List<NeatConnection> artifactConnections = new ArrayList<NeatConnection>();
                for (JsonNode conn : connections) {
                    NeatConnection nc = new NeatConnection(
                            conn.get("gid").asText(),
                            conn.get("weight").asDouble(),
                            conn.get("sourceID").asText(),
                            conn.get("targetID").asText()
                    );
                    artifactConnections.add(nc);
                }

                NeatGenome ng = new NeatGenome(
                        cuid.getInstance().generate(),
                        artifactNodes,
                        artifactConnections,
                        inCount,
                        outCount
                );

                NeatParameters np = NEATInitializer.DefaultNEATParameters();

                //use default params to issue mutations on connection weights originally
                for (int i = 0; i < np.seedMutateConnectionCount; i++)
                    ng.mutate_ConnectionWeights(np);

                //check our genome to see if it was seeded or not
                JsonNode gParents = genome.get("parents");

                if (gParents == null)
                    //no parents! We're the seed derrrr
                    ng.parents = (new ArrayList<String>());
                else {
                    List<String> pars = new ArrayList<String>();
                    for (JsonNode p : gParents)
                        pars.add(p.asText());
                    ng.parents = pars;
                }

                //add our genome please
                genomeFilters.add(ng);
            }

            //set genome filters for artifact
            loadedArtifact.genomeFilters = genomeFilters;

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
