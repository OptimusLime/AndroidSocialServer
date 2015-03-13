package eplex.win.winBackbone;

import java.util.List;


/**
 * Created by paul on 8/8/14.
 */
public interface ArtifactOffspringGenerator {

    Artifact createArtifactFromParents(List<Artifact> parents);
    void clearSession();

}
