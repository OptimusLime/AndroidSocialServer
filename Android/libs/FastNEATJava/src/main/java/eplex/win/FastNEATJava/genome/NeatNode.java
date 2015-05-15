package eplex.win.FastNEATJava.genome;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import eplex.win.FastCPPNJava.network.NodeType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeatNode {

    static double INPUT_LAYER = 0.0;
    static double OUTPUT_LAYER = 10.0;

    @JsonProperty("gid")
    public String gid;

    @JsonProperty("activationFunction")
    public String activationFunction;

    @JsonProperty("nodeType")
    public NodeType nodeType;

    @JsonProperty("layer")
    public double layer;

    @JsonProperty("step")
    public int step = 0;

    @JsonProperty("bias")
    public double bias = 0.0;

    public NeatNode(){}

    public NeatNode(String gid, String functionID, double layer, NodeType type) {
        this.gid = gid;
        this.activationFunction = functionID;
        this.layer = layer;
        this.nodeType = type;
    }

    public NeatNode clone()
    {
        return Copy(this);
    }

    public static NeatNode Copy(NeatNode otherNode)
    {
        return new NeatNode(otherNode.gid, otherNode.activationFunction, otherNode.layer, otherNode.nodeType);
    }
}