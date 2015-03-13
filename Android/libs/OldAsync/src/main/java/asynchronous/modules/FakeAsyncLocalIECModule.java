package asynchronous.modules;

import android.app.Activity;
import android.graphics.Bitmap;

import java.util.List;

import asynchronous.implementation.AsyncArtifactToCard;
import asynchronous.implementation.AsyncLocalIEC;
import asynchronous.implementation.AsyncLocalRandomSeedLoader;
import asynchronous.implementation.SyncLocalOffspringGenerator;
import asynchronous.interfaces.AsyncArtifactToPhenotype;
import asynchronous.interfaces.AsyncArtifactToUI;
import asynchronous.interfaces.AsyncInteractiveEvolution;
import asynchronous.interfaces.AsyncPhenotypeToUI;
import asynchronous.interfaces.AsyncSeedLoader;
import asynchronous.main.AsyncInfiniteIEC;
import cache.implementations.LRUBitmapCache;
import cardUI.cards.GridCard;
import cppn.implementations.AsyncArtifactToCPPN;
import cppn.implementations.AsyncCPPNOutputToCard;
import dagger.Module;
import dagger.Provides;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.ArtifactOffspringGenerator;
import interfaces.PhenotypeCache;
import it.gmariotti.cardslib.library.internal.Card;
import win.eplex.backbone.NEATArtifact;

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