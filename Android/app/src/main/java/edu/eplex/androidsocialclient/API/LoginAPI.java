package edu.eplex.androidsocialclient.API;

import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;

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

    @POST("/auth/signup/facebook")
    void asyncFacebookSignup(@Body OAuth2Signup signupRequest, Callback<APIToken> cb);
}
