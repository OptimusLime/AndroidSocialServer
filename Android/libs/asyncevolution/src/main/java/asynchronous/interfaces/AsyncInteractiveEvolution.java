package asynchronous.interfaces;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import javax.inject.Inject;

import bolts.Task;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.ArtifactOffspringGenerator;


/**
 * Created by paul on 8/14/14.
 */
public abstract class AsyncInteractiveEvolution {
    //seed loader handles initialization of the Interactive Evolution session
    //you can have seeds loaded from file, or preferably from a server
    //you can't really trust client uploads, so it's best to fetch from the server
    @Inject
    public AsyncSeedLoader seedLoader;

    //offspring generator handles taking in a collection of artifact parents,
    //handling any unusual artifact generation logic, then returning the children
    @Inject
    public ArtifactOffspringGenerator offspringGenerator;

    public abstract void clearSession();
    public abstract List<Artifact> seeds();

    //Be able to adjust who the parents are all the time
    public abstract void selectParents(List<String> parentIDs);
    public abstract void unselectParents(List<String> parentIDs);

    //Most important thing are the children. They're the future
    //this is an immediate callback function
    public abstract List<Artifact> createOffspring(int count);

    //same as create offspring, just promising the return of some artifacts
    public abstract Task<List<Artifact>> asyncCreateOffspring(int count);


    //configure with some parameters of course!
    public abstract Task<Void> asyncInitialize(JsonNode configuration);

}
