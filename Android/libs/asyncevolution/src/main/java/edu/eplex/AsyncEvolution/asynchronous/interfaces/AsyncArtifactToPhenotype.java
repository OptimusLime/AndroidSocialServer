package edu.eplex.AsyncEvolution.asynchronous.interfaces;

import com.fasterxml.jackson.databind.JsonNode;

import bolts.Task;

/**
 * Created by paul on 8/14/14.
 */
public interface AsyncArtifactToPhenotype<ArtClass, PhenoClass> {
    Task<PhenoClass> asyncPhenotypeToUI(ArtClass artifact, JsonNode params);
}
