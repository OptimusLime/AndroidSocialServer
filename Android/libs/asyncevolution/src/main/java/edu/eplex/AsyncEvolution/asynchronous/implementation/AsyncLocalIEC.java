package edu.eplex.AsyncEvolution.asynchronous.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

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

    Map<String, ArrayList<String>> privateArtifactParents = new HashMap<>();

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

            //map the child to the parents
            privateArtifactParents.put(child.wid(), Lists.newArrayList(child.parents()));

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
    public Task<Void> asyncInitialize(JsonNode configuration, final List<Artifact> parents)
    {
        //we gunna configure dis or what?

        //we first load up all our seeds
        //then after the load process completes, we need to return
         //seedLoader.asyncLoadSeeds(null).continueWith(
        return Task.call(new Callable<Void>() {
                   @Override
                   public Void call() throws Exception {


                       //we have our parents, let's add the seeds and then select a random seed
                       addSeeds(parents);

                       //select a parent among the seeds
                       selectRandomSeedParents();

                       //all done!
                       return null;
                   }
               }, Task.UI_THREAD_EXECUTOR);
                // This Continuation is a function which takes an Integer as input,
                // and provides a String as output. It must take an Integer because
                // that's what was returned from the previous Task.
//                new Continuation<List<Artifact>, Void>() {
//                    // The Task getIntAsync() returned is passed to "then" for convenience.
//                    public Void then(Task<List<Artifact>> task) throws Exception {
//
//                        if (task.isCancelled()) {
//                            // the load seed was cancelled.
//                            throw new RuntimeException("Seed loading task was cancelled for some reason.");
//                        } else if (task.isFaulted()) {
//                            // the save failed.
//                            Exception error = task.getError();
//                            throw error;
//                        } else {
//
//                            //aha! We have our parents!
//                            List<Artifact> parents = task.getResult();
//
//                            //we have our parents, let's add the seeds and then select a random seed
//                            addSeeds(parents);
//
//                            //select a parent among the seeds
//                            selectRandomSeedParents();
//
//                            //all done!
//                            return null;
//                        }
//                    }
//                }
//        );

    }
    //Some helper funcitons down here -- nothing serious, just simple stuff

    //we need to load the seeds, k thx
    void addSeeds(List<Artifact> artifacts)
    {

        ArrayList<Artifact> clonedArtifacts = new ArrayList<>();
        for(int i=0; i < artifacts.size(); i++) {
            Artifact clone = artifacts.get(0).clone();
            clone.stripAllParents(); //remove all seed parents -- they should be parentless as far as we're concerned!
            clonedArtifacts.add(clone);
        }

        //we have our seeds!
        seedObjects.addAll(clonedArtifacts);

        for(Artifact a : clonedArtifacts)
        {
            if(!allEvolutionArtifacts.containsKey(a.wid()))
                allEvolutionArtifacts.put(a.wid(), a);

            //make sure that the private mappings == null -- they have no parents, do you understand?????
            privateArtifactParents.put(a.wid(), null);
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

    public Map<SessionPublishType, Map<String, Artifact>> prepareArtifactSessionToPublish(String selectedWID)
    {
        Artifact a = allEvolutionArtifacts.get(selectedWID);

        Map<String, Artifact> privateToSave = a.setParentsFromArtifactMap(allEvolutionArtifacts);

        HashMap<SessionPublishType, Map<String, Artifact>> allToPublish = new HashMap<>();

        Map<String, Artifact> publicToSave = new HashMap<>();
        publicToSave.put(a.wid(), a);

        //aggregate the info into a hashmap of public and private public
        allToPublish.put(SessionPublishType.publicPublish, publicToSave);
        allToPublish.put(SessionPublishType.privatePublish, privateToSave);
        return allToPublish;
    }

}
