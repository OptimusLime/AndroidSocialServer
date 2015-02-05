package eplex.win.FastCPPNJava.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eplex.win.FastCPPNJava.activation.ActivationFunction;
import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;

public class CPPN {

    // must be in the same order as neuronSignals. Has null entries for neurons that are inputs or outputs of a module.
    ActivationFunction[] activationFunctions;

    // The modules and connections are in no particular order; only the order of the neuronSignals is used for input and output methods.
    //floatfastconnections
    List<CppnConnection> connections;

    /// The number of bias neurons, usually one but sometimes zero. This is also the index of the first input neuron in the neuron signals.
    int biasNeuronCount;
    /// The number of input neurons.
    int inputNeuronCount;
    /// The number of input neurons including any bias neurons. This is also the index of the first output neuron in the neuron signals.
    int totalInputNeuronCount;
    /// The number of output neurons.
    int outputNeuronCount;

    //save the total neuron count for us
    int totalNeuronCount;

    // For the following array, neurons are ordered with bias nodes at the head of the list,
    // then input nodes, then output nodes, and then hidden nodes in the array's tail.
    double[] neuronSignals;
    double[] modSignals;

    // This array is a parallel of neuronSignals, and only has values during SingleStepInternal().
    // It is declared here to avoid having to reallocate it for every network activation.
    double[] neuronSignalsBeingProcessed;

    double[] biasList;

    // For recursive activation, marks whether we have finished this node yet
    boolean[] activated;
    // For recursive activation, makes whether a node is currently being calculated. For recurrant connections
    boolean[] inActivation;
    // For recursive activation, the previous activation for recurrent connections
    double[] lastActivation;

    double[][] adjacentMatrix;
    int[][] adjacentList;
    int[][] reverseAdjacentList;

    public CPPN(
            int biasNeuronCount,
            int inputNeuronCount,
            int outputNeuronCount,
            int totalNeuronCount,
            List<CppnConnection> connections,
            double[] biasList,
            List<String> activationFunctions
            )
    {
        // must be in the same order as neuronSignals. Has null entries for neurons that are inputs or outputs of a module.
        this.activationFunctions = new ActivationFunction[activationFunctions.size()];
        int ix =0;
        for(String aFunc : activationFunctions) {
            this.activationFunctions[ix++] = CPPNActivationFactory.getActivationFunction(aFunc);
        }

        // The modules and connections are in no particular order; only the order of the neuronSignals is used for input and output methods.
        //floatfastconnections
        this.connections = connections;

        /// The number of bias neurons, usually one but sometimes zero. This is also the index of the first input neuron in the neuron signals.
        this.biasNeuronCount = biasNeuronCount;
        /// The number of input neurons.
        this.inputNeuronCount = inputNeuronCount;
        /// The number of input neurons including any bias neurons. This is also the index of the first output neuron in the neuron signals.
        this.totalInputNeuronCount = this.biasNeuronCount + this.inputNeuronCount;
        /// The number of output neurons.
        this.outputNeuronCount = outputNeuronCount;

        //save the total neuron count for us
        this.totalNeuronCount = totalNeuronCount;

        // For the following array, neurons are ordered with bias nodes at the head of the list,
        // then input nodes, then output nodes, and then hidden nodes in the array's tail.
        this.neuronSignals = new double[totalNeuronCount];
        this.modSignals = new double[totalNeuronCount];

        // This array is a parallel of neuronSignals, and only has values during SingleStepInternal().
        // It is declared here to avoid having to reallocate it for every network activation.
        this.neuronSignalsBeingProcessed = new double[totalNeuronCount];

        //initialize the neuron,mod, and processing signals
        for(int i=0; i < totalNeuronCount; i++){
            //either you are 1 for bias, or 0 otherwise
            neuronSignals[i] = (i < this.biasNeuronCount ? 1 : 0);
            modSignals[i] = 0;
            neuronSignalsBeingProcessed[i] = 0;
        }

        this.biasList = biasList;

        // For recursive activation, marks whether we have finished this node yet
        this.activated = new boolean[totalNeuronCount];
        // For recursive activation, makes whether a node is currently being calculated. For recurrant connections
        this.inActivation = new boolean[totalNeuronCount];
        // For recursive activation, the previous activation for recurrent connections
        this.lastActivation = new double[totalNeuronCount];


        this.adjacentList = new int[totalNeuronCount][];
        this.reverseAdjacentList = new int[totalNeuronCount][];
        this.adjacentMatrix = new double[totalNeuronCount][];

        int[] adjCount = new int[totalNeuronCount];
        int[] rAdjCount = new int[totalNeuronCount];

        for(CppnConnection connection : this.connections)
        {
            int crs = connection.sourceIdx;
            int crt = connection.targetIdx;

            // Holds outgoing nodes
            adjCount[crs]++;

            // Holds incoming nodes
            rAdjCount[crt]++;
        }


        //initialize the activated, in activation, previous activation
        for(int i=0; i < totalNeuronCount; i++) {

            //then we initialize our list of lists!
            int aCount = adjCount[i];
            if(aCount > 0)
                this.adjacentList[i] = new int[aCount];

            int rCount = rAdjCount[i];
            if(rCount > 0)
                this.reverseAdjacentList[i] = new int[rCount];

            //have to setup our weight matrix!
            this.adjacentMatrix[i] = new double[totalNeuronCount];
        }

        adjCount = new int[totalNeuronCount];
        rAdjCount = new int[totalNeuronCount];

        //finally
        // Set up adjacency list and matrix
        for(CppnConnection connection : this.connections)
        {
            int crs = connection.sourceIdx;
            int crt = connection.targetIdx;

            // Holds outgoing nodes
            int aCount = adjCount[crs];
            adjacentList[crs][aCount] = crt;
            adjCount[crs]++;

            // Holds incoming nodes
            int rCount = rAdjCount[crt];
            reverseAdjacentList[crt][rCount] = crs;
            rAdjCount[crt]++;

            //then hodl our weights!
            adjacentMatrix[crs][crt] = connection.weight;
        }

    }

