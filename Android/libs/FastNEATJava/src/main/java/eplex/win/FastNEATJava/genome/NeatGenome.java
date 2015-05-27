package eplex.win.FastNEATJava.genome;


import android.util.Pair;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.network.NodeType;
import eplex.win.FastCPPNJava.utils.MathUtils;
import eplex.win.FastNEATJava.help.CorrelationItem;
import eplex.win.FastNEATJava.help.CorrelationResults;
import eplex.win.FastNEATJava.help.CorrelationType;
import eplex.win.FastNEATJava.utils.ConnectionMutationParameterGroup;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.FastNEATJava.utils.cuid;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeatGenome
{
    @JsonProperty("parents")
    public List<String> parents;

    @JsonProperty("wid")
    public String wid;

    double fitness;

    public long date;

    public String dbType = "NEATGenotype";

    @JsonProperty("nodes")
    public List<NeatNode> nodes;

    @JsonProperty("connections")
    public List<NeatConnection> connections;

    Map<String, NeatConnection> connectionLookup;
    Map<String, NeatNode> nodeLookup;

    public int inputNodeCount;
    public int inputAndBiasNodeCount;
    public int outputNodeCount;
    public int inputBiasOutputNodeCount;
    public int inputBiasOutputNodeCountMinus2;

    public NeatGenome(){}

    public NeatGenome(String gid,
                      List<NeatNode> nodes,
                      List<NeatConnection> connections,
                      int incount,
                      int outcount)
    {
        this.wid = gid;

        // From C#: Ensure that the connectionGenes are sorted by innovation ID at all times.
        this.nodes = nodes;
        this.connections = connections;

        // From C#: For efficiency we store the number of input and output neurons. These two quantities do not change
        // throughout the life of a genome. Note that inputNeuronCount does NOT include the bias neuron! use inputAndBiasNeuronCount.
        // We also keep all input(including bias) neurons at the start of the neuronGeneList followed by
        // the output neurons.

        initializeNodeCounts();

//        this.inputNodeCount= incount;
//        this.inputAndBiasNodeCount= incount+1;
//        this.outputNodeCount= outcount;
//        this.inputBiasOutputNodeCount= this.inputAndBiasNodeCount + this.outputNodeCount;
//        this.inputBiasOutputNodeCountMinus2= this.inputBiasOutputNodeCount -2;


        // Temp tables.
        this.connectionLookup = null;
        this.nodeLookup = null;
    }

    public void initializeNodeCounts()
    {
        int incount = 0;
        int outcount = 0;

        for(int i=0; i < this.nodes.size(); i++) {

            NeatNode nn = this.nodes.get(i);
            if (nn.nodeType == NodeType.input)
                incount++;
            else if(nn.nodeType == NodeType.output)
                outcount++;
        }

        this.inputNodeCount= incount;
        this.inputAndBiasNodeCount= incount+1;
        this.outputNodeCount= outcount;
        this.inputBiasOutputNodeCount= this.inputAndBiasNodeCount + this.outputNodeCount;
        this.inputBiasOutputNodeCountMinus2= this.inputBiasOutputNodeCount -2;
    }

    public static String nextInnovationID(int ix)
    {
        //always the same across machines -- for inputs/outputs/hidden/bias node/conn ids
        return cuid.getInstance().generate(ix);
    }

    public static String nextInnovationID()
    {
        return cuid.getInstance().generate();
    }

    static void insertByInnovation(NeatConnection connection, List<NeatConnection> connectionList)
    {
        // Determine the insert idx with a linear search, starting from the end
        // since mostly we expect to be adding genes that belong only 1 or 2 genes
        // from the end at most.
        int idx= connectionList.size()-1;
        for(; idx>-1; idx--)
        {
            if(cuid.isLessThan(connectionList.get(idx).gid, connection.gid))
            {	// Insert idx found.
                break;
            }
        }

        //add to connection list at this location
        connectionList.add(idx + 1, connection);
    }

    static Map<String, NeatConnection> CreateGIDConnectionLookup(List<NeatConnection> arObject)
    {
        Map<String, NeatConnection> lookup = new HashMap<String, NeatConnection>();

        for(NeatConnection conn : arObject)
        {
            lookup.put(conn.gid, conn);
        }

        return lookup;
    }

    static Map<String, NeatNode> CreateGIDNodeLookup(List<NeatNode> arObject)
    {
        Map<String, NeatNode> lookup = new HashMap<String, NeatNode>();

        for(NeatNode node : arObject)
        {
            lookup.put(node.gid, node);
        }

        return lookup;
    }

    public static NeatGenome CreateGenomeByInnovation(int ins,
                                                      int outs,
                                                      double connectionProportion,
                                                      double connectionWeightRange,
                                                      Map<String, String> existing)
    {
        //existing is for seing if a connection innovation id already exists according to local believers/shamans
        if(existing == null)
            existing = new HashMap<String, String>();

        //create our ins and outs,
        List<NeatNode> inputNodeList = new ArrayList<NeatNode>();
        List<NeatNode> outputNodeList = new ArrayList<NeatNode>();
        List<NeatNode> nodeList = new ArrayList<NeatNode>();
        List<NeatConnection> connectionList = new ArrayList<NeatConnection>();

        String aFunc = "NullFn"; // CPPNactivationFactory.getActivationFunction('NullFn');

        int iCount = 0;

        // IMPORTANT NOTE: The neurons must all be created prior to any connections. That way all of the genomes
        // will obtain the same innovation ID's for the bias,input and output nodes in the initial population.
        // Create a single bias neuron.
        NeatNode node = new NeatNode(nextInnovationID(iCount++), aFunc, NeatNode.INPUT_LAYER, NodeType.bias);

        //null, idGenerator.NextInnovationId, NeuronGene.INPUT_LAYER, NeuronType.Bias, actFunct, stepCount);
        inputNodeList.add(node);
        nodeList.add(node);


        // Create input neuron genes.
        aFunc =  "NullFn";

        for(int i=0; i<ins; i++)
        {
            //TODO: DAVID proper activation function change to NULL?
            node = new NeatNode(nextInnovationID(iCount++), aFunc, NeatNode.INPUT_LAYER, NodeType.input);
            inputNodeList.add(node);
            nodeList.add(node);
        }

        // Create output neuron genes.
        aFunc = "BipolarSigmoid";
        for(int i=0; i<outs; i++)
        {
            //TODO: DAVID proper activation function change to NULL?
            node = new NeatNode(nextInnovationID(iCount++), aFunc, NeatNode.OUTPUT_LAYER, NodeType.output);
            outputNodeList.add(node);
            nodeList.add(node);
        }

        // Loop over all possible connections from input to output nodes and create a number of connections based upon
        // connectionProportion.
        for(NeatNode targetNode : outputNodeList) {

            for (NeatNode sourceNode : inputNodeList) {
                // Always generate an ID even if we aren't going to use it. This is necessary to ensure connections
                // between the same neurons always have the same ID throughout the generated population.

                if (Math.random() < connectionProportion) {

                    String cIdentifier = '(' + sourceNode.gid + "," + targetNode.gid + ')';

                    // Ok lets create a connection.
                    //if it already exists, we can use the existing innovation ID
                    String connectionInnovationId;
                    if (existing.containsKey(cIdentifier))
                        connectionInnovationId = existing.get(cIdentifier);
                    else //if we didn't have one before, we do now! If we did, we simply overwrite with the same innovation id
                        connectionInnovationId = nextInnovationID();

                    existing.put(cIdentifier, connectionInnovationId);

                    connectionList.add(new NeatConnection(connectionInnovationId,
                            (Math.random() * connectionWeightRange) - connectionWeightRange / 2.0,
                            sourceNode.gid,
                            targetNode.gid));

                }
            }
        }

        // Don't create any hidden nodes at this point. Fundamental to the NEAT way is to start minimally!
        return new NeatGenome(nextInnovationID(), nodeList, connectionList, ins, outs);
    }

    public static NeatGenome Copy(NeatGenome genome)
    {
        List<NeatNode> nodeCopy = new ArrayList<NeatNode>();

        List<NeatConnection> connectionCopy = new ArrayList<NeatConnection>();

        for(NeatNode node : genome.nodes)
        {
            nodeCopy.add(node.clone());
        }

        for(NeatConnection conn : genome.connections)
        {
            connectionCopy.add(conn.clone());
        }

        return new NeatGenome(genome.wid, nodeCopy, connectionCopy, genome.inputNodeCount, genome.outputNodeCount);
    }

    public static NeatGenome Copy(NeatGenome genome, String gid)
    {
        //Genome copy
        NeatGenome cp = Copy(genome);

        if(gid != null)
            cp.wid = gid;

        return cp;
    }

    /// Asexual reproduction with built in mutation.
    public NeatGenome createOffspringAsexual(Map<String, NeuronGeneStruct> newNodeTable,
                                             Map<String, String> newConnectionTable,
                                             NeatParameters np)
    {
        //copy the genome, then mutate
        NeatGenome genome = NeatGenome.Copy(this, nextInnovationID());

        //mutate genome before returning
        genome.mutate(newNodeTable, newConnectionTable, np);

        return genome;
    }

    /// <summary>
    /// Adds a connection to the list that will eventually be copied into a child of this genome during sexual reproduction.
    /// A helper function that is only called by CreateOffspring_Sexual_ProcessCorrelationItem().
    /// </summary>
    /// <param name="connectionGene">Specifies the connection to add to this genome.</param>
    /// <param name="overwriteExisting">If there is already a connection from the same source to the same target,
    /// that connection is replaced when overwriteExisting is true and remains (no change is made) when overwriteExisting is false.</param>
    public void createOffspringSexual_AddGene(List<NeatConnection> connectionList,
                                              Map<String, String> connectionTable,
                                              NeatConnection connection,
                                              boolean overwriteExisting)
    {

        String conKey = connection.gid;

        // Check if a matching gene has already been added.
        String oIdx = connectionTable.get(conKey);

        if(oIdx==null)
        {	// No matching gene has been added.
            // Register this new gene with the newConnectionGeneTable - store its index within newConnectionGeneList.
            connectionTable.put(conKey, Integer.toString(connectionList.size()));

            // Add the gene to the list.
            connectionList.add(NeatConnection.Copy(connection));
        }
        else if(overwriteExisting)
        {
            // Overwrite the existing matching gene with this one. In fact only the weight value differs between two
            // matching connection genes, so just overwrite the existing genes weight value.

            // Remember that we stored the gene's index in newConnectionGeneTable. So use it here.
            connectionList.get(Integer.parseInt(oIdx)).weight = connection.weight;
        }
    }


/// <summary>
/// Given a description of a connection in two parents, decide how to copy it into their child.
/// A helper function that is only called by CreateOffspring_Sexual().
/// </summary>
/// <param name="correlationItem">Describes a connection and whether it exists on one parent, the other, or both.</param>
/// <param name="fitSwitch">If this is 1, then the first parent is more fit; if 2 then the second parent. Other values are not defined.</param>
/// <param name="combineDisjointExcessFlag">If this is true, add disjoint and excess genes to the child; otherwise, leave them out.</param>
/// <param name="np">Not used.</param>
    public void createOffspringSexual_ProcessCorrelationItem(
           List<NeatConnection> connectionList,
           Map<String, String> connectionTable,
           CorrelationItem correlationItem,
           int fitSwitch,
           boolean combineDisjointExcessFlag
    )
    {
        switch(correlationItem.correlationType)
        {
            // Disjoint and excess genes.
            case disjointConnectionGene:
            case excessConnectionGene:
            {
                // If the gene is in the fittest parent then override any existing entry in the connectionGeneTable.
                if(fitSwitch==1 && correlationItem.connection1!=null)
                {
                    createOffspringSexual_AddGene(connectionList, connectionTable, correlationItem.connection1, true);
                    return;
                }

                if(fitSwitch==2 && correlationItem.connection2!=null)
                {
                    createOffspringSexual_AddGene(connectionList, connectionTable, correlationItem.connection2, true);
                    return;
                }

                // The disjoint/excess gene is on the less fit parent.
                //if(Utilities.NextDouble() < np.pDisjointExcessGenesRecombined)	// Include the gene n% of the time from whichever parent contains it.
                if(combineDisjointExcessFlag)
                {
                    if(correlationItem.connection1!=null)
                    {
                        createOffspringSexual_AddGene(connectionList, connectionTable, correlationItem.connection1, false);
                        return;
                    }
                    if(correlationItem.connection2!=null)
                    {
                        createOffspringSexual_AddGene(connectionList, connectionTable, correlationItem.connection2, false);
                        return;
                    }
                }
                break;
            }

            case matchedConnectionGenes:
            {
                if(MathUtils.singleThrow(0.5))
                {
                    // Override any existing entries in the table.
                    createOffspringSexual_AddGene(connectionList, connectionTable, correlationItem.connection1, true);
                }
                else
                {
                    // Override any existing entries in the table.
                    createOffspringSexual_AddGene(connectionList, connectionTable, correlationItem.connection2, true);
                }
                break;
            }
        }
    }


    /// <summary>
    /// Correlate the ConnectionGenes within the two ConnectionGeneLists - based upon innovation number.
    /// Return an ArrayList of ConnectionGene[2] structures - pairs of matching ConnectionGenes.
    /// </summary>
    /// <param name="list1"></param>
    /// <param name="list2"></param>
    /// <returns>Resulting correlation</returns>
    CorrelationResults correlateConnectionListsByInnovation(List<NeatConnection> list1, List<NeatConnection> list2)
    {
        CorrelationResults correlationResults = new CorrelationResults();

        //----- Test for special cases.
        if(list1.size() == 0 && list2.size() == 0)
        {	// Both lists are empty!
            return correlationResults;
        }

        if(list1.size() == 0)
        {	// All list2 genes are excess.
            correlationResults.correlationStatistics.excessConnectionCount = list2.size();

            for(NeatConnection connection : list2)
            {
                //add a bunch of excess genes to our new creation!
                correlationResults.correlationList.add(new CorrelationItem(CorrelationType.excessConnectionGene, null, connection));
            }

            //done with correlating al; genes since list1 is empty
            return correlationResults;
        }

        // i believe there is a bug in the C# code, but it's completely irrelevant cause you'll never have 0 connections and for it to be sensical!
        if(list2.size() == 0)
        {	// All list1 genes are excess.
            correlationResults.correlationStatistics.excessConnectionCount  = list1.size();

            for(NeatConnection connection : list1) {
                correlationResults.correlationList.add(new CorrelationItem(CorrelationType.excessConnectionGene, connection, null));
            }

            //done with correlating al; genes since list2 is empty
            return correlationResults;
        }

        //----- Both ConnectionGeneLists contain genes - compare the contents.
        int list1Idx=0;
        int list2Idx=0;
        NeatConnection connection1 = list1.get(list1Idx);
        NeatConnection connection2 = list2.get(list2Idx);

        for(;;)
        {

            if(cuid.isLessThan(connection2.gid, connection1.gid))
            {
                // connectionGene2 is disjoint.
                correlationResults.correlationList.add(new CorrelationItem(CorrelationType.disjointConnectionGene, null, connection2));
                correlationResults.correlationStatistics.disjointConnectionCount++;

                // Move to the next gene in list2.
                list2Idx++;
            }
            else if(connection1.gid.equals(connection2.gid))
            {
                correlationResults.correlationList.add(new CorrelationItem(CorrelationType.matchedConnectionGenes, connection1, connection2));
                correlationResults.correlationStatistics.connectionWeightDelta += Math.abs(connection1.weight-connection2.weight);
                correlationResults.correlationStatistics.matchingCount++;

                // Move to the next gene in both lists.
                list1Idx++;
                list2Idx++;
            }
            else // (connectionGene2.InnovationId > connectionGene1.InnovationId)
            {
                // connectionGene1 is disjoint.
                correlationResults.correlationList.add(new CorrelationItem(CorrelationType.disjointConnectionGene, connection1, null));
                correlationResults.correlationStatistics.disjointConnectionCount++;

                // Move to the next gene in list1.
                list1Idx++;
            }

            // Check if we have reached the end of one (or both) of the lists. If we have reached the end of both then
            // we execute the first if block - but it doesn't matter since the loop is not entered if both lists have
            // been exhausted.
            if(list1Idx >= list1.size())
            {
                // All remaining list2 genes are excess.
                for(; list2Idx<list2.size(); list2Idx++)
                {
                    correlationResults.correlationList.add(
                            new CorrelationItem(CorrelationType.excessConnectionGene,
                                    null,
                                    list2.get(list2Idx)));
                    correlationResults.correlationStatistics.excessConnectionCount++;
                }
                return correlationResults;
            }

            if(list2Idx >= list2.size())
            {
                // All remaining list1 genes are excess.
                for(; list1Idx<list1.size(); list1Idx++)
                {
                    correlationResults.correlationList.add(
                            new CorrelationItem(CorrelationType.excessConnectionGene,
                                    list1.get(list1Idx),
                                    null));
                    correlationResults.correlationStatistics.excessConnectionCount++;
                }
                return correlationResults;
            }

            connection1 = list1.get(list1Idx);
            connection2 = list2.get(list2Idx);
        }
    }
    public NeatGenome createOffspringSexual(NeatGenome otherParent, NeatParameters np) {

        if (otherParent == null)
            return null;

        // Build a list of connections in either this genome or the other parent.
        CorrelationResults correlationResults = correlateConnectionListsByInnovation(this.connections, otherParent.connections);

        //----- Connection Genes.
        // We will temporarily store the offspring's genes in newConnectionGeneList and keeping track of which genes
        // exist with newConnectionGeneTable. Here we ensure these objects are created, and if they already existed
        // then ensure they are cleared. Clearing existing objects is more efficient that creating new ones because
        // allocated memory can be re-used.

        // Key = connection key, value = index in newConnectionGeneList.
        Map<String, String> newConnectionTable = new HashMap<String, String>();

        //TODO: No 'capacity' constructor on CollectionBase. Create modified/custom CollectionBase.
        // newConnectionGeneList must be constructed on each call because it is passed to a new NeatGenome
        // at construction time and a permanent reference to the list is kept.
        List<NeatConnection> newConnectionList = new ArrayList<NeatConnection>();

        // A switch that stores which parent is fittest 1 or 2. Chooses randomly if both are equal. More efficient to calculate this just once.
        int fitSwitch;
        if (this.fitness > otherParent.fitness)
            fitSwitch = 1;
        else if (this.fitness < otherParent.fitness)
            fitSwitch = 2;
        else {    // Select one of the parents at random to be the 'master' genome during crossover.
            if (MathUtils.nextDouble() < 0.5)
                fitSwitch = 1;
            else
                fitSwitch = 2;
        }

        boolean combineDisjointExcessFlag = MathUtils.nextDouble() < np.pDisjointExcessGenesRecombined;

        // Loop through the correlationResults, building a table of ConnectionGenes from the parents that will make it into our
        // new [single] offspring. We use a table keyed on connection end points to prevent passing connections to the offspring
        // that may have the same end points but a different innovation number - effectively we filter out duplicate connections.
//        var idxBound = correlationResults.correlationList.length;
        for (CorrelationItem correlationItem : correlationResults.correlationList) {
            createOffspringSexual_ProcessCorrelationItem(
                    newConnectionList,
                    newConnectionTable,
                    correlationItem,
                    fitSwitch,
                    combineDisjointExcessFlag);

        }

        //----- Neuron Genes.
        // Build a neuronGeneList by analysing each connection's neuron end-point IDs.
        // This strategy has the benefit of eliminating neurons that are no longer connected too.
        // Remember to always keep all input, output and bias neurons though!
        List<NeatNode> newNodeList = new ArrayList<NeatNode>();

        // Keep a table of the NeuronGene ID's keyed by ID so that we can keep track of which ones have been added.
        // Key = innovation ID, value = null for some reason.
        Map<String, NeatNode> newNodeTable = new HashMap<String, NeatNode>();

        // Get the input/output neurons from this parent. All Genomes share these neurons, they do not change during a run.
//        idxBound = neuronGeneList.Count;

        for (NeatNode node : this.nodes) {
            if (node.nodeType != NodeType.hidden) {
                newNodeList.add(NeatNode.Copy(node));
                newNodeTable.put(node.gid, node);
            }
        }

        // Now analyse the connections to determine which NeuronGenes are required in the offspring.
        // Loop through every connection in the child, and add to the child those hidden neurons that are sources or targets of the connection.
//        idxBound = newConnectionGeneList.Count;


        Map<String, NeatNode> nodeLookup = CreateGIDNodeLookup(this.nodes);
        Map<String, NeatNode> otherNodeLookup = CreateGIDNodeLookup(otherParent.nodes);
//        var connLookup =  NeatGenome.Help.CreateGIDLookup(self.connections);

        for (NeatConnection connection : newConnectionList) {
            NeatNode node;

            if (!newNodeTable.containsKey(connection.sourceID)) {
                //TODO: DAVID proper activation function
                // We can safely assume that any missing NeuronGenes at this point are hidden heurons.

                if (nodeLookup.containsKey(connection.sourceID)) {
                    node = nodeLookup.get(connection.sourceID);
                    newNodeList.add(NeatNode.Copy(node));
                } else {
                    if (!otherNodeLookup.containsKey(connection.sourceID))
                        throw new RuntimeException("Connection references source node that does not exist in either parent: " + connection.toString());

                    node = otherNodeLookup.get(connection.sourceID);

                    newNodeList.add(NeatNode.Copy(node));
                }
                //newNeuronGeneList.Add(new NeuronGene(connectionGene.SourceNeuronId, NeuronType.Hidden, ActivationFunctionFactory.GetActivationFunction("SteepenedSigmoid")));
                newNodeTable.put(connection.sourceID, node);
            }

            if (!newNodeTable.containsKey(connection.targetID)) {
                //TODO: DAVID proper activation function
                // We can safely assume that any missing NeuronGenes at this point are hidden heurons.
                if (nodeLookup.containsKey(connection.targetID)) {
                    node = nodeLookup.get(connection.targetID);
                    newNodeList.add(NeatNode.Copy(node));
                } else {

                    if (!otherNodeLookup.containsKey(connection.targetID))
                        throw new Error("Connection references target node that does not exist in either parent: " + connection.toString());

                    node = otherNodeLookup.get(connection.targetID);

                    newNodeList.add(NeatNode.Copy(node));
                }

                //newNeuronGeneList.Add(new NeuronGene(connectionGene.TargetNeuronId, NeuronType.Hidden, ActivationFunctionFactory.GetActivationFunction("SteepenedSigmoid")));
                newNodeTable.put(connection.targetID, node);
            }
        }

        NeatNode[] sortedNodes = new NeatNode[newNodeList.size()];
        newNodeList.toArray(sortedNodes);

        // TODO: Inefficient code?
        Arrays.sort(sortedNodes, new Comparator<NeatNode>() {
            @Override
            public int compare(NeatNode o, NeatNode o2) {

                return cuid.isLessThan(o.gid, o2.gid) ?
                        -1 : //is less than -- a- b = -1
                        (o.gid.equals(o2.gid)) ? 0 : //is possible equal to or greater
                                1;//is greater than definitely
            }
        });

        // newConnectionGeneList is already sorted because it was generated by passing over the list returned by
        // CorrelateConnectionGeneLists() - which is always in order.
        return new NeatGenome(nextInnovationID(), new ArrayList<NeatNode>(Arrays.asList(sortedNodes)), newConnectionList, inputNodeCount, outputNodeCount);
    }

    public NeatGenome cloneGenome()
    {
        return NeatGenome.Copy(this, this.wid);
    }

    public NeatGenome cloneGenomeNewID()
    {
        return NeatGenome.Copy(this, nextInnovationID());
    }

    public double compatFormer(NeatGenome comparisonGenome, NeatParameters np) {
    /* A very simple way of implementing this routine is to call CorrelateConnectionGeneLists and to then loop
     * through the correlation items, calculating a compatibility score as we go. However, this routine
     * is heavily used and in performance tests was shown consume 40% of the CPU time for the core NEAT code.
     * Therefore this new routine has been rewritten with it's own version of the logic within
     * CorrelateConnectionGeneLists. This allows us to only keep comparing genes up to the point where the
     * threshold is passed. This also eliminates the need to build the correlation results list, this difference
     * alone is responsible for a 200x performance improvement when testing with a 1664 length genome!!
     *
     * A further optimisation is achieved by comparing the genes starting at the end of the genomes which is
     * where most disparities are located - new novel genes are always attached to the end of genomes. This
     * has the result of complicating the routine because we must now invoke additional logic to determine
     * which genes are excess and when the first disjoint gene is found. This is done with an extra integer:
     *
     * int excessGenesSwitch=0; // indicates to the loop that it is handling the first gene.
     *						=1;	// Indicates that the first gene was excess and on genome 1.
     *						=2;	// Indicates that the first gene was excess and on genome 2.
     *						=3;	// Indicates that there are no more excess genes.
     *
     * This extra logic has a slight performance hit, but this is minor especially in comparison to the savings that
     * are expected to be achieved overall during a NEAT search.
     *
     * If you have trouble understanding this logic then it might be best to work through the previous version of
     * this routine (below) that scans through the genomes from start to end, and which is a lot simpler.
     *
     */

        List<NeatConnection> list1 = this.connections;
        List<NeatConnection> list2 = comparisonGenome.connections;

//
//        var compatibility = 0;
//        var correlation = NeatGenome.Help.correlateConnectionListsByInnovation(list1, list2);
//        compatibility += correlation.correlationStatistics.excessConnectionCount*np.compatibilityExcessCoeff;
//        compatibility += correlation.correlationStatistics.disjointConnectionCount*np.compatibilityDisjointCoeff;
//        compatibility += correlation.correlationStatistics.connectionWeightDelta*np.compatibilityWeightDeltaCoeff;
//        return compatibility;


        int excessGenesSwitch = 0;

        // Store these heavily used values locally.
        int list1Count = list1.size();
        int list2Count = list2.size();

        //----- Test for special cases.
        if (list1Count == 0 && list2Count == 0) {    // Both lists are empty! No disparities, therefore the genomes are compatible!
            return 0.0;
        }

        if (list1Count == 0) {    // All list2 genes are excess.
            return ((list2.size() * np.compatibilityExcessCoeff));
        }

        if (list2Count == 0) {
            // All list1 genes are excess.
            return ((list1Count * np.compatibilityExcessCoeff));
        }

        //----- Both ConnectionGeneLists contain genes - compare the contents.
        double compatibility = 0.0;
        int list1Idx = list1Count - 1;
        int list2Idx = list2Count - 1;
        NeatConnection connection1 = list1.get(list1Idx);
        NeatConnection connection2 = list2.get(list2Idx);
        for (; ; ) {
            if (connection1.gid.equals(connection2.gid)) {
                // No more excess genes. It's quicker to set this every time than to test if is not yet 3.
                excessGenesSwitch = 3;

                // Matching genes. Increase compatibility by weight difference * coeff.
                compatibility += Math.abs(connection1.weight - connection2.weight) * np.compatibilityWeightDeltaCoeff;

                // Move to the next gene in both lists.
                list1Idx--;
                list2Idx--;
            } else if (!cuid.isLessThan(connection2.gid, connection1.gid)) {
                // Most common test case(s) at top for efficiency.
                if (excessGenesSwitch == 3) {    // No more excess genes. Therefore this mismatch is disjoint.
                    compatibility += np.compatibilityDisjointCoeff;
                } else if (excessGenesSwitch == 2) {    // Another excess gene on genome 2.
                    compatibility += np.compatibilityExcessCoeff;
                } else if (excessGenesSwitch == 1) {    // We have found the first non-excess gene.
                    excessGenesSwitch = 3;
                    compatibility += np.compatibilityDisjointCoeff;
                } else //if(excessGenesSwitch==0)
                {    // First gene is excess, and is on genome 2.
                    excessGenesSwitch = 2;
                    compatibility += np.compatibilityExcessCoeff;
                }

                // Move to the next gene in list2.
                list2Idx--;
            } else // (connectionGene2.InnovationId < connectionGene1.InnovationId)
            {
                // Most common test case(s) at top for efficiency.
                if (excessGenesSwitch == 3) {    // No more excess genes. Therefore this mismatch is disjoint.
                    compatibility += np.compatibilityDisjointCoeff;
                } else if (excessGenesSwitch == 1) {    // Another excess gene on genome 1.
                    compatibility += np.compatibilityExcessCoeff;
                } else if (excessGenesSwitch == 2) {    // We have found the first non-excess gene.
                    excessGenesSwitch = 3;
                    compatibility += np.compatibilityDisjointCoeff;
                } else //if(excessGenesSwitch==0)
                {    // First gene is excess, and is on genome 1.
                    excessGenesSwitch = 1;
                    compatibility += np.compatibilityExcessCoeff;
                }

                // Move to the next gene in list1.
                list1Idx--;
            }


            // Check if we have reached the end of one (or both) of the lists. If we have reached the end of both then
            // we execute the first 'if' block - but it doesn't matter since the loop is not entered if both lists have
            // been exhausted.
            if (list1Idx < 0) {
                // All remaining list2 genes are disjoint.
                compatibility += (list2Idx + 1) * np.compatibilityDisjointCoeff;
                return (compatibility); //< np.compatibilityThreshold);
            }

            if (list2Idx < 0) {
                // All remaining list1 genes are disjoint.
                compatibility += (list1Idx + 1) * np.compatibilityDisjointCoeff;
                return (compatibility); //< np.compatibilityThreshold);
            }

            connection1 = list1.get(list1Idx);
            connection2 = list2.get(list2Idx);
        }
    }


    public double compat(NeatGenome comparisonGenome, NeatParameters np) {

        List<NeatConnection> list1 = this.connections;
        List<NeatConnection> list2 = comparisonGenome.connections;

        double compatibility = 0;
        CorrelationResults correlation = correlateConnectionListsByInnovation(list1, list2);
        compatibility += correlation.correlationStatistics.excessConnectionCount * np.compatibilityExcessCoeff;
        compatibility += correlation.correlationStatistics.disjointConnectionCount * np.compatibilityDisjointCoeff;
        compatibility += correlation.correlationStatistics.connectionWeightDelta * np.compatibilityWeightDeltaCoeff;
        return compatibility;

    }


    public boolean isCompatibleWithGenome(NeatGenome comparisonGenome, NeatParameters np) {
        return (compat(comparisonGenome, np) < np.compatibilityThreshold);
    }

    public boolean InOrderInnovation(List<NeatConnection> aObj) {
        String prevId = null;

        for (NeatConnection connection : aObj) {
            if (cuid.isLessThan(connection.gid, prevId))
                return false;

            prevId = connection.gid;
        }

        return true;
    }


    /// <summary>
    /// For debug purposes only.
    /// </summary>
    /// <returns>Returns true if genome integrity checks out OK.</returns>
    public boolean performIntegrityCheck() {
        return InOrderInnovation(this.connections);
    }

    public int mutate(Map<String, NeuronGeneStruct> newNodeTable, Map<String, String> newConnectionTable, NeatParameters np) {
        // Determine the type of mutation to perform.
        double[] probabilities = new double[6];

        probabilities[0] = (np.pMutateAddNode);
//        probabilities.push(0);//np.pMutateAddModule);
        probabilities[1] = (np.pMutateAddConnection);
        probabilities[2] = (np.pMutateDeleteConnection);
        probabilities[3] = (np.pMutateDeleteSimpleNeuron);
        probabilities[4] = (np.pMutateConnectionWeights);
        probabilities[5] = (np.pMutateChangeActivations);

        int outcome = MathUtils.singleThrowArray(probabilities);
        switch (outcome) {
            case 0:
                mutate_AddNode(newNodeTable, null);
                return 0;
            case 1:
//               self.mutate_Ad Mutate_AddModule(ea);
                mutate_AddConnection(newConnectionTable, np, null);
                return 1;
            case 2:
                mutate_DeleteConnection(null);
                return 2;
            case 3:
                mutate_DeleteSimpleNeuronStructure(newConnectionTable, np);
                return 3;
            case 4:
                mutate_ConnectionWeights(np);
                return 4;
            case 5:
                mutate_ChangeActivation(np);
                return 5;
            default:
                throw new RuntimeException("Incorrect mutation selection: " + outcome);
        }
    }

    //NeuronGene creator
/// <summary>
/// Add a new node to the Genome. We do this by removing a connection at random and inserting
/// a new node and two new connections that make the same circuit as the original connection.
///
/// This way the new node is properly integrated into the network from the outset.
/// </summary>
/// <param name="ea"></param>
    void mutate_AddNode(Map<String, NeuronGeneStruct> newNodeTable, NeatConnection connToSplit) {
        if (this.connections.size() == 0)
            return;

        // Select a connection at random.
        int connectionToReplaceIdx = MathUtils.next(this.connections.size());
        NeatConnection connectionToReplace = (connToSplit == null ? this.connections.get(connectionToReplaceIdx) : connToSplit);

        // Delete the existing connection. JOEL: Why delete old connection?
        //connectionGeneList.RemoveAt(connectionToReplaceIdx);

        // Check if this connection has already been split on another genome. If so then we should re-use the
        // neuron ID and two connection ID's so that matching structures within the population maintain the same ID.
        NeuronGeneStruct existingNeuronGeneStruct = null;
        if (newNodeTable.containsKey(connectionToReplace.gid))
            existingNeuronGeneStruct = newNodeTable.get(connectionToReplace.gid);

        NeatNode newNode;
        NeatConnection newConnection1;
        NeatConnection newConnection2;
        String actFunct;

        Map<String, NeatNode> nodeLookup = CreateGIDNodeLookup(this.nodes);

        //we could attempt to mutate the same node TWICE -- causing big issues, since we'll double add that node

        int acnt = 0;
        int attempts = 5;
        //while we
        while (acnt++ < attempts && newNodeTable.containsKey(connectionToReplace.gid)
                && (existingNeuronGeneStruct != null && nodeLookup.containsKey(existingNeuronGeneStruct.node.gid))) {
            connectionToReplaceIdx = MathUtils.next(this.connections.size());
            connectionToReplace = (connToSplit == null ? this.connections.get(connectionToReplaceIdx) : connToSplit);
            if (newNodeTable.containsKey(connectionToReplace.gid))
                existingNeuronGeneStruct = newNodeTable.get(connectionToReplace.gid);
        }

        //we have failed to produce a new node to split!
        if (acnt == attempts && newNodeTable.containsKey(connectionToReplace.gid)
                && (existingNeuronGeneStruct != null && nodeLookup.containsKey(existingNeuronGeneStruct.node.gid)))
            return;

        if (!newNodeTable.containsKey(connectionToReplace.gid)) {    // No existing matching structure, so generate some new ID's.

            //TODO: DAVID proper random activation function
            // Replace connectionToReplace with two new connections and a neuron.
            actFunct = CPPNActivationFactory.getRandomActivationFunction();
            //newNeuronGene = new NeuronGene(ea.NextInnovationId, NeuronType.Hidden, actFunct);

            String nextID = nextInnovationID();//connectionToReplace.gid);

            newNode = new NeatNode(nextID, actFunct,
                    (nodeLookup.get(connectionToReplace.sourceID).layer + nodeLookup.get(connectionToReplace.targetID).layer) / 2.0,
                    NodeType.hidden);

            nextID = nextInnovationID();
            newConnection1 = new NeatConnection(nextID, 1.0, connectionToReplace.sourceID, newNode.gid);

            nextID = nextInnovationID();
            newConnection2 = new NeatConnection(nextID, connectionToReplace.weight, newNode.gid, connectionToReplace.targetID);

            // Register the new ID's with NewNeuronGeneStructTable.
            newNodeTable.put(connectionToReplace.gid, NeuronGeneStruct.Create(newNode, newConnection1, newConnection2));
        } else {    // An existing matching structure has been found. Re-use its ID's

            //TODO: DAVID proper random activation function
            // Replace connectionToReplace with two new connections and a neuron.
            actFunct = CPPNActivationFactory.getRandomActivationFunction();

            //this exists
            NeuronGeneStruct tmpStruct = newNodeTable.get(connectionToReplace.gid);

            //newNeuronGene = new NeuronGene(tmpStruct.NewNeuronGene.InnovationId, NeuronType.Hidden, actFunct);
            newNode = NeatNode.Copy(tmpStruct.node);
            newNode.nodeType = NodeType.hidden;
            //new NeuronGene(null, tmpStruct.NewNeuronGene.gid, tmpStruct.NewNeuronGene.Layer, NeuronType.Hidden, actFunct, this.step);

            newConnection1 = new NeatConnection(
                    tmpStruct.connection1.gid,
                    1.0, connectionToReplace.sourceID,
                    newNode.gid);
//                new ConnectionGene(tmpStruct.NewConnectionGene_Input.gid, connectionToReplace.SourceNeuronId, newNeuronGene.gid, 1.0);
            newConnection2 = new NeatConnection(
                    tmpStruct.connection2.gid,
                    connectionToReplace.weight,
                    newNode.gid,
                    connectionToReplace.targetID);
//                new ConnectionGene(tmpStruct.NewConnectionGene_Output.gid, newNeuronGene.gid, connectionToReplace.TargetNeuronId, connectionToReplace.Weight);
        }

        // Add the new genes to the genome.
        this.nodes.add(newNode);
        insertByInnovation(newConnection1, this.connections);
        insertByInnovation(newConnection2, this.connections);
    }

    NeatConnection testForExistingConnectionInnovation(String sourceID, String targetID)
    {
        for(NeatConnection connection : this.connections){
            if(connection.sourceID.equals(sourceID) && connection.targetID.equals(targetID)){
                return connection;
            }
        }

        return null;
    }

    //messes with the activation functions
    void mutate_ChangeActivation(NeatParameters np) {
        //let's select a node at random (so long as it's not an input)
        for (int i = 0; i < this.nodes.size(); i++) {
            //not going to change the inputs
            if (i < this.inputAndBiasNodeCount)
                continue;

            if (MathUtils.nextDouble() < np.pNodeMutateActivationRate) {
                this.nodes.get(i).activationFunction = CPPNActivationFactory.getRandomActivationFunction();
            }
        }
    }

    //add a connection, sourcetargetconnect specifies the source, target or both nodes you'd like to connect (optionally)
    boolean mutate_AddConnection(Map<String, String> newConnectionTable,
                              NeatParameters np,
                              Pair<NeatNode, NeatNode> sourceTargetConnect) {
        //if we didn't send specifics, we don't use source/target connection

        // We are always guaranteed to have enough neurons to form connections - because the input/output neurons are
        // fixed. Any domain that doesn't require input/outputs is a bit nonsensical!

        // Make a fixed number of attempts at finding a suitable connection to add.

        if (this.nodes.size() > 1) {    // At least 2 neurons, so we have a chance at creating a connection.

            for (int attempts = 0; attempts < 5; attempts++) {
                // Select candidate source and target neurons. Any neuron can be used as the source. Input neurons
                // should not be used as a target
                int srcNeuronIdx;
                int tgtNeuronIdx;

                // Find all potential inputs, or quit if there are not enough.
                // Neurons cannot be inputs if they are dummy input nodes of a module.
                List<NeatNode> potentialInputs = new ArrayList<NeatNode>();

                for (NeatNode n : this.nodes) {
                    if (!n.activationFunction.equals("ModuleInputNeuron"))
                        potentialInputs.add(n);
                }


                if (potentialInputs.size() < 1)
                    return false;

                List<NeatNode> potentialOutputs = new ArrayList<NeatNode>();

                // Find all potential outputs, or quit if there are not enough.
                // Neurons cannot be outputs if they are dummy input or output nodes of a module, or network input or bias nodes.
                for (NeatNode n : this.nodes) {
                    if (n.nodeType != NodeType.bias && n.nodeType != NodeType.input &&
                            !n.activationFunction.equals("ModuleInputNeuron")
                            && !n.activationFunction.equals("ModuleOutputNeuron"))
                        potentialOutputs.add(n);
                }

                if (potentialOutputs.size() < 1)
                    return false;

                NeatNode sourceNeuron; //= sourceTargetConnect.source || potentialInputs[utilities.next(potentialInputs.length)];
                NeatNode targetNeuron; //= sourceTargetConnect.target || potentialOutputs[utilities.next(potentialOutputs.length)];

                if (sourceTargetConnect != null) {
                    sourceNeuron = sourceTargetConnect.first;
                    targetNeuron = sourceTargetConnect.second;
                } else {
                    sourceNeuron = potentialInputs.get(MathUtils.next(potentialInputs.size()));
                    targetNeuron = potentialOutputs.get(MathUtils.next(potentialOutputs.size()));
                }

                // Check if a connection already exists between these two neurons.
                String sourceID = sourceNeuron.gid;
                String targetID = targetNeuron.gid;

                //we don't allow recurrent connections, we can't let the target layers be <= src
                if (np.disallowRecurrence && targetNeuron.layer <= sourceNeuron.layer)
                    continue;

                if (testForExistingConnectionInnovation(sourceID, targetID) == null) {
                    // Check if a matching mutation has already occured on another genome.
                    // If so then re-use the connection ID.
                    String connectionKey = "(" + sourceID + "," + targetID + ")";
//                String existingConnection = newConnectionTable.get(connectionKey);
                    NeatConnection newConnection;
                    String nextID = nextInnovationID();

                    if (!newConnectionTable.containsKey(connectionKey)) {    // Create a new connection with a new ID and add it to the Genome.
                        newConnection = new NeatConnection(nextID,
                                (MathUtils.nextDouble() * np.connectionWeightRange / 4.0) - np.connectionWeightRange / 8.0,
                                sourceID, targetID);

//                            new ConnectionGene(ea.NextInnovationId, sourceID, targetID,
//                            (Utilities.NextDouble() * ea.NeatParameters.connectionWeightRange/4.0) - ea.NeatParameters.connectionWeightRange/8.0);

                        // Register the new connection with NewConnectionGeneTable.
                        newConnectionTable.put(connectionKey, nextID);

                        // Add the new gene to this genome. We have a new ID so we can safely append the gene to the end
                        // of the list without risk of breaking the innovation ID order.
                        this.connections.add(newConnection);
                    } else {
                        String existingConnectionID = newConnectionTable.get(connectionKey);
                        // Create a new connection, re-using the ID from existingConnection, and add it to the Genome.
                        newConnection = new NeatConnection(existingConnectionID,
                                (MathUtils.nextDouble() * np.connectionWeightRange / 4.0) - np.connectionWeightRange / 8.0,
                                sourceID, targetID);

//                            new ConnectionGene(existingConnection.InnovationId, sourceId, targetID,
//                            (Utilities.NextDouble() * ea.NeatParameters.connectionWeightRange/4.0) - ea.NeatParameters.connectionWeightRange/8.0);

                        // Add the new gene to this genome. We are re-using an ID so we must ensure the connection gene is
                        // inserted into the correct position (sorted by innovation ID).
                        insertByInnovation(newConnection, this.connections);
//                        connectionGeneList.InsertIntoPosition(newConnection);
                    }


                    return true;
                }
            }
        }

        // We couldn't find a valid connection to create. Instead of doing nothing lets perform connection
        // weight mutation.
        mutate_ConnectionWeights(np);

        return false;
    }

    public void mutate_ConnectionWeights(NeatParameters np) {
        // Determine the type of weight mutation to perform.
        double[] probabilties = new double[np.connectionMutationParameterGroupList.size()];

        int ix = 0;

        for (ConnectionMutationParameterGroup connMut : np.connectionMutationParameterGroupList) {
            probabilties[ix++] = connMut.ActivationProportion;
        }

        // Get a reference to the group we will be using.
        ConnectionMutationParameterGroup paramGroup =
                np.connectionMutationParameterGroupList.get(MathUtils.singleThrowArray(probabilties));

        // Perform mutations of the required type.
        if (paramGroup.SelectionType == NeatParameters.ConnectionSelectionType.proportional) {
            boolean mutationOccured = false;
            int connectionCount = this.connections.size();

            for (NeatConnection connection : this.connections) {
                if (MathUtils.nextDouble() < paramGroup.Proportion) {
                    mutateConnectionWeight(connection, np, paramGroup);
                    mutationOccured = true;
                }

            }

            if (!mutationOccured && connectionCount > 0) {    // Perform at least one mutation. Pick a gene at random.
                mutateConnectionWeight(this.connections.get(MathUtils.next(connectionCount)), // (Utilities.NextDouble() * connectionCount)],
                        np,
                        paramGroup);
            }
        } else // if(paramGroup.SelectionType==ConnectionSelectionType.FixedQuantity)
        {
            // Determine how many mutations to perform. At least one - if there are any genes.
            int connectionCount = this.connections.size();

            int mutations = Math.min(connectionCount, Math.max(1, paramGroup.Quantity));
            if (mutations == 0) return;

            // The mutation loop. Here we pick an index at random and scan forward from that point
            // for the first non-mutated gene. This prevents any gene from being mutated more than once without
            // too much overhead. In fact it's optimal for small numbers of mutations where clashes are unlikely
            // to occur.
            for (int i = 0; i < mutations; i++) {
                // Pick an index at random.
                int index = MathUtils.next(connectionCount);
                NeatConnection connection = this.connections.get(index);

                // Scan forward and find the first non-mutated gene.
                while (this.connections.get(index).IsMutated()) {    // Increment index. Wrap around back to the start if we go off the end.
                    if (++index == connectionCount)
                        index = 0;
                }

                // Mutate the gene at 'index'.
                mutateConnectionWeight(this.connections.get(index), np, paramGroup);
                this.connections.get(index).SetIsMutated(true);
            }

            for (NeatConnection connection : this.connections) {
                //reset if connection has been mutated, in case we go to do more mutations...
                connection.SetIsMutated(false);
            }
        }
    }

    void mutateConnectionWeight(NeatConnection connection, NeatParameters np, ConnectionMutationParameterGroup paramGroup) {
        switch (paramGroup.PerturbationType) {
            case jiggleEven:

            {
                connection.weight += (MathUtils.nextDouble() * 2 - 1.0) * paramGroup.PerturbationFactor;

                // Cap the connection weight. Large connections weights reduce the effectiveness of the search.
                connection.weight = Math.max(connection.weight, -np.connectionWeightRange / 2.0);
                connection.weight = Math.min(connection.weight, np.connectionWeightRange / 2.0);
                break;
            }
            //Paul - not implementing cause Randlib.gennor is a terribel terrible function
            //if i need normal distribution, i'll find another javascript source
//            case neatParameters.ConnectionPerturbationType.jiggleND:
//            {
//                connectionGene.weight += RandLib.gennor(0, paramGroup.Sigma);
//
//                // Cap the connection weight. Large connections weights reduce the effectiveness of the search.
//                connectionGene.weight = Math.max(connectionGene.weight, -np.connectionWeightRange/2.0);
//                connectionGene.weight = Math.min(connectionGene.weight, np.connectionWeightRange/2.0);
//                break;
//            }
            case reset: {
                // TODO: Precalculate connectionWeightRange / 2.
                connection.weight = (MathUtils.nextDouble() * np.connectionWeightRange) - np.connectionWeightRange / 2.0;
                break;
            }
            default: {
                throw new RuntimeException("Unexpected ConnectionPerturbationType");
            }
        }
    }

    /// <summary>
    /// If the neuron is a hidden neuron and no connections connect to it then it is redundant.
    /// No neuron is redundant that is part of a module (although the module itself might be found redundant separately).
    /// </summary>
        boolean isNeuronRedundant(Map<String, NeatNode> nodeLookup, String nid)
    {
        NeatNode node = nodeLookup.get(nid);
        if (node.nodeType != NodeType.hidden
                || node.activationFunction.equals("ModuleInputNeuron")
                || node.activationFunction.equals("ModuleOutputNeuron"))
            return false;

        return !this.isNeuronConnected(nid);
    }

    boolean isNeuronConnected(String nid)
    {
        for(NeatConnection connection : this.connections)
        {
            if(connection.sourceID.equals(nid))
                return true;

            if(connection.targetID.equals(nid))
                return true;
        }

        return false;
    }

    void mutate_DeleteConnection(NeatConnection connection)
    {
        if(this.connections.size() ==0)
            return;

        this.nodeLookup = CreateGIDNodeLookup(this.nodes);

        // Select a connection at random.
        int connectionToDeleteIdx = MathUtils.next(this.connections.size());

        if(connection != null){
            for(int i=0; i < this.connections.size(); i++)
            {
                NeatConnection innerConnection = this.connections.get(i);
                if(connection.gid.equals(innerConnection.gid))
                {
                    connectionToDeleteIdx = i;
                    break;
                }
            }
        }

        NeatConnection connectionToDelete = (connection  != null ? connection : this.connections.get(connectionToDeleteIdx));

        // Delete the connection.
        this.connections.remove(connectionToDeleteIdx);

        int srcIx = -1;
        int tgtIx = -1;

        for(int i=0; i < this.nodes.size(); i++)
        {
            NeatNode node = this.nodes.get(i);

            if(node.gid.equals(connectionToDelete.sourceID))
                srcIx = i;

            if(node.gid.equals(connectionToDelete.targetID))
                tgtIx = i;
        }

        // Remove any neurons that may have been left floating.
        if(isNeuronRedundant(this.nodeLookup, connectionToDelete.sourceID)){
            this.nodes.remove(srcIx);//(connectionToDelete.sourceID);
        }

        // Recurrent connection has both end points at the same neuron!
        if(!connectionToDelete.sourceID.equals(connectionToDelete.targetID)){
            if(isNeuronRedundant(this.nodeLookup, connectionToDelete.targetID))
                this.nodes.remove(tgtIx);//neuronGeneList.Remove(connectionToDelete.targetID);
        }
    }

    public static class NodeLookupHelper
    {
        public NeatNode node;
        public List<NeatConnection> incoming = new ArrayList<NeatConnection>();
        public List<NeatConnection> outgoing = new ArrayList<NeatConnection>();

        public NodeLookupHelper(NeatNode node)
        {
            this.node = node;
        }
    }

    public static void BuildNeuronConnectionLookupTable_NewConnection(
            Map<String, NodeLookupHelper> nodeConnectionLookup,
            Map<String, NeatNode> nodeTable,
            String gid,
            NeatConnection connection,
            boolean isIncoming)
    {
        // Is this neuron already known to the lookup table?
        NodeLookupHelper lookup;

        if(!nodeConnectionLookup.containsKey(gid))
        {
            //Create a new lookup entry for this neuron Id.
            lookup = new NodeLookupHelper(nodeTable.get(gid));
            nodeConnectionLookup.put(gid, lookup);
        }
        else
            lookup = nodeConnectionLookup.get(gid);

        // Register the connection with the NeuronConnectionLookup object.
        if(isIncoming)
            lookup.incoming.add(connection);
        else
            lookup.outgoing.add(connection);
    }

    Map<String, NodeLookupHelper> buildNeuronConnectionLookupTable()
    {
        this.nodeLookup = CreateGIDNodeLookup(this.nodes);

        Map<String, NodeLookupHelper> nodeConnectionLookup = new HashMap<String, NodeLookupHelper>();

        for(NeatConnection connection : this.connections)
        {
            //what node is this connection's target? That makes this an incoming connection
            NeatGenome.BuildNeuronConnectionLookupTable_NewConnection(nodeConnectionLookup,
                    this.nodeLookup, connection.targetID, connection, true);

            //what node is this connectinon's source? That makes this an outgoing connection for the node
            NeatGenome.BuildNeuronConnectionLookupTable_NewConnection(nodeConnectionLookup,
                    this.nodeLookup, connection.sourceID, connection, false);
        }

        return nodeConnectionLookup;
    }


    /// <summary>
    /// We define a simple neuron structure as a neuron that has a single outgoing or single incoming connection.
    /// With such a structure we can easily eliminate the neuron and shift it's connections to an adjacent neuron.
    /// If the neuron's non-linearity was not being used then such a mutation is a simplification of the network
    /// structure that shouldn't adversly affect its functionality.
    /// </summary>
    boolean mutate_DeleteSimpleNeuronStructure(Map<String, String> newConnectionTable, NeatParameters np)
    {

        // We will use the NeuronConnectionLookupTable to find the simple structures.
        Map<String, NodeLookupHelper> nodeConnectionLookup = buildNeuronConnectionLookupTable();


        // Build a list of candidate simple neurons to choose from.
        List<String> simpleNeuronIdList = new ArrayList<String>();

        for(String lookupKey : nodeConnectionLookup.keySet())
        {
            NodeLookupHelper lookup = nodeConnectionLookup.get(lookupKey);

            if(lookup == null || lookup.node == null || lookup.incoming == null || lookup.outgoing == null)
                continue;

            // If we test the connection count with <=1 then we also pick up neurons that are in dead-end circuits,
            // RemoveSimpleNeuron is then able to delete these neurons from the network structure along with any
            // associated connections.
            // All neurons that are part of a module would appear to be dead-ended, but skip removing them anyway.
            if (lookup.node.nodeType == NodeType.hidden
                    && !(lookup.node.activationFunction.equals("ModuleInputNeuron"))
                    && !(lookup.node.activationFunction.equals("ModuleOutputNeuron"))) {
                if((lookup.incoming.size() <= 1) || (lookup.outgoing.size() <= 1))
                    simpleNeuronIdList.add(lookup.node.gid);
            }
        }

        // Are there any candiate simple neurons?
        if(simpleNeuronIdList.size() == 0)
        {	// No candidate neurons. As a fallback lets delete a connection.
            mutate_DeleteConnection(null);
            return false;
        }

        // Pick a simple neuron at random.
        int idx = MathUtils.next(simpleNeuronIdList.size());//Math.floor(utilities.nextDouble() * simpleNeuronIdList.length);
        String nid = simpleNeuronIdList.get(idx);
        removeSimpleNeuron(nodeConnectionLookup, nid, newConnectionTable, np);

        return true;
    }

    void removeSimpleNeuron(Map<String, NodeLookupHelper> nodeConnectionLookup,
                            String nid,
                            Map<String, String> newConnectionTable,
                            NeatParameters np)
    {
        // Create new connections that connect all of the incoming and outgoing neurons
        // that currently exist for the simple neuron.
        NodeLookupHelper lookup = nodeConnectionLookup.get(nid);

        for(NeatConnection incomingConnection : lookup.incoming)
        {
            for(NeatConnection outgoingConnection : lookup.incoming)
            {
                if(testForExistingConnectionInnovation(incomingConnection.sourceID, outgoingConnection.targetID) == null)
                {	// Connection doesnt already exists.

                    // Test for matching connection within NewConnectionGeneTable.
                    String connectionKey =  "(" + incomingConnection.sourceID + "," + outgoingConnection.targetID + ")";

                    //new ConnectionEndpointsStruct(incomingConnection.SourceNeuronId,
//                   outgoi//ngConnection.TargetNeuronId);


                    NeatConnection newConnection;
                    String nextID = nextInnovationID();

                    if(!newConnectionTable.containsKey(connectionKey))
                    {	// No matching connection found. Create a connection with a new ID.
                        newConnection = new NeatConnection(nextID,
                                (MathUtils.nextDouble() * np.connectionWeightRange) - np.connectionWeightRange/2.0,
                                incomingConnection.sourceID, outgoingConnection.targetID);
//                           new ConnectionGene(ea.NextInnovationId,
//                           incomingConnection.SourceNeuronId,
//                           outgoingConnection.TargetNeuronId,
//                           (Utilities.NextDouble() * ea.NeatParameters.connectionWeightRange) - ea.NeatParameters.connectionWeightRange/2.0);

                        // Register the new ID with NewConnectionGeneTable.
                        newConnectionTable.put(connectionKey, nextID);

                        // Add the new gene to the genome.
                        this.connections.add(newConnection);
                    }
                    else
                    {
                        String existingConnection = newConnectionTable.get(connectionKey);

                        // Matching connection found. Re-use its ID.
                        newConnection = new NeatConnection(existingConnection,
                                (MathUtils.nextDouble() * np.connectionWeightRange) - np.connectionWeightRange/2.0,
                                incomingConnection.sourceID, outgoingConnection.targetID);

                        // Add the new gene to the genome. Use InsertIntoPosition() to ensure we don't break the sort
                        // order of the connection genes.
                        insertByInnovation(newConnection, this.connections);
                    }

                }
            }

        }

        for(NeatConnection incomingConnection : lookup.incoming)
        {
            for(int i=0; i < this.connections.size(); i++)
            {
                if(this.connections.get(i).gid.equals(incomingConnection.gid))
                {
                    this.connections.remove(i);
                    break;
                }
            }
        }

        for(NeatConnection outgoingConnection : lookup.incoming)
        {
            if(!outgoingConnection.targetID.equals(nid))
            {
                for(int i=0; i < this.connections.size(); i++)
                {
                    if(this.connections.get(i).gid.equals(outgoingConnection.gid))
                    {
                        this.connections.remove(i);
                        break;
                    }
                }
            }
        }

        // Delete the simple neuron - it no longer has any connections to or from it.
        for(int i=0; i < this.nodes.size(); i++)
        {
            if(this.nodes.get(i).gid.equals(nid))
            {
                this.nodes.remove(i);
                break;
            }
        }
    }
}