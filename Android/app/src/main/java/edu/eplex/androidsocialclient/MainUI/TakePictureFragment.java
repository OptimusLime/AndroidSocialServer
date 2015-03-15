package edu.eplex.androidsocialclient.MainUI;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import edu.eplex.androidsocialclient.API.Manager.UserSessionManager;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.FileUtilities;
import edu.eplex.androidsocialclient.Utilities.FragmentFlowManager;
import edu.eplex.androidsocialclient.GPU.NEATTestGPUShader;

/**
 * Created by paul on 2/5/15.
 */
public class TakePictureFragment extends Fragment {

    //Tag for logging
    private static final String TAG = "TakePictureFragment";

    //This really could be any value - what's important is that it's passed with the creation of the
    //camera intent -- thus when the result comes back, we know what action we told the intent to take
    //This would come in handy if we also took video as well -- we could differentiate between the
    //two types of intents we launched
    final static int CAPTURE_IMAGE_ACTION = 1;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    ImageView mImagePreview;
    Bitmap mSampleBitmap;
    Bitmap fullImageBitmap;

    String mCurrentImageSaveLocation;

    //Handle opening an image in the gallery
    String mMostRecentImageLocation;
    String mFileType = "image/*";

    private MediaScannerConnection mScannerConnection;

    /* Photo album for this application */
    private String getPhotoAlbumName() {
        return getString(R.string.image_album_name);
    }

    private boolean userFromCache = false;
    private boolean welcomeUser = false;
    private String currentUser;
    public void shouldWelcomeUsers(boolean welcomeUser)
    {
        this.welcomeUser = welcomeUser;
    }
    void welcomeUserCrouton(String username)
    {
        if(!welcomeUser && !userFromCache)
            return;

        if(userFromCache)
            Crouton.makeText(getActivity(),
                    getActivity().getResources().getString(R.string.user_back_message) + " " + username,
                    Style.INFO,
                    R.id.crouton_handle).show();
        else
            Crouton.makeText(getActivity(),
                    getActivity().getResources().getString(R.string.user_login_message) + " " + username,
                    Style.INFO,
                    R.id.crouton_handle).show();

//        (RelativeLayout)getActivity().findViewById(R.id.crouton_toolbar_layout)).show();

        //no more of this thank you!
        welcomeUser = false;
        userFromCache = false;
    }

    //This is the menu options handling
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu_account_settings, menu);

        //got our current user -- welcome if we should
        welcomeUserCrouton(currentUser);

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //root view -- camera fragment
        View rootView = inflater.inflate(R.layout.fragment_camera_home, container, false);

        //set our content as the camera layout
//        setContentView(R.layout.fragment_camera_home);

        //we store a reference to our image preview -- so we may verify the intent returned with a picture!
        mImagePreview = (ImageView) rootView.findViewById(R.id.camera_image_preview);

        //We must grab our image button so we can attach a click listener -- to launch the camera intent
        ImageButton imgButton = (ImageButton) rootView.findViewById(R.id.launch_camera_button);


        //we wire our image button to launch
        imgButton.setOnClickListener(mCameraCaptureCallback);

        //now we grab our IEC button for launching IEC
        ImageButton galButton = (ImageButton) rootView.findViewById(R.id.launch_iec_button);
//
//        //this will trigger our open gallery callback, which handles launching the gallery intent
        galButton.setOnClickListener(mOpenIECCallback);

        //Now we grab our gallery button, and we attach a callback to it
//        ImageButton galButton = (ImageButton) rootView.findViewById(R.id.launch_gallery_button);
//
//        //this will trigger our open gallery callback, which handles launching the gallery intent
//        galButton.setOnClickListener(mOpenGalleryCallback);

        //make sure to set up our path for scanning on gallery open
        grabMostRecentPhoto();

        //register our toolbar with the activity -- so we get callbacks and stuff
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.cameraHomeToolbar);
        ActionBarActivity abActivity = ((ActionBarActivity)getActivity());
        abActivity.setSupportActionBar(toolbar);
//        abActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        abActivity.getSupportActionBar().setTitle(getResources().getString(R.string.ch_toolbar_title));

        //confirm we want options callback
        setHasOptionsMenu(true);

        UserSessionManager.getInstance().register(this, this);

        //return the inflated root view
        return rootView;
    }

    void grabMostRecentPhoto()
    {
        //Get our storage directory
        File folder = FileUtilities.getStorageDirectoryFromName(getPhotoAlbumName());

        //wasn't created yet -- folder is null!
        if(folder == null)
            return;

        //list all the files contained in the directory (if any)
        File[] allFiles = folder.listFiles();

        //if we don't have any files in the folder, we're done -- nothing to do
        if(allFiles == null || allFiles.length == 0)
            return;

        //if we have any files, we sort according to last modified date (descending)
        Arrays.sort(allFiles, new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                return (int) (file2.lastModified() - file.lastModified());
            }
        });

        //we set our most recent image location to be the last modified image
        mMostRecentImageLocation = allFiles[0].getAbsolutePath();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();

        //Make sure the status bar is hidden on app launch or resume
