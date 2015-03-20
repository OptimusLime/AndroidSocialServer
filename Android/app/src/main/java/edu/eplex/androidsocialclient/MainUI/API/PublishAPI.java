package edu.eplex.androidsocialclient.MainUI.API;

import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.Confirmation;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishRequest;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishResponse;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by paul on 3/19/15.
 */
public interface PublishAPI {

    @GET("/upload/generate")
    void asyncGenerateUpload(@Query("AccessToken") String accessToken, Callback<PublishResponse> cb);

    @GET("/upload/generate")
    PublishResponse syncGenerateUpload(@Query("AccessToken") String accessToken);


    //standard username/password login attempt here
    @POST("/upload/confirm")
    void asyncConfirmUpload(@Body PublishRequest publish, Callback<Confirmation> cb);

    @POST("/upload/confirm")
    Confirmation syncConfirmUpload(@Body PublishRequest publish);

}
