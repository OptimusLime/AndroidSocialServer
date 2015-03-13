package eplex.win.winBackbone;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Created by paul on 8/8/14.
 */
public interface Artifact {

    Artifact fromJSON(String jsonArtifact);
    Artifact fromJSON(JsonNode jsonArtifact);
    JsonNode toJSON();
    String toJSONString();

    //get/set wid please!
    String wid();
    void setWID(String wid);

    List<String> parents();
    void setParents(List<String> parents);

    Artifact clone();
}
