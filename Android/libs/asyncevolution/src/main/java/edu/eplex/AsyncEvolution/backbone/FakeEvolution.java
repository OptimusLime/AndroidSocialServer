package edu.eplex.AsyncEvolution.backbone;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.ArtifactOffspringGenerator;
import eplex.win.winBackbone.AsyncEvolutionLoader;
import eplex.win.winBackbone.AsyncLoadCallback;
import eplex.win.winBackbone.BasicEvolution;
import eplex.win.winBackbone.FinishedCallback;

/**
 * Created by paul on 8/8/14.
 */
public class FakeEvolution implements BasicEvolution {

    @Inject
    ArtifactOffspringGenerator artifactCreator;

    @Inject
    AsyncEvolutionLoader artifactLoader;

    List<Artifact> seedObjects = new ArrayList<Artifact>();
    List<Artifact> parents = new ArrayList<Artifact>();

    Map<String, Artifact> allEvolutionArtifacts = new HashMap<String, Artifact>();
    Map<String, Artifact> selectedParents = new HashMap<String, Artifact>();

    //we need to load the seeds, then do the callback for evolution, k thx
    public void asyncLoadSeeds(final FinishedCallback callback)
    {
        artifactLoader.loadSeeds(new AsyncLoadCallback() {
            @Override
            public void loadedSeeds(List<Artifact> artifacts) {


                //we have our seeds!
                seedObjects.addAll(artifacts);

                for(Artifact a : artifacts)
                {
                    if(!allEvolutionArtifacts.containsKey(a.wid()))
                        allEvolutionArtifacts.put(a.wid(), a);
                }

                //we select a random seed object as our starting parent
                int randomSeedParentIx = (int)Math.floor(Math.random()*artifacts.size());

                //select this object! This is just the behavior for this class
                //it could be different in other circumstances
                selectParents(Arrays.asList(artifacts.get(randomSeedParentIx).wid()));

                //when we're done loading, finish the callback
                //don't need anything other than notice -- keep your silly artifacts
                callback.finishedCallback();
            }
        });
    }

    public void configure(JsonNode jsonConfiguration)
    {
        //we gunna configure dis or what?
    }

    //Be able to adjust who the parents are all the time
    public void selectParents(List<String> parentIDs){

        for(String a : parentIDs)
        {
            //get the artfact for this parent ID --- must exist in this population!
            Artifact p = allEvolutionArtifacts.get(a);

            //if it isn't already selected, we select it
            if(!selectedParents.containsKey(a))
            {
                selectedParents.put(a, p);
                parents.add(p);
            }
        }
    }
    public void unselectParents(List<String> parentIDs){
        //need to remove these parents please!
        for(String a : parentIDs)
        {
            //get the artfact for this parent ID --- must exist in this population!
            Artifact p = allEvolutionArtifacts.get(a);

            //if it is selected, we unselect it
            if(selectedParents.containsKey(a))
            {
                selectedParents.remove(a);
                parents.remove(p);
            }
        }
    }

    //this is a synchronous process anywho -- just call it internally and get some new individuals
    //if you need it to be async, that can be arranged!
    public List<Artifact> createOffspring(int count)
    {
        ArrayList<Artifact> children = new ArrayList<Artifact>(count);

        //use parents to create children -- them is the future yo
        for(int i=0; i < count; i++)
        {
            Artifact child = artifactCreator.createArtifactFromParents(parents);

            children.add(child);
            allEvolutionArtifacts.put(child.wid(), child);
        }

        return children;
    }

}
