package edu.eplex.AsyncEvolution.asynchronous.interfaces;

import android.app.Activity;

import com.fasterxml.jackson.databind.JsonNode;

import bolts.Task;
import eplex.win.winBackbone.Artifact;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by paul on 8/14/14.
 */
public abstract class AsyncArtifactToUI<ArtClass, PhenoClass, UIClass>  {

    public abstract Task<UIClass> asyncConvertArtifactToUI(Activity act, Artifact artifact, JsonNode params);
}
