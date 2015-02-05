package asynchronous.interfaces;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import bolts.Task;
import dagger.ObjectGraph;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 8/14/14.
 */
public interface AsyncSeedLoader {

    //go and grab some seeds and send them back, please!
    //we can customize this process with some parameters
    //in WIN this is a call to the branch loading function
    Task<List<Artifact>> asyncLoadSeeds(JsonNode params);
}
