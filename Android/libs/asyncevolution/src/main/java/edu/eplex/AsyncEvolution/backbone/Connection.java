package edu.eplex.AsyncEvolution.backbone;

public class Connection
{
    public String gid;
    public String sourceID;
    public String targetID;
    public double weight;

    public Connection clone()
    {
        Connection conn = new Connection();
        conn.gid = this.gid;
        conn.sourceID = this.sourceID;
        conn.targetID = this.targetID;
        conn.weight = this.weight;
        return conn;
    }

}
