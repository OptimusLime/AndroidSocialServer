package eplex.win.FastCPPNJava.network;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eplex.win.FastCPPNJava.activation.ActivationFunction;
import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Cos;
import eplex.win.FastCPPNJava.activation.functions.Gaussian;
import eplex.win.FastCPPNJava.activation.functions.Linear;
import eplex.win.FastCPPNJava.activation.functions.Sine;

public class CPPN {

    public static double DEFAULT_BIAS_VALUE = 1.0;

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

    //0.21 R + 0.72 G + 0.07 B
    public static final String THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_ACTIVATIONS = "" +
            "float Sine(float val){ return " + new Sine().gpuFunctionString() + " }\n" +
            "float BipolarSigmoid(float val){ return " + new BipolarSigmoid().gpuFunctionString() + " }\n" +
            "float Gaussian(float val){ return " + new Gaussian().gpuFunctionString() + " }\n" +
            "float Cos(float val){ return " + new Cos().gpuFunctionString() + " }\n" +
            "float Linear(float val){ return " + new Linear().gpuFunctionString() + " }\n" +
            "float grayscale(vec4 color){ return 0.21*color.x + 0.72*color.y + .07*color.z; }\n";


    public static final String THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_VARIABLES = "" +
            "precision highp float;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "uniform mediump mat3 convolutionMatrix;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 leftTextureCoordinate;\n" +
            "varying vec2 rightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 topTextureCoordinate;\n" +
            "varying vec2 topLeftTextureCoordinate;\n" +
            "varying vec2 topRightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 bottomTextureCoordinate;\n" +
            "varying vec2 bottomLeftTextureCoordinate;\n" +
            "varying vec2 bottomRightTextureCoordinate;\n";

    public static final String THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_MAIN_START = "" +
            "void main()\n" +
            "{\n" +
            "    mediump vec4 bottomColor = texture2D(inputImageTexture, bottomTextureCoordinate);\n" +
            "    mediump vec4 bottomLeftColor = texture2D(inputImageTexture, bottomLeftTextureCoordinate);\n" +
            "    mediump vec4 bottomRightColor = texture2D(inputImageTexture, bottomRightTextureCoordinate);\n" +
            "    mediump vec4 centerColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "    mediump vec4 leftColor = texture2D(inputImageTexture, leftTextureCoordinate);\n" +
            "    mediump vec4 rightColor = texture2D(inputImageTexture, rightTextureCoordinate);\n" +
            "    mediump vec4 topColor = texture2D(inputImageTexture, topTextureCoordinate);\n" +
            "    mediump vec4 topRightColor = texture2D(inputImageTexture, topRightTextureCoordinate);\n" +
            "    mediump vec4 topLeftColor = texture2D(inputImageTexture, topLeftTextureCoordinate);\n";

    public static final String THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_MAIN_INPUT_COLORS = "" +
            "bottomLeftColor, bottomColor, bottomRightColor, leftColor, centerColor, rightColor, topRightColor, topColor, topLeftColor";

    public static final String THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_MAIN_END = "" +
            "\n" +
//            "    gl_FragColor = resultColor;\n" +
//            "    gl_FragColor = vec4(resultColor.xyz, centerColor.w);\n" +
            "    gl_FragColor = vec4(resultColor.rgb, 1.0);\n" +
//            "    gl_FragColor = vec4(resultColor.xyz, 1.0);\n" +
            "}";

    static String nodeFunctionName(int ix)
    {
        return "#node_function-" + ix + "-$#";
    }
    static String inputFunctionName(int ix)
    {
        return "%input_function-" + ix + "-%";
    }
    static String biasFunctionName(int ix)
    {
        return "%bias_function-" + ix + "-%";
    }
    static String connectionName(int sourceIdx, int targetIdx)
    {
        return  sourceIdx + ","+ targetIdx;
    }

    static String shaderOutputFunctionName(int outputIx)
    {
        return "outputFunction_" + outputIx;
    }
    static String shaderHiddenFunctionName(int hiddenIx)
    {
        return "hiddenFunction_" + hiddenIx;
    }
    static String hiddenVariableName(int hiddenIx)
    {
        return "hidden_" + hiddenIx;
    }

