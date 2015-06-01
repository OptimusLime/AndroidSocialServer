package edu.eplex.androidsocialclient.MainUI.Filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;

import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 3/15/15.
 */
public class FilterArtifact implements Artifact {

    public static class Meta
    {
        public String user;
        public long timeofcreation;
        public String session;
        public String s3Key;
    }
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
    public String isPrivate = "false";

    public String s3Key = null;

    //dates must be added to all objects being saved
    public long date;

    //meta information stored with each object
    public Meta meta;

    public static Meta createEmptyMeta()
    {
        return new Meta();
    }

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

        if(this.parents != null)
            fa.setParents(new ArrayList<String>(this.parents));
        else
            fa.setParents(null);

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

    @Override
    public void stripAllParents() {
        this.setParents(null);
        for(int i=0; i < genomeFilters.size(); i++)
            genomeFilters.get(i).parents = null;
    }

    @Override
    public Map<String, Artifact> setParentsFromArtifactMap(Map<String, Artifact> artifactMap) {

        HashMap<String, Artifact> privateToSave = new HashMap<>();
        HashMap<String, List<String>> ngParentMap = new HashMap<>();

        //privately, we need to construct the chain from the final all the way back to the seeds
        FilterArtifact a = (FilterArtifact)artifactMap.get(this.wid());
        if(a == null)
            return null;

        HashSet<String> selectedPublicParents = new HashSet<>();
        List<String> parents = a.parents();
        List<String> np;

        ArrayList<NeatGenome> genomes = a.genomeFilters;
//        List<String> ngParents = new ArrayList<>();
        for(int i=0; i < genomes.size();i++) {
            NeatGenome ng = genomes.get(i);
            if (ng.parents != null) {
//                ngParents.addAll(ng.parents);
                ngParentMap.put(ng.wid, Lists.newArrayList(ng.parents));
            }
            else
                ngParentMap.put(ng.wid, null);
        }
//        Map<String, ArrayList<String>> innerGenomeParents = new HashMap<>();

        while(parents != null && parents.size() > 0)
        {
            //grab the parents and build the map
            //privately, we must save all parents please
            np = new ArrayList<>();
            for(int i=0; i < parents.size(); i++) {

                FilterArtifact parent = (FilterArtifact)artifactMap.get(parents.get(i));

                genomes = parent.genomeFilters;
                for(int j=0;j < genomes.size(); j++){
                    NeatGenome ng = genomes.get(j);
                    if(ng.parents ==null)
                        ngParentMap.put(ng.wid, null);
                    else
                        ngParentMap.put(ng.wid, Lists.newArrayList(ng.parents));
                }

                if(parent == null)
                {
                    //this must be somehow publicly loaded -- cuz we don't know anything about it locally!
                    selectedPublicParents.add(parents.get(i));
                }
                else {
                    List<String> ppp = parent.parents();
                    if (ppp != null) {
                        np.addAll(ppp);

                        //not a seed -- therefore we must save!
                        privateToSave.put(parent.wid(), parent);
                    } else {
                        //this is a seed -- we track this publicly
                        selectedPublicParents.add(parent.wid());
                    }
                }
            }

            //now we have to investigate the next round please
            parents = np;
        }
        //now we have public parents for the selected object -- lets clone and send back
        Artifact privateClone = a.clone();
        privateToSave.put(privateClone.wid(), privateClone);

        //who are the actual seed parents we have?
        this.setParents(Lists.newArrayList(selectedPublicParents));

        //now who are the inner genomes connected to?
        for(int g=0; g < a.genomeFilters.size(); g++) {

            NeatGenome ng = a.genomeFilters.get(g);

            //grab the parents
            List<String> ngParents = ng.parents;

            HashSet<String> tp = new HashSet<>();
            HashSet<String> ngnp;
            while (ngParents != null && ngParents.size() > 0) {
                ngnp = new HashSet<>();

                for (int i = 0; i < ngParents.size(); i++) {

                    //here is a single parent
                    String parent = ngParents.get(i);

                    List<String> possibleGrandParents = ngParentMap.get(parent);

                    if(possibleGrandParents == null)
                    {
                        //our parent has no parents -- it's the end of the line
                        tp.add(parent);
                    }
                    else
                    {
                        //we have grandparents, we must investigate all
                        ngnp.addAll(possibleGrandParents);
                    }
                }

                ngParents = Lists.newArrayList(ngnp);
            }

            ng.parents = Lists.newArrayList(tp);
        }

        return privateToSave;
    }
}
