package asynchronous.implementation;

import android.app.Activity;

import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Inject;

import asynchronous.interfaces.AsyncArtifactToPhenotype;
import asynchronous.interfaces.AsyncArtifactToUI;
import asynchronous.interfaces.AsyncPhenotypeToUI;
import bolts.Continuation;
import bolts.Task;
import cardUI.cards.GridCard;
import eplex.win.winBackbone.Artifact;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by paul on 8/14/14.
 */
//artifact class, phenotype class, and UI class for creating async conversion process
public class AsyncArtifactToCard extends AsyncArtifactToUI<Artifact, double[][], GridCard> {

    @Inject
    public AsyncArtifactToPhenotype<Artifact, double[][]> artifactToPhenotypeMapper;

    @Inject
    public AsyncPhenotypeToUI<double[][], GridCard> phenotypeToUIMapper;


    @Override
    public Task<GridCard> asyncConvertArtifactToUI(final Activity act, final Artifact artifact, final JsonNode params) {

        //We make the full conversion here, then return the object asynchronously
        return artifactToPhenotypeMapper.asyncPhenotypeToUI(artifact, params)
                .continueWithTask(new Continuation<double[][], Task<GridCard>>() {
                    @Override
                    public Task<GridCard> then(Task<double[][]> task) throws Exception {
                        return phenotypeToUIMapper.asyncPhenotypeToUI(act, artifact.wid(), task.getResult(), params);
                    }
                });
    }
}
