package asynchronous.implementation;

import android.os.Debug;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import api.CustomJSONMapper;
import api.HomeAPIDeserializer;
import api.HomeAPIObject;
import api.HomeAPIService;
import asynchronous.interfaces.AsyncSeedLoader;
import bolts.Continuation;
import bolts.Task;
import dagger.ObjectGraph;
import eplex.win.winBackbone.Artifact;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;
import win.eplex.backbone.NEATArtifact;

/**
 * Created by paul on 10/19/14.
 */
public class AsyncScrollHomeUI implements AsyncSeedLoader {

    public static String CAT_CHANGE = "category";

    Map<String, Integer> categoryAndStart = new HashMap<String, Integer>();
    Map<String, Integer> tempCategoryAndStart = new HashMap<String, Integer>();

    int artifactsPerRequest = 6;
    String currentCategory = "newest";

    @Inject
    HomeAPIService homeAPI;

    @Override
    public Task<List<Artifact>> asyncLoadSeeds(JsonNode params) {

        if(params.get(CAT_CHANGE) != null)
        {
            currentCategory = params.get(CAT_CHANGE).asText();
        }

        if(!categoryAndStart.containsKey(currentCategory))
            categoryAndStart.put(currentCategory, 0);

        if(!tempCategoryAndStart.containsKey(currentCategory))
            tempCategoryAndStart.put(currentCategory, 0);

        final int categoryStartIx = categoryAndStart.get(currentCategory);
        final int tempCategoryStartIx = tempCategoryAndStart.get(currentCategory);

        final int endRequestIx = categoryStartIx + artifactsPerRequest;

        //we have not yet made a request (that's currently unanswered)
        //we don't want to make duplicate requests, so we track the start/end
        if(tempCategoryStartIx <= categoryStartIx)
        {
            tempCategoryAndStart.put(currentCategory, endRequestIx);
            //everything is good to go, we can make a request
            //now we make an async request using retrofit
            //we fetch from home, then convert the response into a home object, voila!
            return fetchHomeAsync(categoryStartIx, endRequestIx).continueWithTask(new Continuation<Response, Task<List<Artifact>>>() {
                @Override
                public Task<List<Artifact>> then(Task<Response> task) throws Exception {
                    return convertJSONStream(task.getResult());
                }
            });
        }
        else
        {
            //otherwise, just return an empty array
            return Task.callInBackground(new Callable<List<Artifact>>() {
                @Override
                public List<Artifact> call() throws Exception {
                    return new ArrayList<Artifact>();
                }
            });
        }

    }

    //we are converting the HTML response into an actual object (collection of neat artifacts
    public Task<List<Artifact>> convertJSONStream(final Response response)
    {
        return Task.callInBackground(new Callable<List<Artifact>>() {
            @Override
            public List<Artifact> call() throws Exception {
                TypedInput body = response.getBody();

                ObjectMapper mapper = CustomJSONMapper.CustomWINDeserializer();

                try {
                    //convert the stream into a POJO k thx
                    HomeAPIObject homeValue = mapper.readValue(body.in(), HomeAPIObject.class);


                    List<Artifact> aList = new ArrayList<Artifact>();
                    for(int i=0; i < homeValue.neatArtifacts.size(); i++)
                    {
                        NEATArtifact na = homeValue.neatArtifacts.get(i);

                        //very important! The genome was never initialized when loaded in
                        na.genome.initializeNodeCounts();

                        //add to our list as a plain artifact
                        aList.add((Artifact)na);
                    }

                    //update our category! We've gone so far :)
                    int cStart = categoryAndStart.get(currentCategory);
                    categoryAndStart.put(currentCategory, cStart + homeValue.neatArtifacts.size());

                    return aList;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.d("Home", "Fail: " + e.getMessage());
                    return null;
                }
            }
        });
    }

    //make async call to the internet
    public Task<Response> fetchHomeAsync(final int startIx, final int endIx) {
        final Task<Response>.TaskCompletionSource task = Task.<Response> create();

        //call our API object
        homeAPI.asyncGetHomeArtifacts(startIx, endIx, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                task.setResult(response);
            }

            @Override
            public void failure(RetrofitError error) {
                task.setError(error);
            }
        });

        return task.getTask();
    }


}
