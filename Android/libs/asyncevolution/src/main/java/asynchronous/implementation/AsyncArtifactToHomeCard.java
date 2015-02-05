package asynchronous.implementation;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Inject;

import asynchronous.interfaces.AsyncArtifactToPhenotype;
import asynchronous.interfaces.AsyncArtifactToUI;
import asynchronous.interfaces.AsyncPhenotypeToUI;
import bolts.Continuation;
import bolts.Task;
import cardUI.cards.GridCard;
import cardUI.cards.HomeGridCard;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 8/14/14.
 */
//artifact class, phenotype class, and UI class for creating async conversion process
public class AsyncArtifactToHomeCard extends AsyncArtifactToUI<Artifact, double[][], HomeGridCard> {

    @Inject
    public AsyncArtifactToPhenotype<Artifact, double[][]> artifactToPhenotypeMapper;

    @Inject
    public AsyncPhenotypeToUI<double[][], HomeGridCard> phenotypeToUIMapper;


    @Override
    public Task<HomeGridCard> asyncConvertArtifactToUI(final Activity act, final Artifact artifact, final JsonNode params) {

        //We make the full conversion here, then return the object asynchronously
        return artifactToPhenotypeMapper.asyncPhenotypeToUI(artifact, params)
                .continueWithTask(new Continuation<double[][], Task<HomeGridCard>>() {
                    @Override
                    public Task<HomeGridCard> then(Task<double[][]> task) throws Exception {

                        if(task.isCancelled())
                        {
                            throw new RuntimeException("Converting object to UI was cancelled!");
                        }
                        else if(task.isFaulted())
                        {
                            Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                            throw task.getError();
                        }
                        //great success!
                        else {

                            return phenotypeToUIMapper.asyncPhenotypeToUI(act, artifact.wid(), task.getResult(), params);
                        }
                    }
                });
    }
}
