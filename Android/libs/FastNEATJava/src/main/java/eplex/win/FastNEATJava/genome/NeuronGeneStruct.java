package eplex.win.FastNEATJava.genome;

/**
 * Created by paul on 8/17/14.
 */
public class NeuronGeneStruct {

    public NeatNode node;
    public NeatConnection connection1;
    public NeatConnection connection2;

    public static NeuronGeneStruct Create(NeatNode node, NeatConnection conn1, NeatConnection conn2)
    {
        NeuronGeneStruct ng = new NeuronGeneStruct();
        ng.node = node;
        ng.connection1 =conn1;
        ng.connection2 =conn2;
        return ng;
    }

}