    public void recursiveActivation(double[] signalsMinusBias){

        // Initialize boolean arrays and set the last activation signal, but only if it isn't an input (these have already been set when the input is activated)
        for (int i = 0; i < totalNeuronCount; i++)
        {
            inActivation[i] = false;

            //skip bias
            if(i < biasNeuronCount) {
                activated[i] = true;
            }
            else if(i < totalInputNeuronCount)
            {
                //set the signals here!
                neuronSignals[i] = signalsMinusBias[i - this.biasNeuronCount];
                activated[i] = true;
            }
            else
            {
                // Set as activated if i is an input node, otherwise ensure it is unactivated (false)
                activated[i] = false;
                lastActivation[i] = neuronSignals[i];
            }
        }

        // Get each output node activation recursively
        // NOTE: This is an assumption that genomes have started minimally, and the output nodes lie sequentially after the input nodes
        for (int i = 0; i < outputNeuronCount; i++)
            recursiveActivateNode(totalInputNeuronCount + i);

    }

    public double getOutputSignal(int index)
    {
        // For speed we don't bother with bounds checks.
        return this.neuronSignals[this.totalInputNeuronCount + index];
    }

    //we can dispense of this by accessing neuron signals directly
    public void clearSignals()
    {
        // Clear signals for input, hidden and output nodes. Only the bias node is untouched.
        for (int i = this.biasNeuronCount; i < this.neuronSignals.length; i++)
            neuronSignals[i] = 0.0;
    }

