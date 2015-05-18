package edu.eplex.androidsocialclient.MainUI.API;

import java.util.ArrayList;

import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.Confirmation;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.FeedItem;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishRequest;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishResponse;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by paul on 3/19/15.
 */
public interface FeedAPI {

    @GET("/latest")
     void asyncGetLatest(@Query("after") String startTime, Callback<ArrayList<FeedItem>> cb);

    @GET("/latest")
     void asyncGetLatest(@Query("after") String startTime, @Query("count") int number, Callback<ArrayList<FeedItem>> cb);

    @GET("/latest")
    void asyncGetLatestBefore(@Query("before") String startTime, @Query("count") int number, Callback<ArrayList<FeedItem>> cb);

    @GET("/latest")
     void asyncGetLatest(@Query("count") int number, Callback<ArrayList<FeedItem>> cb);

    @GET("/latest")
     void asyncGetLatest(Callback<ArrayList<FeedItem>> cb);

    @GET("/latest")
     ArrayList<FeedItem> syncGetLatest();

    @GET("/latest")
     ArrayList<FeedItem> syncGenerateUpload(@Query("after") String startTime);


    @GET("/hashtag")
    void asyncGetLatestByHashtag(@Query("hashtag") String hashtag, @Query("after") String startTime, @Query("count") int number, Callback<ArrayList<FeedItem>> cb);


}