    String hyperNEATShaderFunctionWrap(String functionName, String innerFunction, int vec4InputCount)
    {
        String shaderFunction = "float " + functionName + "(";
        for(int i=0; i < vec4InputCount; i++)
        {
            shaderFunction += "vec4 v" + i;
            if(i != vec4InputCount - 1)
                shaderFunction += ",";
            else //close up the function
                shaderFunction += "){";
        }

        shaderFunction += "\n";
        shaderFunction += "return " + innerFunction;
        shaderFunction += "\n}\n";

        //send it all back now!
        return  shaderFunction;
    }

    String hyperNEATShaderFunctionWrap(String functionName, String innerFunction, int hiddenNodeCount, int hiddenNodeStart)
    {
        String shaderFunction = "float " + functionName + "(";
        for(int i=0; i < hiddenNodeCount; i++)
        {
            shaderFunction += "float " + hiddenVariableName(i + hiddenNodeStart);
            if(i != hiddenNodeCount - 1)
                shaderFunction += ",";
            else //close up the function
                shaderFunction += "){";
        }

        shaderFunction += "\n";
        shaderFunction += "return " + innerFunction;
        shaderFunction += "\n}\n";

        //send it all back now!
        return  shaderFunction;
    }

    String shaderFunctionWrap(String functionName, String innerFunction, int inputCount)
    {
        String shaderFunction = "float " + functionName + "(";
        for(int i=0; i < inputCount; i++)
        {
            shaderFunction += "float x" + i;
            if(i != inputCount - 1)
                shaderFunction += ",";
            else //close up the function
                shaderFunction += "){";
        }

        shaderFunction += "\n";
        shaderFunction += "return " + innerFunction;
        shaderFunction += "\n}\n";

        //send it all back now!
        return  shaderFunction;
    }

    static String colorOutputFunction(String functionName, int inputCount, int outputCount, int totalInputNeuronCount)
    {
        String colorFunction = "vec4 " + functionName + "(";

        for(int i=0; i < inputCount; i++)
        {
            colorFunction += "vec4 x_" + i;
            if(i != inputCount - 1)
                colorFunction += ", ";
            else //close up the function
                colorFunction += "){";
        }

        colorFunction += "\n";

        //lets make grayscale conversions for all color inputs
        for(int i=0; i < inputCount; i++)
        {
            colorFunction += "float gs_x_" + i + " = grayscale(x_" + i + ");\n";
        }

        colorFunction += "\n";

        //now we're ready to activate the network
        for(int i=0; i < outputCount; i++)
        {
            colorFunction += "float output_" + i + " = (" + shaderOutputFunctionName(i + totalInputNeuronCount) + "(";
            for(int x=0; x < inputCount; x++)
            {
                colorFunction += "gs_x_" + x;
                if(x != inputCount - 1)
                    colorFunction += ", ";
                else //close up the function
                    colorFunction += ")";
            }

            //close up the abs function
            colorFunction += ");\n";

            //now we have each output as the product of its function
        }

        //we need to create our final color
        //this is how we interpret our outputs
        //we do basic stuff here just for an example

        colorFunction += "vec4 finalColor = ";

        for(int i=0; i < outputCount; i++) {
            colorFunction += "output_" + i + "*x_" + i;
            if(i != outputCount -1)
                colorFunction += " + ";
        }

        colorFunction += ";\n";

        colorFunction += "return finalColor;\n}\n";

        //all done up in this function!
        return colorFunction;
    }

