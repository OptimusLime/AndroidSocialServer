package edu.eplex.AsyncEvolution.asynchronous.modules;

import android.app.Activity;
import android.graphics.Bitmap;

import java.util.List;

import edu.eplex.AsyncEvolution.asynchronous.implementation.AsyncArtifactToCard;
import edu.eplex.AsyncEvolution.asynchronous.implementation.AsyncLocalIEC;
import edu.eplex.AsyncEvolution.asynchronous.implementation.AsyncLocalRandomSeedLoader;
import edu.eplex.AsyncEvolution.asynchronous.implementation.SyncLocalOffspringGenerator;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncArtifactToPhenotype;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncArtifactToUI;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncInteractiveEvolution;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncPhenotypeToUI;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncSeedLoader;
import edu.eplex.AsyncEvolution.asynchronous.main.AsyncInfiniteIEC;
import edu.eplex.AsyncEvolution.cache.implementations.LRUBitmapCache;
import edu.eplex.AsyncEvolution.cardUI.cards.GridCard;
import edu.eplex.AsyncEvolution.cppn.implementations.AsyncArtifactToCPPN;
import edu.eplex.AsyncEvolution.cppn.implementations.AsyncCPPNOutputToCard;
import dagger.Module;
import dagger.Provides;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.ArtifactOffspringGenerator;
import edu.eplex.AsyncEvolution.interfaces.PhenotypeCache;
import it.gmariotti.cardslib.library.internal.Card;
import edu.eplex.AsyncEvolution.backbone.NEATArtifact;

/**
 * Created by paul on 8/14/14.
 */
@Module(
        injects={
                AsyncInfiniteIEC.class,
                //this next injects must match the asyncinteractiveevolution callback
                AsyncLocalIEC.class,
                AsyncArtifactToCard.class}
)
public class FakeAsyncLocalIECModule {

    Activity activity;
    NeatParameters np;
    List<NEATArtifact> seeds;
    public FakeAsyncLocalIECModule(Activity activity, NeatParameters np, List<NEATArtifact> seeds)
    {
        this.np = np;
        this.activity = activity;
        this.seeds = seeds;
    }

    FakeAsyncLocalIECModule(List<NEATArtifact> seeds){
        this.seeds = seeds;
    }

    //Handle async infinite injections
    //AsyncInfiniteIEC.class
    @Provides
    public AsyncInteractiveEvolution provideAsyncIEC(){
        return new AsyncLocalIEC();
    }

    @Provides
    public AsyncArtifactToUI<Artifact, double[][], GridCard> provideAsyncArtifactToCard(){
        return new AsyncArtifactToCard();
    }

    @Provides
    public PhenotypeCache<Bitmap> provideBitmapCache()
    {
        return new LRUBitmapCache<Bitmap>(100);
    }

    @Provides
    public PhenotypeCache<GridCard> provideGridCardCache()
    {
        return new LRUBitmapCache(100);
    }

    //Handle AsyncLocalIEC injections!
    //this requires a seed loader -- to fetch the starting objects from whereever
    //and also an offspring generator, to merege objects
    @Provides
    public AsyncSeedLoader provideAsyncSeedLoading(){
        AsyncLocalRandomSeedLoader seedLoader = new AsyncLocalRandomSeedLoader();

        //pull the asset manager from our set (I hope) activity
        seedLoader.assetManager = activity.getAssets();
        seedLoader.customSeeds = seeds;

        return seedLoader;
    }

    //offspring generator handles taking in a collection of artifact parents,
    //handling any unusual artifact generation logic, then returning the children
    @Provides
    public ArtifactOffspringGenerator provideOffspringGenerator() {
        return new SyncLocalOffspringGenerator(this.np);
    }


    //now we handle AsyncArtifactToUI.class injections
    //this provides the conversion between artifact and network outputs,
    //then takes those network outputs and converts them to cards
    @Provides
    public AsyncArtifactToPhenotype<Artifact, double[][]> provideArtifactToPhenotypeConverter(){
        return new AsyncArtifactToCPPN();
    }

    @Provides
    public AsyncPhenotypeToUI<double[][], GridCard> providePhenotypeToUIConverter(){
        return new AsyncCPPNOutputToCard();
    }
}