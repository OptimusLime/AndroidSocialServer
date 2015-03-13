package interfaces;

import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 8/20/14.
 */
public interface PhenotypeCache<T> {

    //cache the phenotype in some format
    void cachePhenotype(String wid, T phenotype);

    //get the phenotype back from the cache
    //returns null if it's not in the cache!
    T retrievePhenotype(String wid);
}
