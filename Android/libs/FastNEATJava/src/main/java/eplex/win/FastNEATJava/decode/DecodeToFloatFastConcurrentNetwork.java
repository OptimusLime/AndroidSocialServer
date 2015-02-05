package eplex.win.FastNEATJava.decode;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastCPPNJava.network.CppnConnection;
import eplex.win.FastCPPNJava.network.NodeType;
import eplex.win.FastNEATJava.genome.NeatConnection;
import eplex.win.FastNEATJava.genome.NeatGenome;

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
             if(ng.nodes.get(nodeIdx).type !=  NodeType.bias)
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



}