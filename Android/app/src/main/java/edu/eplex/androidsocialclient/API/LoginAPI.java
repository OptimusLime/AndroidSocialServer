package edu.eplex.androidsocialclient.API;

import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.LoginRequest;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.UsernameCheck;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by paul on 12/3/14.
 */
public interface LoginAPI {
    //here we send the user off to check if they've already signed up with facebook
    //if so, we get back our api token, no problem
    //if not, we need to do one more step -- sign them up!
    @POST("/auth/login/facebook")
    void asyncFacebookLoginRequest(@Body AccessToken token, Callback<APIToken> cb);

    //do it again, synchronized! For slowness!
    @POST("/auth/login/facebook")
    APIToken syncFacebookLoginRequest(@Body AccessToken token);

    //standard username/password login attempt here
    @POST("/auth/login")
    void asyncLoginRequest(@Body LoginRequest login, Callback<APIToken> cb);

    //do it again, synchronized! For slowness!
    @POST("/auth/login")
    APIToken syncLoginRequest(@Body LoginRequest login);

    @POST("/auth/signup/facebook")
    void asyncFacebookSignup(@Body OAuth2Signup signupRequest, Callback<APIToken> cb);

    @POST("/auth/signup/facebook")
    APIToken syncFacebookSignup(@Body OAuth2Signup signupRequest);

    @POST("/auth/signup/email")
    void asyncEmailSignup(@Body OAuth2Signup signupRequest, Callback<APIToken> cb);

    @POST("/auth/signup/email")
    APIToken syncEmailSignup(@Body OAuth2Signup signupRequest);

    @GET("/auth/signup/{username}")
    void asyncUsernameCheck(@Path("username") String usernameCheck, Callback<UsernameCheck> cb);

    @GET("/auth/signup/{username}")
    UsernameCheck syncUsernameCheck(@Path("username") String usernameCheck);

    @GET("/auth/verify")
    APIToken syncVerifyAPIAccess(@Query("api_token") String apiToken);


}
