package eplex.win.winBackbone;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by paul on 8/13/14.
 */
public interface ArtifactToPhenotypeMapping {
    Object createArtifactPhenotype(Artifact offspring, JsonNode params);
    void asyncCreateArtifactPhenotype(Artifact offspring, JsonNode params, AsyncArtifactToPhenotype callback);
}
