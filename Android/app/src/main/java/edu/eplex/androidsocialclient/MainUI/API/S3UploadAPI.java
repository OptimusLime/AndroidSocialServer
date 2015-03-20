package edu.eplex.androidsocialclient.MainUI.API;

import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishRequest;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by paul on 3/20/15.
 */
public interface S3UploadAPI {

    @PUT("/{username}/{folder}/{image}")
    Response syncS3Upload(@Body byte[] pngImage,
                          @Path("username") String username,
                          @Path("folder") String folder,
                          @Path("image") String imageName,
                          @Query("AWSAccessKeyId") String awsAccessKeyId,
                          @Query("Expires") long expires,
                          @Query("Signature") String Signature);




}
