package eplex.win.FastNEATJava.help;

import eplex.win.FastNEATJava.genome.NeatConnection;

/**
 * Created by paul on 8/16/14.
 */
public class CorrelationItem {

    public CorrelationType correlationType;
    public NeatConnection connection1;
    public NeatConnection connection2;

    public CorrelationItem(CorrelationType correlationType, NeatConnection conn1, NeatConnection conn2)
    {
        this.correlationType = correlationType;
        this.connection1 = conn1;
        this.connection2 = conn2;
    }
}
