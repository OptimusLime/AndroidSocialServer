package win.eplex.backbone;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.Genome;

/**
 * Created by paul on 8/13/14.
 */
public class FakeArtifact implements Artifact {

    private String wid;

    @Inject
    public Genome genome;

    @Override
    public String wid() {
        return this.wid;
    }

    public void setWID(String value)
    {
        this.wid = value;
    }

    List<String> parents;

    @Override
    public List<String> parents() {
        return parents;
    }

    public void setParents(List<String> parents)
    {
        this.parents = parents;
    }

    @Override
    public Artifact clone() {
        FakeArtifact fa = new FakeArtifact();

        fa.setParents(new ArrayList<String>(((this.parents))));

        //clone our own genome
        fa.genome = this.genome.clone();
        fa.wid = this.wid;

        return fa;
    }

    @Override
    public Artifact fromJSON(String jsonArtifact) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(jsonArtifact, FakeArtifact.class);
        }
        catch (JsonParseException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (JsonMappingException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Artifact fromJSON(JsonNode jsonArtifact) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.convertValue(jsonArtifact, FakeArtifact.class);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    @Override
    public JsonNode toJSON() {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;

        try {
            node = mapper.convertValue(this, JsonNode.class);


        } catch (IllegalArgumentException e) {

            e.printStackTrace();
            node = null;
        }

        return node;
    }
    @Override
    public String toJSONString() {

        JsonNode tree = this.toJSON();
        ObjectMapper mapper = new ObjectMapper();
        try {
            final Object obj = mapper.treeToValue(tree, Object.class);
            final String json = mapper.writeValueAsString(obj);
            return json;
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
            return null;
        }
    }

}
