package edu.eplex.androidsocialclient.MainUI.API;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

//import com.squareup.okhttp.MediaType;
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;
//import com.squareup.okhttp.RequestBody;
//import com.squareup.okhttp.Response;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import bolts.Continuation;
import bolts.Task;
import dagger.ObjectGraph;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.Confirmation;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.UploadURL;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishRequest;
import edu.eplex.androidsocialclient.MainUI.API.Publish.PublishResponse;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import retrofit.RestAdapter;
import retrofit.client.Response;

/**
 * Created by paul on 3/19/15.
 */
public class WinAPIManager {

//    public static final MediaType JSON
//            = MediaType.parse("application/json; charset=utf-8");
//
//    public static final MediaType PNG
//            = MediaType.parse("image/png");
//
//    public static final MediaType PLAINTEXT
//            = MediaType.parse("text/plain");

    static String FILTER_THUMB = "filter-thumbnail";
    static String FILTER_FULL = "filter-full";
    static String IMAGE_THUMB = "image-thumbnail";
    static String IMAGE_FULL = "image-full";


    @Inject
    RestAdapter restAdapter;

    @Inject
    PublishAPI publishAPI;

    @Inject
    S3UploadAPI s3UploadAPI;

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

    Task<Response> asyncUploadImage(final UploadURL url, final Bitmap image)
    {
        return Task.callInBackground(new Callable<ByteArrayOutputStream>() {
            @Override
            public ByteArrayOutputStream call() throws Exception {
                return encodeToPNG(image);
            }
        }).continueWith(new Continuation<ByteArrayOutputStream, Response>() {
            @Override
            public Response then(Task<ByteArrayOutputStream> task) throws Exception {

                if(task.getResult() != null)
                {
                    //we need to pull the info from the url
                    String s3Key = url.request.Key;

                    String[] urlParams = s3Key.split("\\/");

                    String[] qSplit = url.url.split("\\?");

                    String[] signature = qSplit[1].split("\\&");

                    return s3UploadAPI.syncS3Upload(task.getResult().toByteArray(),
                            urlParams[0],
                            urlParams[1],
                            urlParams[2],
                            signature[0],
                            Long.parseLong(signature[1]),
                            signature[2]);


//                    return null;// post(url, task.getResult().toByteArray());

                }
                //now we must upload to the url provided
                return null;
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

    public Task<Void> asyncPublishArtifact(final FilterComposite artifactToPublish)
    {
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

                    ArrayList<Task<Response>> asyncUploadRequests = new ArrayList<Task<Response>>();

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
        }).continueWith(new Continuation<Void, Confirmation>() {
            @Override
            public Confirmation then(Task<Void> task) throws Exception {

                //if we made it this far, we've uploaded images
                if (!task.isCancelled() || !task.isFaulted()) {
                    //wow, lets go ahead and confirm, and send our filter artifact while we're at it too!
                    pr.accessToken = "bobsyourunclefool";
                    pr.filterArtifacts = new HashMap<String, FilterArtifact>();
                    FilterArtifact fa = artifactToPublish.getFilterArtifact();
                    pr.filterArtifacts.put(fa.wid(), fa);
                    return publishAPI.syncConfirmUpload(pr);
                }

                return null;
            }
        }).continueWith(new Continuation<Confirmation, Void>() {
            @Override
            public Void then(Task<Confirmation> task) throws Exception {

                if(task.getResult() != null)
                {
                    if(task.getResult().uuid.equals(pr.uuid))
                    {
                        Log.d("WINAPIMANAGER", "Successful upload!");
                    }
                }

                return null;
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
