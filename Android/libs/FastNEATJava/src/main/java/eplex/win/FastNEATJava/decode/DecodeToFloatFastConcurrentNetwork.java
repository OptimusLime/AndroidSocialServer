package eplex.win.FastNEATJava.decode;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.activation.functions.BipolarSigmoid;
import eplex.win.FastCPPNJava.activation.functions.Linear;
import eplex.win.FastCPPNJava.activation.functions.PlainSigmoid;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastCPPNJava.network.CppnConnection;
import eplex.win.FastCPPNJava.network.NodeType;
import eplex.win.FastNEATJava.genome.NeatConnection;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.FastNEATJava.utils.NeatParameters;

public class DecodeToFloatFastConcurrentNetwork
{
    public static CPPN DecodeNeatGenomeToCPPN(NeatGenome ng)
    {
         int outputNeuronCount = ng.outputNodeCount;
         int neuronGeneCount = ng.nodes.size();

         double[] biasList = new double[neuronGeneCount];

         // Slightly inefficient - determine the number of bias nodes. Fortunately there is not actually
         // any reason to ever have more than one bias node - although there may be 0.
         List<String> activationFunctionArray = new ArrayList<String>();

         int nodeIdx=0;
         for(; nodeIdx<neuronGeneCount; nodeIdx++)
         {
             if(ng.nodes.get(nodeIdx).nodeType !=  NodeType.bias)
                 break;

             activationFunctionArray.add(ng.nodes.get(nodeIdx).activationFunction);

         }
         int biasNodeCount = nodeIdx;
         int inputNeuronCount = ng.inputNodeCount;
         for (; nodeIdx < neuronGeneCount; nodeIdx++)
         {
         activationFunctionArray.add(ng.nodes.get(nodeIdx).activationFunction);
         biasList[nodeIdx] = ng.nodes.get(nodeIdx).bias;
         }

         // ConnectionGenes point to a neuron ID. We need to map this ID to a 0 based index for
         // efficiency.

         // Use a quick heuristic to determine which will be the fastest technique for mapping the connection end points
         // to neuron indexes. This is heuristic is not 100% perfect but has been found to be very good in in real word
         // tests. Feel free to perform your own calculation and create a more intelligent heuristic!
         int  connectionCount= ng.connections.size();

            List<CppnConnection> fastConnectionArray = new ArrayList<CppnConnection>();

        Map<String, Integer> neuronIndexTable = new HashMap<String, Integer>();

         for(int i=0; i<neuronGeneCount; i++)
             neuronIndexTable.put(ng.nodes.get(i).gid, i);

         for(NeatConnection connection : ng.connections)
         {
             fastConnectionArray.add(new CppnConnection(
                     neuronIndexTable.get(connection.sourceID),
                     neuronIndexTable.get(connection.targetID),
                     connection.weight));
         }


        //now we're ready to return our CPPN object
         return new CPPN(biasNodeCount,
                 inputNeuronCount,
                 outputNeuronCount,
                 neuronGeneCount,
                 fastConnectionArray,
                 biasList,
                 activationFunctionArray);
    }

    static int B_InputToHidden = 0;
    static int H_InputToHidden = 1;
    static int S_InputToHidden = 2;
    static int V_InputToHidden = 3;
    static int H_HiddenToOutput = 4;
    static int S_HiddenToOutput = 5;
    static int V_HiddenToOutput = 6;

    static int H_IX_OUT = 0;
    static int S_IX_OUT = 1;
    static int V_IX_OUT = 2;

    static double weightThreshold = .2;

