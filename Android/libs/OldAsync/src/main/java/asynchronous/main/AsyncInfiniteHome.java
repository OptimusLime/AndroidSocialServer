package asynchronous.main;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhaarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.AlphaInAnimationAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import asynchronous.interfaces.AsyncArtifactToUI;
import asynchronous.interfaces.AsyncInteractiveEvolution;
import asynchronous.interfaces.AsyncSeedLoader;
import bolts.Continuation;
import bolts.Task;
import cardUI.EndlessGridScrollListener;
import cardUI.StickyCardGridArrayAdapter;
import cardUI.StickyCardGridView;
import cardUI.cards.GridCard;
import cardUI.cards.HomeGridCard;
import dagger.ObjectGraph;
import eplex.win.winBackbone.Artifact;
import interfaces.PhenotypeCache;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.view.CardGridView;
import main.fragments.HomeFragment;
import main.fragments.IECFragment;
import win.eplex.backbone.NEATArtifact;
import win.eplex.backbone.R;

/**
 * Created by paul on 10/19/14.
 */
public class AsyncInfiniteHome implements HomeGridCard.HomeGridCardButtonHandler {

    JsonNode iecParams;

    CardGridArrayAdapter mCardArrayAdapter;
    EndlessGridScrollListener mScrollListener;

    @Inject
    AsyncArtifactToUI<Artifact, double[][], HomeGridCard> asyncArtifactToUIMapper;

    @Inject
    PhenotypeCache<Bitmap> parentBitmapCache;

    @Inject
    PhenotypeCache<HomeGridCard> gridCardCache;

    @Inject
    AsyncSeedLoader homeUIArtifactLoader;

    Map<String, Artifact> allArtifactMap = new HashMap<String, Artifact>();

    Activity activity;

    Activity getActivity() {
        return activity;
    }

    public AsyncInfiniteHome()
    {

    }
    public void injectGraph(Activity act, ObjectGraph graph)
    {
        this.activity = act;

        //need to inject these individuals
        //that will create appropriate phenotype caches and async artifact objects
        graph.inject(this);

        //inject our artifact loader (so it can pull in the proper API services
        graph.inject(homeUIArtifactLoader);

        //inject the inner workings of our async artifact-to-UI object
        graph.inject(asyncArtifactToUIMapper);
    }

    public void handleEvolve(String wid)
    {
        Toast.makeText(getActivity(), "Evolve please!", Toast.LENGTH_SHORT).show();

        //lets try launching a new infinite IEC fragment with this info

        IECFragment frag = new IECFragment();

        List<NEATArtifact> artifacts = Arrays.asList((NEATArtifact)allArtifactMap.get(wid));
        frag.setFragmentSeeds(artifacts);

        getActivity().getFragmentManager().beginTransaction()
                .add(R.id.container, frag)
                .commit();

    }
    public void handleHistory(String wid)
    {
        Toast.makeText(getActivity(), "History please!", Toast.LENGTH_SHORT).show();
    }

    public Task<Void> asyncInitializeHomeandUI(JsonNode params) {

        iecParams = params;
        if(iecParams == null) {

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jNode = mapper.createObjectNode();

            ObjectNode uiParams = mapper.createObjectNode();

            uiParams.set("width", mapper.convertValue(20, JsonNode.class));
            uiParams.set("height", mapper.convertValue(20 , JsonNode.class));

            jNode.set("ui", uiParams);

            ObjectNode uiParParams = mapper.createObjectNode();

            uiParams.set("width", mapper.convertValue(20, JsonNode.class));
            uiParams.set("height", mapper.convertValue(20 , JsonNode.class));

            jNode.set("parents", uiParParams);

            iecParams = jNode;
        }

        //first we initialize all our internal organs, so to speak
        initializeUI();

        //then we send off our homeUI loader process to initialize itself!
        return loadNextBatchOfItems(params);
    }

