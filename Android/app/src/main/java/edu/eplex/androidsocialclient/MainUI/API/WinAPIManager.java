package edu.eplex.androidsocialclient.MainUI.API;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import bolts.Continuation;
import bolts.Task;
import dagger.ObjectGraph;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.Confirmation;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.FeedItem;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.UploadURL;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishRequest;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishResponse;
import edu.eplex.androidsocialclient.MainUI.Cache.BitmapCacheManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.R;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

/**
 * Created by paul on 3/19/15.
 */
public class WinAPIManager {

//    public static final MediaType JSON
//            = MediaType.parse("application/json; charset=utf-8");
////
//    public static final MediaType PNG
//            = MediaType.parse("image/png");
//
//    public static final MediaType PLAINTEXT
//            = MediaType.parse("text/plain");

    public static String FILTER_THUMB = "filterThumbnail.png";
    public static String FILTER_FULL = "filterFull.png";
    public static String IMAGE_THUMB = "imageThumbnail.png";
    public static String IMAGE_FULL = "imageFull.png";


    @Inject
    RestAdapter restAdapter;

    @Inject
    PublishAPI publishAPI;

    @Inject
    S3UploadAPI s3UploadAPI;

    @Inject
    FeedAPI feedAPI;

    @Inject
    DiscoveryAPI discoveryAPI;

//    OkHttpClient client = new OkHttpClient();

    private static WinAPIManager instance = null;
    protected WinAPIManager() {
        // Exists only to defeat instantiation.

        //need to construct object graph and inject self
    }

    public static WinAPIManager getInstance() {
        if(instance == null) {
            instance = new WinAPIManager();
        }
        return instance;
    }

    public void injectAPIManager(ObjectGraph graph)
    {
        graph.inject(this);
    }

    HashMap<String, PublishRequest> confirmedUploads;// = new HashMap<>();
    final static String SUCCESSFUL_UPLOAD_SAVE_LOCATION = "SavedUploadRequests";