    public static CPPN DecodeNeatGenomeToHyperNEATCPPN(NeatGenome ng)
    {
        //first we do a decode
        CPPN cppn = DecodeNeatGenomeToCPPN(ng);

        //now we need to take this CPPN to produce a new CPPN that will handle our filtering.

        //lets query accross the layers
        int allX = 3;
        int allY = 3;

        int hiddenAllX =2;
        int hiddenAllY =2;

        double currentX, currentY;

        double currentHiddenX, currentHiddenY;

        //query a 3 x 3 grid
        double[] cppnInputs;

        double hWeight, sWeight, vWeight;

        double h_outputWeight, s_outputWeight, v_outputWeight;
        double biasWeight;

        // Slightly inefficient - determine the number of bias nodes. Fortunately there is not actually
        // any reason to ever have more than one bias node - although there may be 0.
        List<String> activationFunctionArray = new ArrayList<String>();

        //single bias, wired to all the hidden nodes
        int biasNodeCount = 1;
        int inputNodeCount = 3*allX*allY; //h,s,v as inputs for the network
        int hiddenNeuronCount = hiddenAllX*hiddenAllY;
        int outputNeuronCount = 3;

        //how many total nodes inside the substrate?
        int neuronGeneCount = biasNodeCount + inputNodeCount +  hiddenNeuronCount + outputNeuronCount;//1 bias, 3x3 inputs +  3 x 3 hidden layer + 3 outputs = 22 nodes

        Map<Integer, Double> nodeMapPositiveSum = new HashMap<>();
        Map<Integer, Double> nodeMapNegativeSum = new HashMap<>();


        //set as linear for inputs and bipolar sigmoid for outputs
        for(int nodeIdx=0; nodeIdx<neuronGeneCount; nodeIdx++)
        {
            if(nodeIdx < biasNodeCount + inputNodeCount)
                activationFunctionArray.add(Linear.class.getSimpleName());
            else {
                activationFunctionArray.add(PlainSigmoid.class.getSimpleName());
                nodeMapPositiveSum.put(nodeIdx, 0.0);
                nodeMapNegativeSum.put(nodeIdx, 0.0);
            }


        }

        List<CppnConnection> fastConnectionArray = new ArrayList<CppnConnection>();


        double[] biasList = new double[neuronGeneCount];

        int hiddenIx, inputIx;

        int inputStartIx =  biasNodeCount;
        int outputStartIx = biasNodeCount + inputNodeCount;
        int hiddenStartIx = outputStartIx + outputNeuronCount;

        //loop through all the hidden nodes -- then for each hidden node, ask the connections from input layer
        //and also ask for connections to the 3 output nodes
        for(int sx =0; sx < hiddenAllX; sx++)
        {
            for(int sy = 0; sy < hiddenAllY; sy++)
            {

                //grab the id number
                hiddenIx = sx + sy*hiddenAllX;

                //now we measure from one to the other
                currentHiddenX = ((sx << 1) - hiddenAllX + 1.0) / hiddenAllX;
                currentHiddenY = ((sy << 1) - hiddenAllY + 1.0) / hiddenAllY;

                currentX = 0;
                currentY = 0;

                //first we query to find out what the second layer hidden-output weights should be
                cppnInputs = new double[]{
                        currentX, //x1
                        currentY, //y1
                        currentHiddenX, //x2
                        currentHiddenY //y2
                };

                cppn.recursiveActivation(cppnInputs);

                //get the final output
                h_outputWeight = cppn.getOutputSignal(H_HiddenToOutput);
                s_outputWeight = cppn.getOutputSignal(S_HiddenToOutput);
                v_outputWeight = cppn.getOutputSignal(V_HiddenToOutput);

                fastConnectionArray.add(new CppnConnection(
                        hiddenStartIx + hiddenIx,
                        outputStartIx + H_IX_OUT,
                        h_outputWeight
                ));

                //need to add this weight for normalization later
                if(h_outputWeight >= 0)
                    nodeMapPositiveSum.put(outputStartIx+ H_IX_OUT, nodeMapPositiveSum.get(outputStartIx+ H_IX_OUT) + h_outputWeight);
                else
                    nodeMapNegativeSum.put(outputStartIx+ H_IX_OUT, nodeMapNegativeSum.get(outputStartIx+ H_IX_OUT) + h_outputWeight);

                fastConnectionArray.add(new CppnConnection(
                        hiddenStartIx + hiddenIx,
                        outputStartIx + S_IX_OUT,
                        s_outputWeight
                ));

                //need to add this weight for normalization later
                if(s_outputWeight >= 0)
                    nodeMapPositiveSum.put(outputStartIx+ S_IX_OUT, nodeMapPositiveSum.get(outputStartIx+ S_IX_OUT) + s_outputWeight);
                else
                    nodeMapNegativeSum.put(outputStartIx+ S_IX_OUT, nodeMapNegativeSum.get(outputStartIx+ S_IX_OUT) + s_outputWeight);

                fastConnectionArray.add(new CppnConnection(
                        hiddenStartIx + hiddenIx,
                        outputStartIx + V_IX_OUT,
                        v_outputWeight
                ));

                //need to add this weight for normalization later
                if(v_outputWeight >= 0)
                    nodeMapPositiveSum.put(outputStartIx+ V_IX_OUT, nodeMapPositiveSum.get(outputStartIx+ V_IX_OUT) + v_outputWeight);
                else
                    nodeMapNegativeSum.put(outputStartIx+ V_IX_OUT, nodeMapNegativeSum.get(outputStartIx+ V_IX_OUT) + v_outputWeight);

                //also read the bias weight being wired to this particular node
                biasWeight = cppn.getOutputSignal(B_InputToHidden);
//                biasList[hiddenIx] = biasWeight;

                //add a connection from the bias input node to the current hidden node
                fastConnectionArray.add(new CppnConnection(
                        0,
                        hiddenStartIx + hiddenIx,
                        biasWeight
                ));

                double hiddenNodePositive = nodeMapPositiveSum.get(hiddenStartIx+ hiddenIx);
                double hiddenNodeNegative = nodeMapNegativeSum.get(hiddenStartIx+hiddenIx);

                if(biasWeight >= 0)
                    hiddenNodePositive += biasWeight;
                else
                    hiddenNodeNegative += biasWeight;

                //then create the weight accordingly

                for (int y = 0; y < allY; y++) {

                    for (int x = 0; x < allX; x++) {

                        //what input are we?
                        //hsv for each input so 3 x the index
                        inputIx = 3*(x + y*allX);

                        currentX = ((x << 1) - allX + 1.0) / allX;
                        currentY = ((y << 1) - allY + 1.0) / allY;

                        //now we input the x1, y1 - x2, y2 of the substrate connections
                        cppnInputs = new double[]{
                                currentX, //x1
                                currentY, //y1
                                currentHiddenX, //x2
                                currentHiddenY //y2
                        };

                        //query the CPPN
                        cppn.recursiveActivation(cppnInputs);

                        //grab the outputs
                        hWeight = cppn.getOutputSignal(H_InputToHidden);
                        sWeight = cppn.getOutputSignal(S_InputToHidden);
                        vWeight = cppn.getOutputSignal(V_InputToHidden);

                        //then create a connection for each input to hidden node accordingly!
                        fastConnectionArray.add(new CppnConnection(
                                inputStartIx + inputIx+ H_IX_OUT,
                                hiddenStartIx + hiddenIx,
                                hWeight
                        ));

                        if(hWeight >= 0)
                            hiddenNodePositive += hWeight;
                        else
                            hiddenNodeNegative += hWeight;


                        fastConnectionArray.add(new CppnConnection(
                                inputStartIx + inputIx+ S_IX_OUT,
                                hiddenStartIx + hiddenIx,
                                sWeight
                        ));

                        if(sWeight >= 0)
                            hiddenNodePositive += sWeight;
                        else
                            hiddenNodeNegative += sWeight;


                        fastConnectionArray.add(new CppnConnection(
                                inputStartIx + inputIx + V_IX_OUT,
                                hiddenStartIx + hiddenIx,
                                vWeight
                        ));

                        if(vWeight >= 0)
                            hiddenNodePositive += vWeight;
                        else
                            hiddenNodeNegative += vWeight;

                    }
                }

                //done with this particular hidden node
                nodeMapPositiveSum.put(hiddenStartIx+hiddenIx, hiddenNodePositive);
                nodeMapNegativeSum.put(hiddenStartIx+hiddenIx, hiddenNodeNegative);
            }
        }

        boolean normalization = false;

        if(normalization) {
            //we'll need to normalize values into each input node, and normalize values for each output node
            for (int i = 0; i < fastConnectionArray.size(); i++) {
                //loop through outputs and
                CppnConnection conn = fastConnectionArray.get(i);

                //if we're an output node or a hidden node target
                if (conn.targetIdx >= outputStartIx) {
                    double mapValue;
                    if (conn.weight >= 0) {
                        //what was the sum of the positives
                        mapValue = nodeMapPositiveSum.get(conn.targetIdx);
                    } else {
                        //get the max value (absolute for negatives cause we're dividing)
                        mapValue = Math.abs(nodeMapNegativeSum.get(conn.targetIdx));
                    }

                    if (mapValue > 0)
                        conn.weight /= Math.abs(mapValue);
                }
            }
        }

        //now take that network substrate created from the original, and send it back for translation into a GPU network


        //now we're ready to return our CPPN object
        return new CPPN(biasNodeCount,
                inputNodeCount,
                outputNeuronCount,
                neuronGeneCount,
                fastConnectionArray,
                biasList,
                activationFunctionArray);

    }

}