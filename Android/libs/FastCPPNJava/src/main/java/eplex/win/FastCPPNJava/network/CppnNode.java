package eplex.win.FastCPPNJava.network;

/**
 * Initialize a new cppnNode.
 *
 * @param {String} actFn
 * @param {String} neurType
 * @param {String} nid
 * @api public
 */

public class CppnNode {

    public NodeType neuronType;
    public String id;
    public double outputValue;
    public String activationFunction;

    public CppnNode(String actFn, NodeType neurType, String nid) {

        this.neuronType = neurType;
        this.id = nid;
        this.outputValue = (this.neuronType == NodeType.bias ? 1.0 : 0.0);
        this.activationFunction = actFn;
    }
}