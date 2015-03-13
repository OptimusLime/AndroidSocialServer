package edu.eplex.AsyncEvolution.main.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;

import edu.eplex.AsyncEvolution.asynchronous.main.AsyncInfiniteIEC;
import edu.eplex.AsyncEvolution.asynchronous.modules.FakeAsyncLocalIECModule;
import edu.eplex.AsyncEvolution.cardUI.BaseFragment;
import dagger.ObjectGraph;
import eplex.win.FastNEATJava.utils.NeatParameters;
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import edu.eplex.AsyncEvolution.backbone.NEATArtifact;
import edu.eplex.AsyncEvolution.R;

/**
 * Created by paul on 10/19/14.
 */
public class IECFragment extends BaseFragment {

    protected ScrollView mScrollView;
    private ObjectGraph graph;
    AsyncInfiniteIEC asyncIEC;
    List<NEATArtifact> artifactSeeds;

    JsonNode inputParams;
    NeatParameters neatInputParams;

    public void setFragmentSeeds(List<NEATArtifact> seeds)
    {
        artifactSeeds = seeds;
    }

    /**
     * Default layout to apply to card
     */
    protected int list_card_layout_resourceID = R.layout.carddemo_grid_gplay;

    @Override
    public int getTitleResourceId() {
        return R.string.iec_grid_title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sticky_grid_infinite_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //now we have np and params, we make our evolution happen
        if(graph == null)
        {
            //we need to inject our objects!
            graph = ObjectGraph.create(Arrays.asList(new FakeAsyncLocalIECModule(getActivity(), neatInputParams, artifactSeeds)).toArray());

            //now inject ourselves! mwahahahahaha
            //a-rod loves this line
            //SPORTZ!
            asyncIEC = graph.get(AsyncInfiniteIEC.class);
            asyncIEC.injectGraph(getActivity(), graph);

            //now we initialize everything! This can be an async task, if you want
            //those it's pretty simple on its own
            asyncIEC.asyncInitializeIECandUI(inputParams);
        }
    }

    public JsonNode DefaultParams(FragmentActivity activity)
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jNode = mapper.createObjectNode();

        ObjectNode uiParams = mapper.createObjectNode();

        int width = (int)activity.getResources().getInteger(R.integer.cppn_render_width);
        int height = (int)activity.getResources().getInteger(R.integer.cppn_render_height);

        uiParams.set("width", mapper.convertValue(width, JsonNode.class));
        uiParams.set("height", mapper.convertValue(height , JsonNode.class));
        jNode.set("ui", uiParams);

        ObjectNode parentUIParams = mapper.createObjectNode();

        int pWidth = (int)activity.getResources().getInteger(R.integer.cppn_parent_render_width);
        int pHeight = (int)activity.getResources().getInteger(R.integer.cppn_parent_render_height);

        parentUIParams.set("width", mapper.convertValue(pWidth, JsonNode.class));
        parentUIParams.set("height", mapper.convertValue(pHeight , JsonNode.class));

        jNode.set("parents", parentUIParams);
        return jNode;
    }

    public void Initialize(FragmentActivity act, JsonNode params, NeatParameters np)
    {
        if(np == null)
        {
            np = NEATInitializer.DefaultNEATParameters();
        }

        //initialize (only happens once no worries)
        NEATInitializer.InitializeActivationFunctions();

        if(params == null)
            params = DefaultParams(act);

        inputParams = params;
        neatInputParams = np;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if(uiObject != null)
//            uiObject.cardGenerator.spicyStop();
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

}
