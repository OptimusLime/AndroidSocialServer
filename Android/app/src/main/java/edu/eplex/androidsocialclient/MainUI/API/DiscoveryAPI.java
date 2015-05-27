package edu.eplex.androidsocialclient.MainUI.API;

import java.util.ArrayList;

import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.FeedItem;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by paul on 3/19/15.
 */
public interface DiscoveryAPI {

    @GET("/artifacts/favorite/{username}")
    ArrayList<FilterArtifact> syncGetFavoriteArtifacts(@Path("username") String username, @Query("start") int startIx, @Query("count") int count);

    @GET("/artifacts/popular")
    ArrayList<FilterArtifact> syncGetPopularArtifacts(@Query("start") int startIx, @Query("count") int count);

    @GET("/artifacts/hashtag")
    ArrayList<FilterArtifact> syncGetHashtagArtifacts(@Query("hashtag") String hashtag, @Query("after") String startIx, @Query("count") int count);

    @GET("/artifacts/latest")
    ArrayList<FilterArtifact> syncGetLatestArtifacts(@Query("after") String startIx, @Query("count") int count);
}
