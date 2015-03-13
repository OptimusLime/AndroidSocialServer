package eplex.win.winBackbone;

import java.util.List;

/**
 * Created by paul on 8/8/14.
 */
public interface AsyncEvolutionLoader {
   //handles loading seeds in an asynchronous manner
    void loadSeeds(AsyncLoadCallback callback);
}



