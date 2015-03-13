package edu.eplex.androidsocialclient.Utilities;

import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;

import dagger.ObjectGraph;
import edu.eplex.androidsocialclient.MainUI.FilterAsyncLocalIECModule;
import edu.eplex.AsyncEvolution.cache.implementations.EvolutionBitmapManager;
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import edu.eplex.androidsocialclient.Login.LoginFragment;
import edu.eplex.androidsocialclient.Login.RegisterFragment;
import edu.eplex.androidsocialclient.MainUI.IECFilters;
import edu.eplex.androidsocialclient.MainUI.TakePictureFragment;
import edu.eplex.androidsocialclient.MainUI.UserSettingsFragment;
import edu.eplex.androidsocialclient.R;
import eplex.win.FastNEATJava.utils.NeatParameters;

/**
 * Created by paul on 12/9/14.
 */
public class FragmentFlowManager {

    private static FragmentFlowManager instance = null;
    protected FragmentFlowManager() {
        // Exists only to defeat instantiation.
    }

    public static FragmentFlowManager getInstance() {
        if(instance == null) {
            instance = new FragmentFlowManager();
        }
        return instance;
    }

    public void returnToLoginScreen(FragmentActivity activity)
    {
        //TODO: figure out login/main activity issues
        //we need to drop back to whatever activity is holding this
        //it's not really a fragment issue, it's an activity level issue

        //importantly, we cannot go back to anything else in our history
        //we are at the login, there is no back -- just out of the app
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        //for now, we just replace the current fragment with a login screen
        //add login fragment to the stack
        //do not add to back state
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new LoginFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

    }

    public void homeSignupFacebookOrEmail(FragmentActivity activity)
    {
        //transfer
        //create a registration fragment -- handles signing up with e-mail
        //or alternatively, signing up once logged in with facebook
        RegisterFragment rf = new RegisterFragment();

        //add register fragment to the stack
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, rf)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    public void tempLaunchIEC(FragmentActivity activity, Bitmap squareSelected)
    {
        //importantly, we cannot go back to anything else in our history
        //we are at the user settings and logged in, there is no back -- just out of the app
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);


        IECFilters iec = new IECFilters();



        //set the evo cache location in our params
        JsonNode params = iec.DefaultParams(activity);
        ObjectNode uiParams = (ObjectNode)params.get("ui");

        ObjectMapper mapper = new ObjectMapper();
        String imageName = activity.getResources().getString(R.string.evolution_cache_name);
        uiParams.set("image", mapper.convertValue(imageName, JsonNode.class));

        //now save the square version of the bitmap -- at some point we will ask the user to do this
//        Bitmap squareSelected = squareBitmap(selectedImage);
        EvolutionBitmapManager.getInstance().setBitmap(imageName, squareSelected);

        //create our parameters
        NeatParameters np = NEATInitializer.DefaultNEATParameters();

        //initialize (only happens once no worries)
        NEATInitializer.InitializeActivationFunctions();

        //we need to inject our objects!
        ObjectGraph graph = ObjectGraph.create(Arrays.asList(new FilterAsyncLocalIECModule(activity, np, null)).toArray());
        iec.injectGraph(activity, graph);

        //now send in the params to start evolution and fill the screen!
//        iec.asyncInitializeIECandUI(activity, params);
        iec.InitializeParameters(params);


        //add register fragment to the stack
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, iec)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

    }



    public void launchLoginLanding(FragmentActivity activity, String username)
    {
        //importantly, we cannot go back to anything else in our history
        //we are at the user settings and logged in, there is no back -- just out of the app
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        TakePictureFragment userEntryPoint = new TakePictureFragment();
//        userEntryPoint.shouldWelcomeUsers(true);

        //add register fragment to the stack
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, userEntryPoint)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public void tempLaunchUserSettings(FragmentActivity activity, String username)
    {
        //importantly, we cannot go back to anything else in our history
        //we are at the user settings and logged in, there is no back -- just out of the app
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

//        FragmentManager fm = activity.getSupportFragmentManager();
//        for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
//            fm.popBackStack();
//        }

        UserSettingsFragment userEntryPoint = new UserSettingsFragment();
        userEntryPoint.shouldWelcomeUsers(true);

        //add register fragment to the stack
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, userEntryPoint)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        //only happens when we successfully get logged in

    }



}
