package edu.eplex.AsyncEvolution.asynchronous.implementation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncInteractiveEvolution;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncSeedLoader;
import bolts.Continuation;
import bolts.Task;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.ArtifactOffspringGenerator;
import eplex.win.winBackbone.AsyncEvolutionLoader;
import eplex.win.winBackbone.AsyncLoadCallback;
import eplex.win.winBackbone.FinishedCallback;

/**
 * Created by paul on 8/14/14.
 */
public class AsyncLocalIEC extends AsyncInteractiveEvolution {

    List<Artifact> seedObjects = new ArrayList<Artifact>();
    List<Artifact> parents = new ArrayList<Artifact>();

    Map<String, Artifact> allEvolutionArtifacts = new HashMap<String, Artifact>();
    Map<String, Artifact> selectedParents = new HashMap<String, Artifact>();

    @Override
    public List<Artifact> seeds() {
        return seedObjects;
    }

    //Be able to adjust who the parents are all the time
    @Override
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

    @Override
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

    @Override
    public void clearParents() {
        selectedParents.clear();
        parents.clear();
    }

    List<Artifact> lastChildren;

    @Override
    public List<Artifact> lastOffspring() {
        return lastChildren;
    }

    //this is a synchronous process anywho -- just call it internally and get some new individuals
    //if you need it to be async, that can be arranged!
    @Override
    public List<Artifact> createOffspring(int count)
    {
        ArrayList<Artifact> children = new ArrayList<Artifact>(count);

        lastChildren = new ArrayList<>();
        //use parents to create children -- them is the future yo
        for(int i=0; i < count; i++)
        {
            Artifact child = offspringGenerator.createArtifactFromParents(parents);

            lastChildren.add(child);
            children.add(child);
            allEvolutionArtifacts.put(child.wid(), child);
        }

        return children;
    }


    @Override
    public Task<List<Artifact>> asyncCreateOffspring(final int count) {

        //same as the synchronous, except it's performed asynchronously
        return Task.callInBackground(new Callable<List<Artifact>>()
        {
             @Override
             public List<Artifact> call() throws Exception {
                 return createOffspring(count);
             }
         });
    }

    //need to initialize evolution, and when we're all done
    //we'll have some seeds and a single random parent selected
    @Override
    public Task<Void> asyncInitialize(JsonNode configuration)
    {
        //we gunna configure dis or what?

        //we first load up all our seeds
        //then after the load process completes, we need to return
        return seedLoader.asyncLoadSeeds(null).continueWith(
                // This Continuation is a function which takes an Integer as input,
                // and provides a String as output. It must take an Integer because
                // that's what was returned from the previous Task.
                new Continuation<List<Artifact>, Void>() {
                    // The Task getIntAsync() returned is passed to "then" for convenience.
                    public Void then(Task<List<Artifact>> task) throws Exception {

                        if (task.isCancelled()) {
                            // the load seed was cancelled.
                            throw new RuntimeException("Seed loading task was cancelled for some reason.");
                        } else if (task.isFaulted()) {
                            // the save failed.
                            Exception error = task.getError();
                            throw error;
                        } else {

                            //aha! We have our parents!
                            List<Artifact> parents = task.getResult();

                            //we have our parents, let's add the seeds and then select a random seed
                            addSeeds(parents);

                            //select a parent among the seeds
                            selectRandomSeedParents();

                            //all done!
                            return null;
                        }
                    }
                }
        );

    }
    //Some helper funcitons down here -- nothing serious, just simple stuff

    //we need to load the seeds, k thx
    void addSeeds(List<Artifact> artifacts)
    {
        //we have our seeds!
        seedObjects.addAll(artifacts);

        for(Artifact a : artifacts)
        {
            if(!allEvolutionArtifacts.containsKey(a.wid()))
                allEvolutionArtifacts.put(a.wid(), a);
        }
    }
    //select someone to be the lucky parent, in this version of IEC
    void selectRandomSeedParents()
    {
        //we select a random seed object as our starting parent
        int randomSeedParentIx = (int)Math.floor(Math.random()*seedObjects.size());

        //select this object! This is just the behavior for this class
        //it could be different in other circumstances
        selectParents(Arrays.asList(seedObjects.get(randomSeedParentIx).wid()));
    }

    @Override
    public void clearSession() {

        //clear out session info -- including selected parents

        //remove all of our parents -- one at a time
        while(parents.size() > 0) {

            this.unselectParents(Arrays.asList(parents.get(0).wid()));
            parents.remove(0);
        }

        //then clear out all our session info!
       offspringGenerator.clearSession();
    }
}
