package edu.eplex.AsyncEvolution.asynchronous.modules;

import android.app.Activity;
import android.graphics.Bitmap;

import edu.eplex.AsyncEvolution.api.HomeAPIService;
import edu.eplex.AsyncEvolution.asynchronous.implementation.AsyncArtifactToHomeCard;
import edu.eplex.AsyncEvolution.asynchronous.implementation.AsyncScrollHomeUI;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncArtifactToPhenotype;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncArtifactToUI;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncPhenotypeToUI;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncSeedLoader;
import edu.eplex.AsyncEvolution.asynchronous.main.AsyncInfiniteHome;
import edu.eplex.AsyncEvolution.cache.implementations.LRUBitmapCache;
import edu.eplex.AsyncEvolution.cardUI.cards.HomeGridCard;
import edu.eplex.AsyncEvolution.cppn.implementations.AsyncArtifactToCPPN;
import edu.eplex.AsyncEvolution.cppn.implementations.AsyncCPPNOutputToHomeCard;
import dagger.Module;
import dagger.Provides;
import eplex.win.winBackbone.Artifact;
import edu.eplex.AsyncEvolution.interfaces.PhenotypeCache;
import retrofit.RestAdapter;

/**
 * Created by paul on 10/19/14.
 */
@Module(
        injects={
                AsyncScrollHomeUI.class,
                AsyncInfiniteHome.class,
                AsyncArtifactToHomeCard.class}
)
public class AsyncWinArkHomeModule {

    Activity activity;
    RestAdapter winarkAPI;

    public AsyncWinArkHomeModule(Activity activity)
    {
        this.activity = activity;

        winarkAPI = new RestAdapter.Builder()
                .setEndpoint("http://winark.org/apps/win-Picbreeder/api")
                .build();
    }

    @Provides
    public AsyncArtifactToUI<Artifact, double[][], HomeGridCard> provideAsyncArtifactToHomeCard(){
        return new AsyncArtifactToHomeCard();
    }

    @Provides
    public PhenotypeCache<Bitmap> provideBitmapCache()
    {
        return new LRUBitmapCache<Bitmap>(100);
    }

    @Provides
    public PhenotypeCache<HomeGridCard> provideGridCardCache()
    {
        return new LRUBitmapCache(100);
    }


    //Handle loading our artifacts from the Home API
    @Provides
    public AsyncSeedLoader provideAsyncSeedLoading(){
        return new AsyncScrollHomeUI();
    }

    //now we handle AsyncArtifactToUI.class injections
    //this provides the conversion between artifact and network outputs,
    //then takes those network outputs and converts them to cards
    @Provides
    public AsyncArtifactToPhenotype<Artifact, double[][]> provideArtifactToPhenotypeConverter(){
        return new AsyncArtifactToCPPN();
    }

    @Provides
    public AsyncPhenotypeToUI<double[][], HomeGridCard> providePhenotypeToUIConverter(){
        return new AsyncCPPNOutputToHomeCard();
    }

    @Provides
    public HomeAPIService provideHomeAPIService()
    {
        return winarkAPI.create(HomeAPIService.class);
    }

}