//        hideStatusBar();
    }

    //A function to hide the status bar according to different Android versions
    //Pre-API 16 and Post-API 16 requires two separate system calls
    private void hideStatusBar()
    {
        //Stackoverflow: http://stackoverflow.com/questions/19904166/hide-status-bar-android
        if (Build.VERSION.SDK_INT < 16) { //Previous API method
            getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else { // Jellybean and up, new hide function calls
            View decorView = getActivity().getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    //Our image button listener -- simply calls dispatch with a given actionCode (capture image)
    ImageButton.OnClickListener mCameraCaptureCallback =
            new ImageButton.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(CAPTURE_IMAGE_ACTION);
                }
            };

    ImageButton.OnClickListener mOpenIECCallback =
            new ImageButton.OnClickListener(){
                @Override
                public void onClick(View v) {
                    openIECFragment();
                }
            };
    ImageButton.OnClickListener mOpenGalleryCallback = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //time to open the gallery!
            dispatchOpenGalleryIntent();
        }
    };

    static Bitmap squareBitmap(Bitmap source)
    {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap result = Bitmap.createBitmap(source, x, y, size, size);
        if (result != source) {
            source.recycle();
        }
        return result;
    }

    private void openIECFragment()
    {
        //we're ready to open our IEC interface ONLY IF we have an image selected
        if(fullImageBitmap == null)
        {
            notifyUserMissingPicture();
            return;
        }

        //otherwise, we're ready to IEC!
        FragmentFlowManager.getInstance().tempLaunchIEC(getActivity(), squareBitmap(fullImageBitmap), squareBitmap(mSampleBitmap));
    }
    void notifyUserMissingPicture()
    {
        new MaterialDialog.Builder(getActivity())
                .title("Missing Image")
                .content(Html.fromHtml("<br/>Please select an image before continuing...<br/>"))
                .titleAlignment(Alignment.CENTER)
                .contentAlignment(Alignment.CENTER)
                .positiveText("Okay")
                .callback(new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        //easy, just hide ourselves!
                        materialDialog.hide();
                    }
                })
                .show();
    }


    //Here we handle opening of the gallery to a specific folder
    private void dispatchOpenGalleryIntent() {

        //Create an intent that will request some app handle viewing this file
        Intent intent = new Intent(Intent.ACTION_VIEW);

        //We use the most recent image, and open the gallery to that image
        //we also specify the file type to be an image
        intent.setDataAndType(Uri.fromFile(new File(mMostRecentImageLocation)), mFileType);

        //we launch the intent -- the user will select which app to handle this action
        startActivity(intent);
    }

    //dispatch handles sending out our photo request
    private void dispatchTakePictureIntent(int actionCode) {

        //We create an intent to capture an image from the capture device
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //We must first create our photo file
        File imageFileHolder;

        try {

            //file utilities creates our temp file location
            imageFileHolder = FileUtilities.createImageFile(getPhotoAlbumName(), JPEG_FILE_PREFIX, JPEG_FILE_SUFFIX);

            //We record where we're attemptign to save our image
            mCurrentImageSaveLocation = imageFileHolder.getAbsolutePath();

            //now we inform the action_image_capture intent to store the result in the provided file location
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFileHolder));


        } catch (IOException e) {

            //anything goes wrong and we'll be forced to abort
            e.printStackTrace();
            imageFileHolder = null;
            mCurrentImageSaveLocation = null;
        }

        //we then launch the intent from this activity
        startActivityForResult(takePictureIntent, actionCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //http://stackoverflow.com/questions/17768145/what-data-is-returned-when-using-action-image-capture
        if(requestCode == CAPTURE_IMAGE_ACTION && resultCode == Activity.RESULT_OK) {

            //The Intent was sent with a URI now,
            //Therefore, we must load our bitmap from the proper file location

            //we set a integer resource defining the desired max size of the preview window
            //this is only for debuggin purposes, so adjusting this number is not a big concern
            int previewWidthHeight = getResources().getInteger(R.integer.default_preview_square_size);

            int maxImageWidthHeight = getResources().getInteger(R.integer.default_max_image_square_size);

            //grab the full image please
//            fullImageBitmap = BitmapFactory.decodeFile(mCurrentImageSaveLocation);

            fullImageBitmap = FileUtilities.decodeSampledBitmapFromResource(mCurrentImageSaveLocation,
                    maxImageWidthHeight,
                    maxImageWidthHeight);


            //we decode using file utilities and our desired width/height
            Bitmap decodedBitmap = FileUtilities.decodeSampledBitmapFromResource(mCurrentImageSaveLocation,
                    previewWidthHeight,
                    previewWidthHeight);

            //Set the bitmap inside our previews
            mImagePreview.setImageBitmap(decodedBitmap);

            //save the bitamp
            mSampleBitmap = decodedBitmap;

            //we keep track of the most recently updated image location -- for opening gallery
            mMostRecentImageLocation = mCurrentImageSaveLocation;

            //DEBUG
            //TEST AWAY
//            NEATTestGPUShader.TestShader(getActivity(), mSampleBitmap);

            //we must alert the system that an image has been added
            handlePhotoTaken();
        }
    }

    private void handlePhotoTaken() {

        if (mCurrentImageSaveLocation != null) {
            updateGalleryIntent();
            mCurrentImageSaveLocation = null;
        }
    }

    //this function broadcasts a signal that lets Android know is should scan for our new image file
    private void updateGalleryIntent() {

        //we intend on updated Android about our new image
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");

        //first we create a file object representing the absolute path of the image
        File f = new File(mCurrentImageSaveLocation);

        //we get our URI from the file location
        Uri contentUri = Uri.fromFile(f);

        //let the location of the image file be known
        mediaScanIntent.setData(contentUri);

        //we broadcase this intent to Android
        getActivity().sendBroadcast(mediaScanIntent);
    }




}
