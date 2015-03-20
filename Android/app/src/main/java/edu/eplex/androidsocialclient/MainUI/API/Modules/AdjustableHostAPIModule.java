package edu.eplex.androidsocialclient.MainUI.API.Modules;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import edu.eplex.androidsocialclient.MainUI.API.S3UploadAPI;
import edu.eplex.androidsocialclient.MainUI.API.PublishAPI;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import retrofit.RestAdapter;

/**
 * Created by paul on 3/19/15.
 */
@Module(
        injects= {
                WinAPIManager.class
        }
)
public class AdjustableHostAPIModule {

    Context activity;
    int apiEndpoint;
    RestAdapter restAdapter;
    RestAdapter s3Adapter;

    public AdjustableHostAPIModule(Context activity, int apiEndpointResource, int amazonEndpoint)
    {
        this.apiEndpoint = apiEndpointResource;
        this.activity = activity;

        restAdapter = new RestAdapter.Builder()
                .setEndpoint(activity.getResources().getString(apiEndpoint))
                .build();

        s3Adapter = new RestAdapter.Builder()
                .setEndpoint(activity.getResources().getString(amazonEndpoint))
                .build();
    }

    //Handle async infinite injections
    //AsyncInfiniteIEC.class
    @Provides
    public PublishAPI providePublishAPI(){
        return restAdapter.create(PublishAPI.class);
    }

    @Provides
    public S3UploadAPI provideS3UploadAPI(){
        return s3Adapter.create(S3UploadAPI.class);
    }


    @Provides
    public RestAdapter provideLocalRestAdapter(){

        return restAdapter;
    }
}