package edu.eplex.androidsocialclient.MainUI.Main.Tabs;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.lang.reflect.Method;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.Constants;
import edu.eplex.androidsocialclient.Utilities.UriToUrl;

/**
 * Created by paul on 3/15/15.
 */
public class SelectPictureFragment extends Fragment {

    public static final TabFlowManager.TabID TAB = TabFlowManager.TabID.Camera;

    private Animation animation;

    @InjectView(R.id.top_holder)
    public RelativeLayout top_holder;

    @InjectView(R.id.bottom_holder)
    public RelativeLayout bottom_holder;

    @InjectView(R.id.step_number)
    public RelativeLayout step_number;

    private Uri imageUri;
    private boolean click_status = true;

    private boolean flyingIn = false;

    //take a picture
    @OnClick(R.id.top_holder) void cameraClick() {
        startCamera(null);
    }
    //start the gallery
    @OnClick(R.id.bottom_holder) void galleryClick()
    {
        startGallery(null);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.app_fragment_take_picture, container, false);
        ButterKnife.inject(this, rootView);
        TabFlowManager.getInstance().registerUIEvents(this);


        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        ButterKnife.inject(getActivity());
    }

    @Override
    public void onStart() {
        getActivity().overridePendingTransition(0, 0);
        flyIn(null);
        super.onStart();
    }

    @Override
    public void onStop() {
        getActivity().overridePendingTransition(0, 0);
        super.onStop();
    }

    public void startGallery(View view) {
        flyOut("displayGallery");
    }

    public void startCamera(View view) {
        flyOut("displayCamera");
    }

    @SuppressWarnings("unused")
    private void displayGallery() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !Environment.getExternalStorageState().equals(Environment.MEDIA_CHECKING)) {
            Intent intent = new Intent();
            intent.setType("image/jpeg");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, Constants.REQUEST_GALLERY);
        } else {
            Toast.makeText(getActivity(), R.string.no_media, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CAMERA) {
            try{
                if (resultCode == FragmentActivity.RESULT_OK) {
                    displayPhotoActivity(1);
                } else {
                    UriToUrl.deleteUri(getActivity(), imageUri);
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.error_img_not_found, Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == FragmentActivity.RESULT_OK && requestCode == Constants.REQUEST_GALLERY) {
            try {
                imageUri = data.getData();
                displayPhotoActivity(2);
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.error_img_not_found, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressWarnings("unused")
    private void displayCamera() {
        imageUri = getOutputMediaFile();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, Constants.REQUEST_CAMERA);
    }

    private Uri getOutputMediaFile(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Camera Pro");
        values.put(MediaStore.Images.Media.DESCRIPTION, "www.appsroid.org");
        return getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void displayPhotoActivity(int source_id) throws Exception {

        //create our new composite -- then peace outta here -- we der dunnn yo'
//        for(int i=0; i < 10; i++)
        FilterManager.getInstance().createNewComposite(getActivity(), UriToUrl.get(getActivity(), imageUri));

        switchTabs();
        //fly out and away!
//        flyIn("switchTabs");


//        Intent intent = new Intent(getActivity(), PhotoActivity.class);
//        intent.putExtra(Constants.EXTRA_KEY_IMAGE_SOURCE, source_id);
//        intent.setData(imageUri);
//        startActivity(intent);
//        getActivity().overridePendingTransition(0, 0);
//        finish();
    }

    public void switchTabs() throws Exception
    {
        //need to do something with the uri for the image -- but nothing for now
        TabFlowManager.getInstance().switchToTab(TAB, TabFlowManager.TabID.Workshop);
    }

    private void flyOut(final String method_name) {
        if (click_status) {
            click_status = false;

            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.step_number_back);
            step_number.startAnimation(animation);

            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.holder_top_back);
            top_holder.startAnimation(animation);

            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.holder_bottom_back);
            bottom_holder.startAnimation(animation);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                }

                @Override
                public void onAnimationEnd(Animation arg0) {
                    callMethod(method_name);
                }
            });
        }
    }

    private void callMethod(String method_name) {
        if (method_name.equals("finish")) {
            getActivity().overridePendingTransition(0, 0);
//            finish();
        } else {
            try {
                Method method = getClass().getDeclaredMethod(method_name);
                method.invoke(this, new Object[] {});
            } catch (Exception e) {}
        }
    }


//    @Override
//    public void onBackPressed() {
//        flyOut("finish");
//        super.onBackPressed();
//    }

    private void flyIn(final String method_name) {

        if(!flyingIn){

            flyingIn = true;
            click_status = true;

            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.holder_top);
            top_holder.startAnimation(animation);

            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.holder_bottom);
            bottom_holder.startAnimation(animation);

            animation = AnimationUtils.loadAnimation(getActivity(), R.anim.step_number);
            step_number.startAnimation(animation);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                }

                @Override
                public void onAnimationEnd(Animation arg0) {
                    flyingIn = false;

                    if (method_name != null && method_name != "") {
                        callMethod(method_name);
                    }
                }
            });
        }
    }
}
