package eplex.win.FastCPPNJava.network;
/**
 * Initialize a new cppnConnection.
 *
 * @param {Number} sourceIdx
 * @param {Number} targetIdx
 * @param {Number} cWeight
 * @api public
 */
//simple connection type -- from FloatFastConnection.cs
public class CppnConnection {
    public int sourceIdx;
    public int targetIdx;
    public double weight;
    public double signal;

    public CppnConnection(int sourceIdx, int targetIdx, double cWeight) {
        this.sourceIdx = sourceIdx;
        this.targetIdx = targetIdx;
        this.weight = cWeight;
        this.signal = 0;
    }
}
