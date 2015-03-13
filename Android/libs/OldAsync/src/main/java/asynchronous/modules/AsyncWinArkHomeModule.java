package asynchronous.modules;

import android.app.Activity;
import android.graphics.Bitmap;

import api.HomeAPIService;
import asynchronous.implementation.AsyncArtifactToHomeCard;
import asynchronous.implementation.AsyncScrollHomeUI;
import asynchronous.interfaces.AsyncArtifactToPhenotype;
import asynchronous.interfaces.AsyncArtifactToUI;
import asynchronous.interfaces.AsyncPhenotypeToUI;
import asynchronous.interfaces.AsyncSeedLoader;
import asynchronous.main.AsyncInfiniteHome;
import cache.implementations.LRUBitmapCache;
import cardUI.cards.HomeGridCard;
import cppn.implementations.AsyncArtifactToCPPN;
import cppn.implementations.AsyncCPPNOutputToHomeCard;
import dagger.Module;
import dagger.Provides;
import eplex.win.winBackbone.Artifact;
import interfaces.PhenotypeCache;
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