    static String outputFunctionInputWrap(int ix, int hiddenNodeCount, int hiddenNodeStartIx)
    {
        String wrap = shaderOutputFunctionName(ix) + "(";
        for(int x=0; x < hiddenNodeCount; x++)
        {
            //read the .xyz from the inputs
            wrap += hiddenVariableName(hiddenNodeStartIx + x);
            if(x != hiddenNodeCount - 1)
                wrap += ", ";
            else //close up the function
                wrap += ")";
        }

        return wrap;
    }
    static String hiddenFunctionInputWrap(int ix, int inputCount)
    {
        String wrap = shaderHiddenFunctionName(ix) + "(";
        for(int x=0; x < inputCount; x++)
        {
            //read the .xyz from the inputs
            wrap += "v_" + x;
            if(x != inputCount - 1)
                wrap += ", ";
            else //close up the function
                wrap += ")";
        }

        return wrap;
    }
    static String hyperNEATColorOutputFunction(String functionName, int inputCount, int hiddenCount, int outputCount, int totalInputNeuronCount)
    {
        String colorFunction = "vec4 " + functionName + "(";

        for(int i=0; i < inputCount; i++)
        {
            colorFunction += "vec4 v_" + i;
            if(i != inputCount - 1)
                colorFunction += ", ";
            else //close up the function
                colorFunction += "){";
        }

//        colorFunction += "\n";
//
//        //lets make grayscale conversions for all color inputs
//        for(int i=0; i < inputCount; i++)
//        {
//            colorFunction += "float gs_x_" + i + " = grayscale(x_" + i + ");\n";
//        }

        colorFunction += "\n";

        int hiddenNodeStart = totalInputNeuronCount + outputCount;

        //now we're ready to activate the network
        for(int i=0; i < hiddenCount; i++)
        {
            colorFunction += "float " + hiddenVariableName(hiddenNodeStart + i) + " = " + hiddenFunctionInputWrap(hiddenNodeStart + i, inputCount);

            //close up the abs function
            colorFunction += ";\n";

            //now we have each output as the product of its function
        }

        for(int i=0; i < outputCount; i++)
        {
            colorFunction += "float output_" + i + " = " + outputFunctionInputWrap(totalInputNeuronCount + i, hiddenCount, hiddenNodeStart) + ";\n";
        }

        //we need to create our final color
        //this is how we interpret our outputs
        //we do basic stuff here just for an example
        colorFunction += "float alphaBlend = 0.0f;";
        colorFunction += "float omAlphaBlend = 1.0f - alphaBlend;";

        //we blend together previous pixel with new pixel
        colorFunction += "vec4 finalColor = vec4(" +
                "alphaBlend*v_4.x + omAlphaBlend*output_0, " +
                "alphaBlend*v_4.y + omAlphaBlend*output_1, " +
                "alphaBlend*v_4.z + omAlphaBlend*output_2, " +
                "1.0);";

//        colorFunction += "vec4 finalColor = vec4(output_0, output_1, output_2, 1.0);";

//        for(int i=0; i < outputCount; i++) {
//            colorFunction += "output_" + i + "*x_" + i;
//            if(i != outputCount -1)
//                colorFunction += " + ";
//        }
//
//        colorFunction += ";\n";

        colorFunction += "return finalColor;\n}\n";

        //all done up in this function!
        return colorFunction;
    }

