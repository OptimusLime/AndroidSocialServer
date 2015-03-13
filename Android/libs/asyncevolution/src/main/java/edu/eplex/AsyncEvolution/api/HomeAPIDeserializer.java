package edu.eplex.AsyncEvolution.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.eplex.AsyncEvolution.backbone.NEATArtifact;

/**
 * Created by paul on 10/19/14.
 */


public class HomeAPIDeserializer extends JsonDeserializer<HomeAPIObject> {

    @Override
    public HomeAPIObject deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        return null;
//
//        JsonNode node = jp.readValuesAs(HomeAPIObject.class);
//
//
//        JsonNode artifacts = node.get("artifacts");
//
//        List<NEATArtifact> artList = new ArrayList<NEATArtifact>();
//
//        for (int i = 0; i < artifacts.size(); i++)
//        {
//            JsonNode a = artifacts.get(i);
//            NEATArtifact na = new NEATArtifact();
//            na.fromJSON(a);
//        }
//
//        int start = node.get("start").asInt();
//        int end = node.get("end").asInt();
//
//
//        return new HomeAPIObject(artList, start,end);
    }
}