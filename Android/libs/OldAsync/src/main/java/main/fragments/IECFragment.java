package main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import PicbreederActivations.PBBipolarSigmoid;
import PicbreederActivations.PBCos;
import PicbreederActivations.PBGaussian;
import PicbreederActivations.pbLinear;
import asynchronous.main.AsyncInfiniteIEC;
import asynchronous.modules.FakeAsyncLocalIECModule;
import cardUI.BaseFragment;
import cardUI.StickyGridFragment;
import dagger.ObjectGraph;
import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastNEATJava.utils.NeatParameters;
import main.NEATInitializer;
import win.eplex.backbone.NEATArtifact;
import win.eplex.backbone.R;

/**
 * Created by paul on 10/19/14.
 */
public class IECFragment extends BaseFragment {

    protected ScrollView mScrollView;
    private ObjectGraph graph;
    AsyncInfiniteIEC asyncIEC;
    List<NEATArtifact> artifactSeeds;

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

        NeatParameters np = NEATInitializer.DefaultNEATParameters();

        //initialize (only happens once no worries)
        NEATInitializer.InitializeActivationFunctions();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jNode = mapper.createObjectNode();

        ObjectNode uiParams = mapper.createObjectNode();

        int width = (int)getActivity().getResources().getInteger(R.integer.cppn_render_width);
        int height = (int)getActivity().getResources().getInteger(R.integer.cppn_render_height);

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
            graph = ObjectGraph.create(Arrays.asList(new FakeAsyncLocalIECModule(getActivity(), np, artifactSeeds)).toArray());

            //now inject ourselves! mwahahahahaha
            //a-rod loves this line
            //SPORTZ!
            asyncIEC = graph.get(AsyncInfiniteIEC.class);
            asyncIEC.injectGraph(getActivity(), graph);

            //now we initialize everything! This can be an async task, if you want
            //those it's pretty simple on its own
            asyncIEC.asyncInitializeIECandUI(jNode);
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