    public Task<Void> saveSuccessfulFilterUpload(String wid, PublishRequest pr, final Context mContext)
    {
        if(confirmedUploads ==null)
            confirmedUploads = new HashMap<>();

        confirmedUploads.put(wid, pr);

        return Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();

                mapper.writeValue(out, confirmedUploads);

                FileOutputStream fos = mContext.openFileOutput(SUCCESSFUL_UPLOAD_SAVE_LOCATION, Context.MODE_PRIVATE);
                fos.write(out.toByteArray());
                fos.close();

                return null;
            }
        });
    }

    public Task<PublishRequest> asyncCheckSuccessfulUpload(final String wid, final Context mContext)
    {
        if(confirmedUploads == null) {
            return Task.call(new Callable<PublishRequest>() {
                @Override
                public PublishRequest call() throws Exception {

                    ObjectMapper mapper = new ObjectMapper();

                    try {
                        confirmedUploads = mapper.readValue(
                                new InputStreamReader(mContext.openFileInput(SUCCESSFUL_UPLOAD_SAVE_LOCATION)),
                                new TypeReference<HashMap<String, PublishRequest>>() {
                                });
                    }
                    catch (IOException e)
                    {
                        confirmedUploads = new HashMap<String, PublishRequest>();
                    }

//                    confirmedUploads = new HashMap<String, PublishRequest>();

                    return confirmedUploads.get(wid);
                }
            });
        }
        else
            return Task.call(new Callable<PublishRequest>() {
                @Override
                public PublishRequest call() throws Exception {
                    return confirmedUploads.get(wid);
                }
            });
    }



    public static ByteArrayOutputStream encodeToPNG(Bitmap image)
    {
        Bitmap immagex=image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);

        return baos;
    }

    public static Bitmap decodePNGToBitmap(byte[] input)
    {
//        byte[] decodedByte = Base64.decode(input, 0);

        return BitmapFactory.decodeByteArray(input, 0, input.length);
    }

    Task<Void> asyncUploadImage(final UploadURL url, final Bitmap image)
    {
        return Task.callInBackground(new Callable<ByteArrayOutputStream>() {
            @Override
            public ByteArrayOutputStream call() throws Exception {
                return encodeToPNG(image);
            }
        }).continueWith(new Continuation<ByteArrayOutputStream, HttpResponse>() {
            @Override
            public HttpResponse then(Task<ByteArrayOutputStream> task) throws Exception {

                if (task.getResult() != null) {
                    //we need to pull the info from the url
                    String s3Key = url.request.Key;

                    String[] urlParams = s3Key.split("\\/");

                    String[] qSplit = url.url.split("\\?");

                    String[] signature = qSplit[1].split("\\&");

                    byte[] data = task.getResult().toByteArray();

                    TypedByteArray typedByteArray = new TypedByteArray("binary/octet-stream", data);//"application/json; charset=UTF-8", data);

//                    @Body byte[] pngImage,
//                    @Header("Content-Length") int contentLength,
//                    @Path("username") String username,
//                    @Path("folder") String folder,
//                    @Path("image") String imageName,
//                    @Query("AWSAccessKeyId") String awsAccessKeyId,
//                    @Query("Expires") long expires,
//                    @Query("Signature") String Signature

                    String awsAccessKeyId = signature[0].split("\\=")[1];
                    long expires = Long.parseLong(signature[1].split("\\=")[1]);
                    String sig = signature[2].split("\\=")[1];
//
                    HttpClient client = new DefaultHttpClient();
                    HttpPut put = new HttpPut(url.url);
//                    HttpPut put = new HttpPut("http://192.168.1.101:8000/check/put");//url.url);
//                    put.removeHeaders();
                    org.apache.http.Header[] defaults = put.getAllHeaders();

                    put.removeHeaders("Content-Length");
                    put.removeHeaders("Content-Type");
                    put.removeHeaders("Accept");
                    defaults = put.getAllHeaders();

//                    put.addHeader("Content-Type", "binary/octet-stream");
                    put.addHeader("Connection", "close");
                    put.addHeader("User-Agent", "filters");
                    put.addHeader("Host", url.request.Bucket + ".s3.amazonaws.com");
//                    put.addHeader("Content-Length", "" + data.length);
                    put.addHeader("Accept", "*/*");
                    put.setEntity(new ByteArrayEntity(data));//new StringEntity(jsonObj.writeValue()));
                    defaults = put.getAllHeaders();

                    return client.execute(put);
//                    HttpResponse response = client.execute(put);


//                    SyncHttpClient client = new SyncHttpClient();
//                    client.removeAllHeaders();
//                    client.addHeader("Accept", "*/*");
//                    RequestParams requestParams = new RequestParams();
//                    requestParams.put("image", new ByteArrayInputStream(data));
//
////                    client.addHeader("Content-Length", requestParams."" + data.length);
//                    client.set
//
//                    client.put(url.url, requestParams, new AsyncHttpResponseHandler() {
//
//                        @Override
//                        public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) {
//                            Log.d("TAGS", "" + statusCode);
//                        }
//
//                        @Override
//                        public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) {
//                            Log.d("TAG", "" + statusCode);
//                        }
//                    });

//                    OkHttpClient client = new OkHttpClient();
//
//                    RequestBody body = RequestBody.create(PNG, data);
//                    com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
//                            .url(url.url)
//                            .put(body)
//                            .build();
//
//                    com.squareup.okhttp.Response response = client.newCall(request).execute();
//                    return response;



//                    RequestBody body = Request.create(JSON, json);
//                    ArrayList<Header> headers = new ArrayList<Header>();
//                    headers.add(new Header("Accept", "*/*"));
//                    headers.add(new Header("Content-Length", "" + typedByteArray.getBytes().length));
//
//                    //send a put request please
//                    Request request = new Request("PUT", url.url, headers, typedByteArray);
//                    Response r = client.execute(request);
//
//                    return r;
//
//                    Response response = client.newCall(request).execute();
//                    return response.body().string();


//                    return s3UploadAPI.syncS3Upload(typedByteArray,
//                            typedByteArray.getBytes().length,
//                            url.request.Bucket + ".s3.amazonaws.com",
//                            "close",
//                            "",
//                            urlParams[0],
//                            urlParams[1],
//                            urlParams[2],
//                            awsAccessKeyId,
//                            expires,
//                            sig);


//                    return null;// post(url, task.getResult().toByteArray());

                }
                //now we must upload to the url provided
                return null;
            }
        }).continueWith(new Continuation<HttpResponse, Void>() {
            @Override
            public Void then(Task<HttpResponse> task) throws Exception {

                if(task.getResult() != null)
                {
                    //get info from the response -- was ita a success?
                    if(task.getResult().getStatusLine().getStatusCode() == 200)
                        return null;
                }

                //TODO custom exception
                throw new Exception("Upload to S3 failed");
            }
        });
    }

