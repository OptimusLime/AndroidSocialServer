package win.eplex.backbone;

import android.app.Activity;

import eplex.win.winBackbone.Artifact;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by paul on 8/13/14.
 */
public interface ArtifactToCard {
    Card convertArtifactToUI(Artifact offspring);
    void asyncConvertArtifactToUI(Artifact offspring, ArtifactCardCallback finished);
    void spicyStart(Activity act);
    void spicyStop();
}
