package edu.eplex.androidsocialclient.MainUI.Main.Publish;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import dagger.ObjectGraph;
import edu.eplex.AsyncEvolution.cardUI.EndlessGridScrollListener;
import edu.eplex.AsyncEvolution.views.HorizontalListView;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.MainUI.Adapters.EditIECCompositeAdapter;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.R;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 3/16/15.
 */
public class PublishFragment extends Fragment {

    private static final String HASHTAG_PATTERN = "(#[\\p{L}0-9-_]+)";
    private static final String MENTION_PATTERN = "(@[\\p{L}0-9-_]+)";
    private static Pattern hashtagMatcher = Pattern.compile("[#]+[A-Za-z0-9-_]+\\b");

    //    public class HashtagsSpanRenderer implements AwesomeTextHandler.ViewSpanRenderer {
//
//        private final static int textSizeInDips = 18;
//        private final static int backgroundResource = R.drawable.common_hashtags_background;
//        private final static int textColorResource = android.R.color.white;
//
//        @Override
//        public View getView(String text, Context context) {
//            TextView view = new TextView(context);
//            view.setText(text.substring(1));
//            view.setTextSize(dipsToPixels(context, textSizeInDips));
//            view.setBackgroundResource(backgroundResource);
//            int textColor = context.getResources().getColor(textColorResource);
//            view.setTextColor(textColor);
//            return view;
//        }
//
//    }
    public static int dipsToPixels(Context ctx, float dips) {
        final float scale = ctx.getResources().getDisplayMetrics().density;
        int px = (int) (dips * scale + 0.5f);
        return px;
    }
    //need to handle evolution and our evolution views

    @InjectView(R.id.pub_filter_main_image_view)
    public ImageView filterImage;

    @InjectView(R.id.pub_filter_hashtag_edit_text)
    public EditText hashtagText;

    @InjectView(R.id.pub_filter_publish_button)
    public Button publishButton;

    boolean initialized = false;
    boolean touchImage = false;

    FilterComposite toPublishFilter;

    int mainImageDesiredWidthHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.app_publish_main_ui, container, false);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        //Register for events from the edit flow manager please -- we should keep in touch
        //you know, as friends or whatever
        //with event benefits
        //just thinking out loud
        EditFlowManager.getInstance().registerUIEvents(this);

        //initilaize!
        initializeUI(getActivity());
        setSelectedFilterAsMainImage(FilterManager.getInstance().getLastEditedFilter());

        return rootView;
    }

    ArrayList<String> hashtagsFromText(String text)
    {
        ArrayList<String> hts = new ArrayList<>();

        Matcher matcher = hashtagMatcher.matcher(text);
        int matchCount = 0;
        while (matcher.find()) {
            hts.add(matcher.group(0));
            matchCount++;
        }
        if(matchCount ==0)return null;
        else
            return hts;
    }

    boolean publishInProgress = false;

    @OnClick(R.id.pub_filter_publish_button)
    void publishClick()
    {
        if(publishInProgress)
            return;



        publishInProgress = true;

        FilterArtifact fa = toPublishFilter.getFilterArtifact();

        String caption =  hashtagText.getText().toString();
        fa.hashtags = hashtagsFromText(caption);
        if(fa.hashtags == null)
        {
            //not allowed to publish without captions
            warnUserMissingHashtags();

            return;
        }

        fa.photoCaption = caption;


        WinAPIManager.getInstance().asyncPublishArtifact(getActivity(), toPublishFilter)
            .continueWith(new Continuation<Boolean, Void>() {
                @Override
                public Void then(Task<Boolean> task) throws Exception {

                    publishInProgress = false;

                    Log.d("PUBFRAG", "Returned from publish: " + task.isCompleted());

                   if(task.isCancelled() || task.isFaulted())
                    {
                        //failed!
                        failedToPublishImageError();
                    }
                    else if(task.isCompleted() && task.getResult())
                    {
                        PublishFlowManager.getInstance().finishPublishArtifactActivity(getActivity(), toPublishFilter);
                    }
                    else {
//                        Toast.makeText(getActivity(), "Publish successful: " + toPublishFilter.getUniqueID(), Toast.LENGTH_SHORT).show();
                        //lets close this place up!
                        failedToPublishImageError();
                        //this should finish the publishing process -- we need to be very concerned about the published image --
                        //cause it needs to be removed
//                        PublishFlowManager.getInstance().finishPublishArtifactActivity(getActivity(), toPublishFilter);
                    }

                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);

    }
    //here we warn the user before loggin off
    void failedToPublishImageError()
    {
        new MaterialDialog.Builder(getActivity())
                .title("Error Publishing" )
                .content(Html.fromHtml("<br/>Error publishing photo. Try again, sorry about that!<br/>"))
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
    //here we warn the user before loggin off
    void warnUserMissingHashtags()
    {
        new MaterialDialog.Builder(getActivity())
                .title("Missing #hashtags" )
                .content(Html.fromHtml("<br/>Please include at least one #hashtag in your caption, for science.<br/>"))
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void initializeUI(FragmentActivity activity)
    {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

//        mainImageDesiredWidthHeight = Math.min(width,height);
        mainImageDesiredWidthHeight = activity.getResources().getInteger(R.integer.max_filtered_image_size);//Math.min(width,height);

//        int screenMatch = Math.min(width, height);

//        filterImage.getLayoutParams().width = mainImageDesiredWidthHeight;
//        filterImage.getLayoutParams().height = mainImageDesiredWidthHeight;
        filterImage.setImageResource(R.drawable.ic_action_emo_tongue_white);
        filterImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        Typeface typeFace= Typeface.createFromAsset(getActivity().getAssets(), "fonts/android.ttf");
        publishButton.setTypeface(typeFace);

//        TextDrawable drawable = TextDrawable.builder()
//                .beginConfig()
//                .withBorder(4) /* thickness in px */
//                .endConfig()
//                .buildRoundRect("A", Color.RED, 10);

//        final AwesomeTextHandler awesomeEditTextHandler = new AwesomeTextHandler();
//        awesomeEditTextHandler
//                .addViewSpanRenderer(HASHTAG_PATTERN, new HashtagsSpanRenderer())
//                .setView(hashtagText);

//        hashtagText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });

    }


    void setSelectedFilterAsMainImage(FilterComposite filterComposite) {
        //now we have to install in the main image!
        if(filterComposite == toPublishFilter)
            return;

        toPublishFilter = filterComposite;

        try {
            //need to lazy load in the main image -- then async run the filter plz
            EditFlowManager.getInstance().lazyLoadFilterIntoImageView(getActivity(), filterComposite, mainImageDesiredWidthHeight, mainImageDesiredWidthHeight, false, filterImage);
        }
        catch (Exception e)
        {
            Toast.makeText(getActivity(), "Error loading main image file.", Toast.LENGTH_SHORT).show();
        }
    }



}
