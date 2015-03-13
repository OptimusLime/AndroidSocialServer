package eplex.win.winBackbone;

import java.util.List;

/**
 * Created by paul on 8/8/14.
 */
public abstract class AsyncLoadCallback
{
    public abstract void loadedSeeds(List<Artifact> artifacts);
}