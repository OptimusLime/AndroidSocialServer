package eplex.win.FastNEATJava.genome;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Expose `NeatConnection`.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeatConnection
{
    @JsonProperty("gid")
    public String gid;

    @JsonProperty("weight")
    public double weight;

    @JsonProperty("sourceID")
    public String sourceID;

    @JsonProperty("targetID")
    public String targetID;

    public boolean isMutated;

    public NeatConnection(){}

    public NeatConnection(String gid, double weight, String sourceID, String targetID)
    {
        //gid must be a string
        this.gid = gid;
        this.weight = weight;

        //node ids are strings now -- so make sure to save as string always
        this.sourceID = sourceID;
        this.targetID = targetID;
    }

    public NeatConnection clone()
    {
        return Copy(this);
    }

    public static NeatConnection Copy(NeatConnection connection)
    {
        return new NeatConnection(connection.gid, connection.weight, connection.sourceID, connection.targetID);
    }
}