    public String cppnToShader(boolean useHyperNEAT, String additionalActivations)
    {
        //shader bo bader
        String cppnShader = "";

        cppnShader += THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_VARIABLES;
        cppnShader += THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_ACTIVATIONS;
        if(additionalActivations != null)
            cppnShader += additionalActivations;

        Map<String, CppnConnection> connectionMap = new HashMap<String, CppnConnection>();
        for(int i=0; i < this.connections.size(); i++)
        {
            CppnConnection conn = this.connections.get(i);
            String cIx = connectionName(conn.sourceIdx, conn.targetIdx);
            connectionMap.put(cIx, conn);
        }

        //lets go through each node and create a string for it
        String[] nodeFunctions = new String[this.totalNeuronCount];

        //each node has a function
        for(int i =0; i < this.biasNeuronCount; i++)
            nodeFunctions[i] = biasFunctionName(i);

        for(int i =this.biasNeuronCount; i < this.totalInputNeuronCount; i++)
            nodeFunctions[i] = inputFunctionName(i);


        //now for other node functions, we only need to determine what their inputs are and the activation functions
        for(int i= this.totalInputNeuronCount; i < this.totalNeuronCount; i++)
        {
            //grab our inputs
            int[] incomingNodes = reverseAdjacentList[i];

            int tgtNode = i;

            if(incomingNodes == null || incomingNodes.length == 0) {
                nodeFunctions[tgtNode] = "0.0";
                continue;
            }

            //these are all of our incoming
            //let's build a function!
            String innerFunction = "";
            innerFunction += activationFunctions[i].functionID() + "(";

            for(int x=0; x < incomingNodes.length; x++)
            {
                //this is our src
                int srcNode = incomingNodes[x];

                //there is a connection between the src and tgt -- otherwise this wouldn't exist in the adjaceny list
                String cIx = connectionName(srcNode, tgtNode);

                //get our connection
                CppnConnection conn = connectionMap.get(cIx);

                if(x > 0)
                    innerFunction += " + ";

                if(srcNode < this.biasNeuronCount)
                    innerFunction += (float)conn.weight + "f*" + biasFunctionName(srcNode);
                else if(srcNode < this.totalInputNeuronCount)
                    innerFunction += (float)conn.weight + "f*" + inputFunctionName(srcNode);
                else
                    innerFunction += (float)conn.weight + "f*" + nodeFunctionName(srcNode);

            }

            innerFunction += ")";

            //now we have our inner function -- store it!
            nodeFunctions[tgtNode] = innerFunction;
        }

        String hyperNEATOutputVariables = "";
        int hiddenNodeStartIx = this.totalInputNeuronCount + this.outputNeuronCount;
        int hiddenNodeCount = this.totalNeuronCount - hiddenNodeStartIx;

        //now we have all the node functions -- we want to calculate the final node functions for the outputs
        if(useHyperNEAT)
        {
            hyperNEATOutputVariables = "";
            //go through all the hidden nodes -- these are the only functions we need to call in our final value
            for (int i = this.totalInputNeuronCount; i < this.totalNeuronCount; i++) {
                //we have our output node
                int hiddenIx = i;

                //check it out yo
                String hiddenFunction = nodeFunctions[hiddenIx];
                String[] componentFunctions;
                String reconstructed;

                //we go until we have replaced ALL the node functions
                while (hiddenFunction.contains("$")) {
                    //we need to replace any node functions with functions of the inputs and bias node strictly
                    componentFunctions = hiddenFunction.split("#");

                    reconstructed = "";

                    for (int s = 0; s < componentFunctions.length; s++) {
                        String inner = componentFunctions[s];
                        //if we're a node function, we must replace
                        if (inner.contains("node_function")) {
                            //get the node number
                            int replaceIx = Integer.parseInt(inner.split("-")[1]);
                            componentFunctions[s] = hiddenVariableName(replaceIx);
//                            componentFunctions[s] = hiddenFunctionInputWrap(replaceIx, this.inputNeuronCount/3);
//                            componentFunctions[s] = nodeFunctions[replaceIx];
                        }

                        //reconstruct the string!
                        reconstructed += componentFunctions[s];
                    }

                    //now replace
                    hiddenFunction = reconstructed;
                }

                //now save our hidden node functions -- these are our precious outputs of course!
                nodeFunctions[hiddenIx] = hiddenFunction;

                //now lets break it apart
                //we need to replace any node functions with functions of the inputs and bias node strictly
                componentFunctions = hiddenFunction.split("%");

                reconstructed = "";

                for (int s = 0; s < componentFunctions.length; s++) {
                    String inner = componentFunctions[s];
                    //if we're a node function, we must replace
                    if (inner.contains("input") || inner.contains("bias")) {
                        //convert to input ix
                        //get the node number
                        int nodeIx = Integer.parseInt(inner.split("-")[1]);

                        if (nodeIx < this.biasNeuronCount)
                            componentFunctions[s] = "" + DEFAULT_BIAS_VALUE;
                        else //what inputs to read from
                        {
                            int readVector = (nodeIx - this.biasNeuronCount) / 3;
                            int channel = (nodeIx - this.biasNeuronCount)%3;

                            //we need to read the rgb values separately from our pixel inputs
                            switch (channel)
                            {
                                case 0:
                                    componentFunctions[s] = "v" + readVector + ".x";
                                    break;
                                case 1:
                                    componentFunctions[s] = "v" + readVector + ".y";
                                    break;
                                case 2:
                                    componentFunctions[s] = "v" + readVector + ".z";
                                    break;
                            }
                        }
                    }
                    //that should simplify our input and bias functions -- down to either hardcoded values -- or input value
                    reconstructed += componentFunctions[s];
                }

                //finish it up!
                reconstructed += ";";

                //that's a full network!
                //Now we need to build the shader parts
                String wrappedFunction;

                //if we're an output neuron, call it an output name
                if(i - this.totalInputNeuronCount < this.outputNeuronCount)
                    wrappedFunction = hyperNEATShaderFunctionWrap(shaderOutputFunctionName(hiddenIx), reconstructed, hiddenNodeCount, hiddenNodeStartIx);
                else
                    wrappedFunction = hyperNEATShaderFunctionWrap(shaderHiddenFunctionName(hiddenIx), reconstructed, this.inputNeuronCount/3);


                //includes some spaces too, so no need to pad it out
                cppnShader += wrappedFunction;
            }
        }
        else {
            for (int i = this.totalInputNeuronCount; i < this.totalInputNeuronCount + this.outputNeuronCount; i++) {
                //we have our output node
                int outputIx = i;

                //check it out yo
                String outputFunction = nodeFunctions[outputIx];
                String[] componentFunctions;
                String reconstructed;

                //we go until we have replaced ALL the node functions
                while (outputFunction.contains("$")) {
                    //we need to replace any node functions with functions of the inputs and bias node strictly
                    componentFunctions = outputFunction.split("#");

                    reconstructed = "";

                    for (int s = 0; s < componentFunctions.length; s++) {
                        String inner = componentFunctions[s];
                        //if we're a node function, we must replace
                        if (inner.contains("node_function")) {
                            //get the node number
                            int replaceIx = Integer.parseInt(inner.split("-")[1]);
                            componentFunctions[s] = nodeFunctions[replaceIx];
                        }

                        //reconstruct the string!
                        reconstructed += componentFunctions[s];
                    }

                    //now replace
                    outputFunction = reconstructed;
                }

                //now save our output functions -- these are our precious outputs of course!
                nodeFunctions[outputIx] = outputFunction;

                //now lets break it apart
                //we need to replace any node functions with functions of the inputs and bias node strictly
                componentFunctions = outputFunction.split("%");

                reconstructed = "";

                for (int s = 0; s < componentFunctions.length; s++) {
                    String inner = componentFunctions[s];
                    //if we're a node function, we must replace
                    if (inner.contains("input") || inner.contains("bias")) {
                        //convert to input ix
                        //get the node number
                        int nodeIx = Integer.parseInt(inner.split("-")[1]);

                        if (nodeIx < this.biasNeuronCount)
                            componentFunctions[s] = "" + DEFAULT_BIAS_VALUE;
                        else //what inputs to read from
                            componentFunctions[s] = "x" + (nodeIx - this.biasNeuronCount);
                    }
                    //that should simplify our input and bias functions -- down to either hardcoded values -- or input value
                    reconstructed += componentFunctions[s];
                }

                //finish it up!
                reconstructed += ";";

                //that's a full network!
                //Now we need to build the shader parts
                String wrappedFunction = shaderFunctionWrap(shaderOutputFunctionName(outputIx), reconstructed, this.inputNeuronCount);

                //includes some spaces too, so no need to pad it out
                cppnShader += wrappedFunction;
            }
        }

        //we need to add in a final function, that will take the inputs of the convolution filter -- then get all the outputs
        //finally converting it into a pixel value
        String colorFunctionName = "pixelColor";

        if(useHyperNEAT)
            cppnShader += hyperNEATColorOutputFunction(colorFunctionName, inputNeuronCount/3, (this.totalNeuronCount - this.totalInputNeuronCount - outputNeuronCount), outputNeuronCount, totalInputNeuronCount);
        else
            cppnShader += colorOutputFunction(colorFunctionName, inputNeuronCount, outputNeuronCount, totalInputNeuronCount);

        //now we add in the start to our main function -- setting our convolution filter 3x3
        cppnShader += THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_MAIN_START;

        //now we need to pipe in the inputs into our network, then translate the outputs
        cppnShader += "vec4 resultColor = " + colorFunctionName + "(" + THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_MAIN_INPUT_COLORS + ");\n";

        //we have our color!
        cppnShader += THREE_X_THREE_TEXTURE_SAMPLING_FRAGMENT_SHADER_MAIN_END;

        return  cppnShader;

    }


}