    Task<Void> loadNextBatchOfItems(JsonNode params)
    {
        return homeUIArtifactLoader.asyncLoadSeeds(params)
                .continueWithTask(new Continuation<List<Artifact>, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<List<Artifact>> task) throws Exception {

                        List<Artifact> allSeeds = task.getResult();
                        for (Artifact seed : allSeeds)
                            allArtifactMap.put(seed.wid(), seed);

                        //now we need to display all these artifacts
                        return asyncGetMoreCards(allSeeds);
                    }
                });
    }

    //we've been summoned to fetch new card objects
    //this is the crucial function for async IEC
    //this means creating artifacts, eating their children, then turning them into cards for the UI
    //wait... what was that second thing?
    //Creating new artifacts?
    //no... that wasn't it...
    //OH! Turning the artifacts into Card UI objects
    //uhhhh.....
    Task<Void> asyncGetMoreCards(List<Artifact> offspring)
    {
        //we get some cards, async style!

        //we're going to do a bunch of async conversions between artifacts and UI objects
        //that's the process of converting a genome into a phenotype into a UI object
        ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();

//        final GridCard.GridCardButtonHandler self = this;

        //now that we have our artifacts, we go about our real businazzzz
        //lets make some cards ... son!
        for(Artifact a : offspring)
        {
            //DO NOT convert somethign that we've already converted in the past
            if(gridCardCache.retrievePhenotype(a.wid()) != null)
                continue;

            //asynch convert artifact to UI Card Object? ... Check!
            tasks.add(asyncArtifactToUIMapper.asyncConvertArtifactToUI(getActivity(), a, iecParams.get("ui"))
                    .continueWith(new Continuation<HomeGridCard, Void>() {
                        @Override
                        public Void then(Task<HomeGridCard> task) throws Exception {

                            if (task.isCancelled()) {
                                throw new RuntimeException("Converting object to UI was cancelled!");
                            } else if (task.isFaulted()) {
                                Toast.makeText(getActivity(), "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
                                Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                                throw task.getError();
                            }
                            //great success!
                            else {
                                final HomeGridCard c = task.getResult();
                                cacheAndLoad(c);
                            }

                            return null;
                        }
                    }));
        }

        Task<Void> voidTask;

        if(tasks.size() == 0)
            voidTask = Task.call(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    return null;
                }
            });
        else
            voidTask = Task.whenAll(tasks);

        //now that we've created all those promises, when they're all done,
        //we renotify that we're accepting more scrolls
        return voidTask
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        //all done with this batch, be prepared to fetch more!
                        mScrollListener.notifyMorePages();
                        return null;
                    }
                });
    }

    void cacheAndLoad(final HomeGridCard c)
    {
        if (c != null) {

            gridCardCache.cachePhenotype(c.wid, c);
            c.setButtonHandler(this);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //add the card please, but only if it hasn't been added before!
//                    for(int i=0; i < mCardArrayAdapter.getCount(); i++)
//                        if(((HomeGridCard)mCardArrayAdapter.getItem(i)).wid == c.wid)
//                            return;

                    mCardArrayAdapter.add(c);
                }
            });
        }
    }

    void initializeUI()
    {
        mCardArrayAdapter = new CardGridArrayAdapter(getActivity(), new ArrayList<Card>());

//        mCardArrayAdapter.setBitmapCacheAndFetch(this);

        CardGridView listView = (CardGridView) getActivity().findViewById(R.id.home_card_grid_view);
        if (listView != null) {
//            listView.setAdapter(mCardArrayAdapter);
            setAlphaAdapter(listView);
//            listView.setAdapter(mCardArrayAdapter);
//            listView.setAdapter(mCardArrayAdapter);

            //here we're going to set our scroll listener for creating more objects and appending them!
            mScrollListener = new EndlessGridScrollListener(listView);

            //lets set our callback item now -- this is called whenever the user scrolls to the bottom
            mScrollListener.setRequestItemsCallback(new EndlessGridScrollListener.RequestItemsCallback() {
                @Override
                public void requestItems(int pageNumber) {
                    System.out.println("On Refresh invoked..");

                    //add more cards, hoo-ray!!!

                    //every time it's the same process -- call home API for artifacts, convert to phenotype, display!
                    //rinse and repeat
                    loadNextBatchOfItems(iecParams);
                }
            });

            //make sure to add our infinite scroller here
            listView.setOnScrollListener(mScrollListener);
        }
    }

    /**
     * Alpha animation
     */
    private void setAlphaAdapter(CardGridView gridView) {
        AnimationAdapter animCardArrayAdapter = new AlphaInAnimationAdapter(mCardArrayAdapter);
        animCardArrayAdapter.setAbsListView(gridView);

        gridView.setExternalAdapter(animCardArrayAdapter, mCardArrayAdapter);

        //set the external adapter for our sticky mofo
//        gridView.setExternalAdapter(animCardArrayAdapter, mCardArrayAdapter);
    }

}
