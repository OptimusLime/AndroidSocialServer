package edu.eplex.AsyncEvolution.api;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import edu.eplex.AsyncEvolution.backbone.NEATArtifact;

/**
 * Created by paul on 10/19/14.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeAPIObject {

    @JsonProperty("artifacts")
    public List<NEATArtifact> neatArtifacts;

    public int startIx;
    public int endIx;

    public HomeAPIObject(){}

    public HomeAPIObject(List<NEATArtifact> neatArtifacts, int start, int end)
    {
        this.neatArtifacts = neatArtifacts;
        this.startIx = start;
        this.endIx = end;
    }
}

