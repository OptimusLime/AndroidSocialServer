package edu.eplex.androidsocialclient.MainUI.Filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;

import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 3/15/15.
 */
public class FilterArtifact implements Artifact {

    public FilterArtifact(NeatGenome singleFilterGenome)
    {
        genomeFilters = new ArrayList<>();
        genomeFilters.add(singleFilterGenome);
    }

    public FilterArtifact(List<NeatGenome> filterGenomes)
    {
        genomeFilters = new ArrayList<>();
        genomeFilters.addAll(filterGenomes);
    }

    public FilterArtifact()
    {
        genomeFilters = new ArrayList<>();
    }

    @JsonProperty("wid")
    private String wid;

    public String dbType = "FilterArtifact";

    @JsonProperty("genomeFilters")
    public ArrayList<NeatGenome> genomeFilters;

    @JsonProperty("caption")
    public String photoCaption;

    @JsonProperty("hashtags")
    public ArrayList<String> hashtags;

    @JsonProperty("isPrivate")
    public Boolean isPrivate;

    @Override
    public String wid() {
        return this.wid;
    }

    public void setWID(String value)
    {
        this.wid = value;
    }

    @JsonProperty("parents")
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
        FilterArtifact fa = new FilterArtifact();

        fa.setParents(new ArrayList<String>(this.parents));

        //clone our own genomes
        for(int i=0; i < this.genomeFilters.size(); i++)
        {
            fa.genomeFilters.add(this.genomeFilters.get(i).cloneGenome());
        }

        //set our wid to be the old ID :)
        fa.wid = this.wid;

        //return our new found clone hehe
        return fa;
    }

    @Override
    public Artifact fromJSON(String jsonArtifact) {

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(jsonArtifact, FilterArtifact.class);
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
            return mapper.convertValue(jsonArtifact, FilterArtifact.class);
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
