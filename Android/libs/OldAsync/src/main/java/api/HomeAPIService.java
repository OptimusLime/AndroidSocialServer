package api;

import java.util.List;

import eplex.win.winBackbone.Artifact;
import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by paul on 10/19/14.
 */
public interface HomeAPIService {
    @GET("/home/recent")
    void asyncGetHomeArtifacts(@Query("start") int startIx, @Query("end") int endIx, Callback<Response> cb);

    @GET("/home/recent")
    Response getHomeArtifacts(@Query("start") int startIx, @Query("end") int endIx);

}
