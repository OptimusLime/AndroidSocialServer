package win.eplex.backbone;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import eplex.win.winBackbone.Genome;

public class FakeGenome implements Genome {

    public ArrayList<Node> nodes;
    public ArrayList<Connection> connections;

    public void setNodes(List<Node> nodeObjects)
    {
        nodes = new ArrayList<Node>();
        nodes.addAll(nodeObjects);
    }

    public void setConnections(List<Connection> connObjects)
    {
        connections = new ArrayList<Connection>();
        connections.addAll(connObjects);
    }

    ArrayList<Node> cloneNodes()
    {
        ArrayList<Node> cloned = new ArrayList<Node>();
        for(Node n : this.nodes)
        {
            cloned.add(n.clone());
        }
        return cloned;
    }
    ArrayList<Connection> cloneConnections()
    {
        ArrayList<Connection> cloned = new ArrayList<Connection>();
        for(Connection c : this.connections)
        {
            cloned.add(c.clone());
        }
        return cloned;
    }

    @Override
    public Genome clone() {
        FakeGenome fg = new FakeGenome();

        //clone our nodes, then our connections
        fg.setNodes(this.cloneNodes());
        fg.setConnections(this.cloneConnections());

        //finish cloning ourselves!
        return fg;
    }
}