//    Response post(String url, byte[] pngData) throws IOException {
//        RequestBody body = RequestBody.create(PNG, pngData);
//        Request request = new Request.Builder()
//                .url(url)
//                .post(body)
//                .build();
//
//        return client.newCall(request).execute();
//    }


    Continuation<Void, Confirmation> ConfirmUpload(final Context mContext, final PublishRequest pr, final FilterComposite artifactToPublish)
    {
        return new Continuation<Void, Confirmation>() {
            @Override
            public Confirmation then(Task<Void> task) throws Exception {

                //if we made it this far, we've uploaded images
                if (!task.isCancelled() || !task.isFaulted()) {
                    //wow, lets go ahead and confirm, and send our filter artifact while we're at it too!
                    //we should actually save this attempt -- so that we don't upload twice

                    pr.accessToken = "bobsyourunclefool";
                    pr.filterArtifacts = new HashMap<String, FilterArtifact>();
                    FilterArtifact fa = artifactToPublish.getFilterArtifact();
                    //need to read in privacy choice from somewhere -- not here though
                    fa.isPrivate = "false";
                    pr.filterArtifacts.put(fa.wid(), fa);

                    saveSuccessfulFilterUpload(artifactToPublish.getUniqueID(), pr, mContext);

                    return publishAPI.syncConfirmUpload(pr);
                }
                else
                    throw new Error("Upload to S3 failed: " + (task.getError() != null ? task.getError().getMessage() : " unknown reason"));
            }
        };
    }

    public Task<Boolean> loadImage(Context mContext, final FilterComposite artifact, final boolean isFilter, final boolean isThumbnail, int size)
    {
        final Task<Boolean>.TaskCompletionSource singleLoadImage = Task.create();

        try {
            BitmapCacheManager.getInstance().lazyLoadBitmap(mContext, artifact.getImageURL(), size, isFilter, new BitmapCacheManager.LazyLoadedCallback() {
                @Override
                public void imageLoaded(String url, Bitmap bitmap) {

                    if(isThumbnail)
                    {
                        if(isFilter)
                            artifact.setFilteredThumbnailBitmap(bitmap);
                        else
                            artifact.setThumbnailBitmap(bitmap);
                    }
                    else
                    {
                        if(isFilter)
                            artifact.setFilteredBitmap(bitmap);
                        else
                            artifact.setCurrentBitmap(bitmap);
                    }

                    singleLoadImage.setResult(true);
                }

                @Override
                public void imageLoadFailed(String reason, Exception e) {
                    singleLoadImage.setError(e);
                }
            });
        }
        catch (Exception e)
        {
            singleLoadImage.setError(e);
        }

        return singleLoadImage.getTask();
    }

    public Task<FilterComposite> loadImageAndFilter(final Context mContext, final FilterComposite filter, final boolean isThumbnail, final int size)
    {
        return loadImage(mContext, filter, false, isThumbnail, size)
                .continueWithTask(new Continuation<Boolean, Task<FilterComposite>>() {
                    @Override
                    public Task<FilterComposite> then(Task<Boolean> task) throws Exception {
                        return EditFlowManager.getInstance().asyncRunFilterOnImage(mContext, filter, size, isThumbnail, (isThumbnail ? filter.getThumbnailBitmap() : filter.getCurrentBitmap()));
                    }
                });
    }

    public Task<Void> ensureFilteredImagesExist(final Context mContext, final FilterComposite artifact)
    {
        ArrayList<Task<FilterComposite>> loadImageTasks = new ArrayList<>();

        boolean isFilter;
        int fullSize = mContext.getResources().getInteger(R.integer.max_filtered_image_size);
        int thumbnailSize = (int)mContext.getResources().getDimension(R.dimen.app_edit_iec_thumbnail_size);

        //For now, do this regardless of what happens to ensuyre that we aren't usign recycled bitmap
//        if(artifact.getThumbnailBitmap() == null)
//        {
            loadImageTasks.add(loadImageAndFilter(mContext, artifact, true, thumbnailSize));
//        }

//        if(artifact.getCurrentBitmap() == null)
//        {
            loadImageTasks.add(loadImageAndFilter(mContext, artifact, false, fullSize));
//        }

        return Task.whenAll(loadImageTasks);
    }


    public Task<Boolean> asyncPublishArtifact(final Context mContext, final FilterComposite artifactToPublish)
    {
//        final PublishRequest checkResponse = checkSuccessfulUpload(artifactToPublish.getUniqueID());
        return ensureFilteredImagesExist(mContext, artifactToPublish)
                .continueWithTask(new Continuation<Void, Task<PublishRequest>>() {
                    @Override
                    public Task<PublishRequest> then(Task<Void> task) throws Exception {
                        return asyncCheckSuccessfulUpload(artifactToPublish.getUniqueID(), mContext);
                    }
                })
                .continueWithTask(new Continuation<PublishRequest, Task<Boolean>>() {
                    @Override
                    public Task<Boolean> then(Task<PublishRequest> task) throws Exception {

                        final PublishRequest checkResponse = task.getResult();

                        if (checkResponse == null) {
                            //we have to do everything
                            final PublishRequest pr = new PublishRequest();

                            return Task.callInBackground(new Callable<PublishResponse>() {
                                @Override
                                public PublishResponse call() throws Exception {
                                    //need to generate a publish request first
                                    return publishAPI.syncGenerateUpload(null);
                                }
                            }).continueWithTask(new Continuation<PublishResponse, Task<Void>>() {
                                @Override
                                public Task<Void> then(Task<PublishResponse> task) throws Exception {

                                    //we got the publish response, we need to check for errors
                                    if (task.getResult() != null) {
                                        PublishResponse publishResponse = task.getResult();

                                        pr.uuid = publishResponse.uuid;

                                        UploadURL[] uploads = publishResponse.uploads;

                                        ArrayList<Task<Void>> asyncUploadRequests = new ArrayList<>();

                                        //uplaod it all, plz!
                                        for (int i = 0; i < uploads.length; i++) {
                                            UploadURL upURL = uploads[i];
                                            String uploadKey = upURL.request.Key;
                                            if (uploadKey.contains(FILTER_FULL)) {
                                                asyncUploadRequests.add(asyncUploadImage(upURL, artifactToPublish.getFilteredBitmap()));
                                            } else if (uploadKey.contains(FILTER_THUMB)) {
                                                asyncUploadRequests.add(asyncUploadImage(upURL, artifactToPublish.getFilteredThumbnailBitmap()));
                                            } else if (uploadKey.contains(IMAGE_FULL)) {
                                                asyncUploadRequests.add(asyncUploadImage(upURL, artifactToPublish.getCurrentBitmap()));
                                            } else if (uploadKey.contains(IMAGE_THUMB)) {
                                                asyncUploadRequests.add(asyncUploadImage(upURL, artifactToPublish.getThumbnailBitmap()));
                                            }
                                        }
                                        //we now have a bunch of requests to make
                                        return Task.whenAll(asyncUploadRequests);
                                    }

                                    //then after error check we grab the response and then upload to amazon s3
                                    return null;
                                }
                            })
                                    .continueWith(ConfirmUpload(mContext, pr, artifactToPublish))
                                    .continueWith(new Continuation<Confirmation, Boolean>() {
                                        @Override
                                        public Boolean then(Task<Confirmation> task) throws Exception {

                                            if (task.getResult() != null) {
                                                if (task.getResult().uuid.equals(pr.uuid)) {
                                                    Log.d("WINAPIMANAGER", "Successful upload!");
                                                }
                                                return true;
                                            } else {//otherwise, remove the wid -- we didn't succeed -- we need to be careful
                                                confirmedUploads.remove(artifactToPublish.getUniqueID());
                                                //throw new Error("Publish failed.");
                                                return false;
                                            }
                                        }
                                    });
                        } else {
                            //otherwise, we can skip ahead, the upload has been done -- and we know it
                            return Task.callInBackground(new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    return null;
                                }
                            })
                                    .continueWith(ConfirmUpload(mContext, checkResponse, artifactToPublish))
                                    .continueWith(new Continuation<Confirmation, Boolean>() {
                                        @Override
                                        public Boolean then(Task<Confirmation> task) throws Exception {

                                            if (task.getResult() != null) {
                                                if (task.getResult().uuid.equals(checkResponse.uuid)) {
                                                    Log.d("WINAPIMANAGER", "Successful upload!");
                                                }
                                                return true;
                                            } else {//otherwise, remove the wid -- we didn't succeed -- we need to be careful
                                                confirmedUploads.remove(artifactToPublish.getUniqueID());
//                                                throw new Error("Publish failed.");
                                                return false;
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

//    public Task<ArrayList<FeedItem>> asyncGetLatestFeed(int count, long time)
//    {
//        String tString = null;
//        if(time > 0)
//            tString = "" + time;
//
//        return asyncGetLatestFeed(count, tString);
//    }
    public Task<ArrayList<FeedItem>> asyncGetLatestFeed(int count)
    {
        final Task<ArrayList<FeedItem>>.TaskCompletionSource tcs = Task.create();


        feedAPI.asyncGetLatest(count, new Callback<ArrayList<FeedItem>>() {
            @Override
            public void success(ArrayList<FeedItem> feedItems, Response response) {

                if(response.getStatus() == 200)
                    tcs.setResult(feedItems);
                else
                    tcs.setError(new Exception("Status: " + response.getStatus() + " r: " + response.getReason()));
            }

            @Override
            public void failure(RetrofitError error) {
                tcs.setError(error);
            }
        });

        return tcs.getTask();
    }
    public Task<ArrayList<FeedItem>> asyncGetLatestFeedAfter(int count, long lastTime) {
        String tString = null;
        if(lastTime != -1)
            tString = "" + lastTime;
        return asyncGetLatestFeedAfter(count, tString);
    }
    public Task<ArrayList<FeedItem>> asyncGetLatestFeedAfter(int count, String lastTime)
    {
        final Task<ArrayList<FeedItem>>.TaskCompletionSource tcs = Task.create();

        feedAPI.asyncGetLatest(lastTime, count, new Callback<ArrayList<FeedItem>>() {
            @Override
            public void success(ArrayList<FeedItem> feedItems, Response response) {

                if(response.getStatus() == 200)
                    tcs.setResult(feedItems);
                else
                    tcs.setError(new Exception("Status: " + response.getStatus() + " r: " + response.getReason()));
            }

            @Override
            public void failure(RetrofitError error) {
                tcs.setError(error);
            }
        });

        return tcs.getTask();
    }
    String lastSearch;
    public String lastSearchString()
    {
        return lastSearch;
    }
    public Task<ArrayList<FeedItem>> asyncGetLatestByHashtagAfter(String hashtag, int count, long lastTime) {

        String tString = null;
        if(lastTime != -1)
            tString = "" + lastTime;
        return asyncGetLatestByHashtagAfter(hashtag, count, tString);
    }
    public Task<ArrayList<FeedItem>> asyncGetLatestByHashtagAfter(String hashtag, int count, String lastTime)
    {
        lastSearch = hashtag.replace("#", "");
        final Task<ArrayList<FeedItem>>.TaskCompletionSource tcs = Task.create();

        feedAPI.asyncGetLatestByHashtag(hashtag, lastTime, count, new Callback<ArrayList<FeedItem>>() {
            @Override
            public void success(ArrayList<FeedItem> feedItems, Response response) {

                if(response.getStatus() == 200)
                    tcs.setResult(feedItems);
                else
                    tcs.setError(new Exception("Status: " + response.getStatus() + " r: " + response.getReason()));
            }

            @Override
            public void failure(RetrofitError error) {
                tcs.setError(error);
            }
        });

        return tcs.getTask();
    }

    public Task<ArrayList<FilterArtifact>> asyncGetLatestArtifacts(final String lastTime, final int count)
    {
        return Task.callInBackground(new Callable<ArrayList<FilterArtifact>>() {
            @Override
            public ArrayList<FilterArtifact> call() throws Exception {
                return discoveryAPI.syncGetLatestArtifacts(lastTime, count);
            }
        });
    }
    public Task<ArrayList<FilterArtifact>> asyncGetHashtagArtifacts(final String hashtag, final String lastTime, final int count)
    {
        return Task.callInBackground(new Callable<ArrayList<FilterArtifact>>() {
            @Override
            public ArrayList<FilterArtifact> call() throws Exception {
                return discoveryAPI.syncGetHashtagArtifacts(hashtag, lastTime, count);
            }
        });
    }
    public Task<ArrayList<FilterArtifact>> asyncGetPopularArtifacts(final int skipCount, final int count)
    {
        return Task.callInBackground(new Callable<ArrayList<FilterArtifact>>() {
            @Override
            public ArrayList<FilterArtifact> call() throws Exception {
                return discoveryAPI.syncGetPopularArtifacts(skipCount, count);
            }
        });
    }

    public Task<ArrayList<FilterArtifact>> asyncGetFavoriteArtifacts(final String username, final int skipCount, final int count)
    {
        return Task.callInBackground(new Callable<ArrayList<FilterArtifact>>() {
            @Override
            public ArrayList<FilterArtifact> call() throws Exception {
                return discoveryAPI.syncGetFavoriteArtifacts(username, skipCount, count);
            }
        });
    }






//    //WIN API Mananger handles talking to the server to negotiate publishing and what have you
//    private void createAPIAdapter(Context context)
//    {
//        if(restAdapter != null)
//            return;
//
//        restAdapter = new RestAdapter.Builder()
//                .setEndpoint(context.getResources().getString(R.string.app_server_endpoint))
//                .build();
//    }
//
//    //only create one across the app, it doesn't need to be duplicated -- the calls all'
//    //come from the same place
//    public PublishAPI createPublishAPI(Context context) throws Exception {
//        if(restAdapter == null)
//            createAPIAdapter(context);
//
//        if(publishAPI == null)
//            publishAPI = restAdapter.create(PublishAPI.class);
//
//        return publishAPI;
//    }
}
