package cardUI;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import PicbreederActivations.PBBipolarSigmoid;
import PicbreederActivations.PBCos;
import PicbreederActivations.PBGaussian;
import PicbreederActivations.pbLinear;
import asynchronous.main.AsyncInfiniteIEC;
import asynchronous.modules.FakeAsyncLocalIECModule;
import cardUI.cards.GridCard;
import dagger.Module;
import dagger.ObjectGraph;
import eplex.win.FastCPPNJava.activation.CPPNActivationFactory;
import eplex.win.FastCPPNJava.activation.functions.Sine;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.BasicEvolution;
import eplex.win.winBackbone.FinishedCallback;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;
import it.gmariotti.cardslib.library.view.CardGridView;
import win.eplex.backbone.ArtifactCardCallback;
import win.eplex.backbone.ArtifactToCard;
import win.eplex.backbone.Modules.FakeArtifactModule;
import win.eplex.backbone.R;

/**
 * Grid as Google Play example
 *
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class GridSquareFragment extends BaseFragment {

    protected ScrollView mScrollView;
    private ObjectGraph graph;
    AsyncInfiniteIEC asyncIEC;

    @Override
    public int getTitleResourceId() {
        return R.string.iec_grid_title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.demo_fragment_grid_gplay, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //no recurrent networks please!
        NeatParameters np = new NeatParameters();
        //set up the defaults here
        np.pMutateAddConnection = .13;
        np.pMutateAddNode = .12;
        np.pMutateDeleteSimpleNeuron = .005;
        np.pMutateDeleteConnection = .005;
        np.pMutateConnectionWeights = .72;
        np.pMutateChangeActivations = .02;
        np.pNodeMutateActivationRate = 0.2;

        np.connectionWeightRange = 3.0;
        np.disallowRecurrence = true;

        Map<String, Double> probs = new HashMap<String, Double>();
        probs.put(PBBipolarSigmoid.class.getName(), .22);
        probs.put(PBGaussian.class.getName(), .22);
        probs.put(Sine.class.getName(), .22);
        probs.put(PBCos.class.getName(), .22);
        probs.put(pbLinear.class.getName(), .12);

        //now we set up our probabilities of generating particular activation functions
        CPPNActivationFactory.setProbabilities(probs);


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
            graph = ObjectGraph.create(Arrays.asList(new FakeAsyncLocalIECModule(getActivity(), np, null)).toArray());

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