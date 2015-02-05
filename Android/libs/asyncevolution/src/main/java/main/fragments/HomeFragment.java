package main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import asynchronous.main.AsyncInfiniteHome;
import asynchronous.modules.AsyncWinArkHomeModule;
import cardUI.BaseFragment;
import dagger.ObjectGraph;
import main.NEATInitializer;
import win.eplex.backbone.R;

/**
 * Created by paul on 10/19/14.
 */
public class HomeFragment extends BaseFragment {

    private ObjectGraph graph;
    AsyncInfiniteHome asyncHomeUI;

    /**
     * Default layout to apply to card
     */
    protected int list_card_layout_resourceID = R.layout.carddemo_grid_gplay;

    @Override
    public int getTitleResourceId() {
        return R.string.home_grid_title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_grid_infinite_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        //make sure it happens at least once (it will prevent double calls)
        NEATInitializer.InitializeActivationFunctions();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jNode = mapper.createObjectNode();

        ObjectNode uiParams = mapper.createObjectNode();

        int width = (int)getActivity().getResources().getInteger(R.integer.home_cppn_render_width);
        int height = (int)getActivity().getResources().getInteger(R.integer.home_cppn_render_height);

        uiParams.set("width", mapper.convertValue(width, JsonNode.class));
        uiParams.set("height", mapper.convertValue(height , JsonNode.class));
        jNode.set("ui", uiParams);

        ObjectNode parentUIParams = mapper.createObjectNode();

        int pWidth = (int)getActivity().getResources().getInteger(R.integer.cppn_parent_render_width);
        int pHeight = (int)getActivity().getResources().getInteger(R.integer.cppn_parent_render_height);

        parentUIParams.set("width", mapper.convertValue(pWidth, JsonNode.class));
        parentUIParams.set("height", mapper.convertValue(pHeight , JsonNode.class));

        jNode.set("parents", parentUIParams);

        if(graph == null)
        {
            //we need to inject our objects!
            graph = ObjectGraph.create(Arrays.asList(new AsyncWinArkHomeModule(getActivity())).toArray());

            //now inject ourselves! mwahahahahaha
            //a-rod loves this line
            //SPORTZ!
            asyncHomeUI = graph.get(AsyncInfiniteHome.class);
            asyncHomeUI.injectGraph(getActivity(), graph);

            //now we initialize everything! This can be an async task, if you want
            //those it's pretty simple on its own
            asyncHomeUI.asyncInitializeHomeandUI(jNode);
        }
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
