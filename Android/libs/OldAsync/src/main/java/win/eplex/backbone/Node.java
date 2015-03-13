package win.eplex.backbone;

/**
 * Created by paul on 8/13/14.
 */

public class Node
{
    public String gid;

    public Node clone()
    {
        Node node = new Node();
        node.gid = this.gid;
        return node;
    }
}