    void recursiveActivateNode(int currentNode)
    {
        // If we've reached an input node we return since the signal is already set
        if (activated[currentNode])
        {
            inActivation[currentNode] = false;
            return;
        }

        // Mark that the node is currently being calculated
        inActivation[currentNode] = true;

        // Set the presignal to 0
        neuronSignalsBeingProcessed[currentNode] = 0;

        if(reverseAdjacentList[currentNode] != null) {

            // Adjacency list in reverse holds incoming connections, go through each one and activate it
            for (int i = 0; i < reverseAdjacentList[currentNode].length; i++) {
                int crntAdjNode = reverseAdjacentList[currentNode][i];

                // If this node is currently being activated then we have reached a cycle, or recurrent connection.
                // Use the previous activation in this case
                if (inActivation[crntAdjNode]) {
                    neuronSignalsBeingProcessed[currentNode] +=
                            lastActivation[crntAdjNode] * adjacentMatrix[crntAdjNode][currentNode];
                }

                // Otherwise proceed as normal
                else {
                    // Recurse if this neuron has not been activated yet
                    if (!activated[crntAdjNode])
                        recursiveActivateNode(crntAdjNode);

                    // Add it to the new activation
                    neuronSignalsBeingProcessed[currentNode] +=
                            neuronSignals[crntAdjNode] * adjacentMatrix[crntAdjNode][currentNode];
                }
            }
        }

        // Mark this neuron as completed
        activated[currentNode] = true;

        // This is no longer being calculated (for cycle detection)
        inActivation[currentNode] = false;

        // Set this signal after running it through the activation function
        neuronSignals[currentNode] = activationFunctions[currentNode].calculate(neuronSignalsBeingProcessed[currentNode]);
    }

    boolean isRecurrent()
    {
        //if we're a hidden/output node (nodeid >= totalInputcount), and we connect to an input node (nodeid <= self.totalInputcount) -- it's recurrent!
        //if we are a self connection, duh we are recurrent
        for(CppnConnection connection : this.connections)
            if((connection.sourceIdx >= this.totalInputNeuronCount
                    && connection.targetIdx < this.totalInputNeuronCount)
                    || connection.sourceIdx == connection.targetIdx
                    )
                return true;

        boolean[] recursed = new boolean[totalInputNeuronCount];
        boolean[] inRecursiveCheck = new boolean[totalInputNeuronCount];

        for(int i=0; i < totalNeuronCount; i++)
        {
            recursed[i] = (i < totalInputNeuronCount);
        }

        // Get each output node activation recursively
        // NOTE: This is an assumption that genomes have started minimally, and the output nodes lie sequentially after the input nodes
        for (int i = 0; i < outputNeuronCount; i++){
            if(recursiveCheckRecurrent(totalInputNeuronCount + i, recursed, inRecursiveCheck))
            {
                return true;
            }
        }

        return false;

    }

    boolean recursiveCheckRecurrent(int currentNode, boolean[] recursed, boolean[] inRecursiveCheck)
    {
        //  If we've reached an input node we return since the signal is already set
        if (recursed[currentNode])
        {
            inRecursiveCheck[currentNode] = false;
            return false;
        }

        // Mark that the node is currently being calculated
        inRecursiveCheck[currentNode] = true;

        if(reverseAdjacentList[currentNode] != null) {
            // Adjacency list in reverse holds incoming connections, go through each one and activate it
            for (int i = 0; i < reverseAdjacentList[currentNode].length; i++) {
                int crntAdjNode = reverseAdjacentList[currentNode][i];

                //recurrent connection handling - not applicable in our implementation
                // If this node is currently being activated then we have reached a cycle, or recurrant connection. Use the previous activation in this case
                if (inRecursiveCheck[crntAdjNode]) {
                    inRecursiveCheck[currentNode] = false;
                    return true;
                }

                // Otherwise proceed as normal
                else {
                    boolean verifiedRecursive = false;
                    // Recurse if this neuron has not been activated yet
                    if (!recursed[crntAdjNode])
                        verifiedRecursive = recursiveCheckRecurrent(crntAdjNode, recursed, inRecursiveCheck);

                    if (verifiedRecursive)
                        return true;
                }
            }
        }

        // Mark this neuron as completed
        recursed[currentNode] = true;

        // This is no longer being calculated (for cycle detection)
        inRecursiveCheck[currentNode] = false;

        return false;
    }
}