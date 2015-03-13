package edu.eplex.AsyncEvolution.cardUI;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ListAdapter;

import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardGridCursorAdapter;
import it.gmariotti.cardslib.library.view.CardView;
import edu.eplex.AsyncEvolution.R;
import it.gmariotti.cardslib.library.view.base.CardViewWrapper;

/**
 * Created by paul on 8/19/14.
 */
public class StickyCardGridView extends StickyGridHeadersGridView implements CardView.OnExpandListAnimatorListener {

        protected static String TAG = "StickyCardGridView";

        /**
         *  Card Grid Array Adapter
         */
        protected StickyCardGridArrayAdapter mAdapter;


        //--------------------------------------------------------------------------
        // Custom Attrs
        //--------------------------------------------------------------------------

        /**
         * Default layout to apply to card
         */
        protected int list_card_layout_resourceID = R.layout.carddemo_grid_gplay;

        //--------------------------------------------------------------------------
        // Constructors
        //--------------------------------------------------------------------------


        public StickyCardGridView(Context context) {
            super(context);
            init(null, 0);
        }

        public StickyCardGridView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init(attrs, 0);
        }

        public StickyCardGridView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init(attrs, defStyle);
        }

        //--------------------------------------------------------------------------
        // Init
        //--------------------------------------------------------------------------

        /**
         * Initialize
         *
         * @param attrs
         * @param defStyle
         */
        protected void init(AttributeSet attrs, int defStyle){

            //Init attrs
            initAttrs(attrs,defStyle);

        }


        /**
         * Init custom attrs.
         *
         * @param attrs
         * @param defStyle
         */
        protected void initAttrs(AttributeSet attrs, int defStyle) {

            list_card_layout_resourceID = R.layout.carddemo_grid_gplay;

            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs, R.styleable.card_options, defStyle, defStyle);

            try {
                list_card_layout_resourceID = a.getResourceId(R.styleable.card_options_list_card_layout_resourceID, this.list_card_layout_resourceID);
            } finally {
                a.recycle();
            }
        }

        //--------------------------------------------------------------------------
        // Adapter
        //--------------------------------------------------------------------------

        /**
         * Forces to use a {@link CardGridArrayAdapter}
         *
         * @param adapter
         */
        @Override
        public void setAdapter(ListAdapter adapter) {
            if (adapter instanceof StickyCardGridArrayAdapter){
                setAdapter((StickyCardGridArrayAdapter)adapter);
            }else{
                Log.w(TAG, "You are using a generic adapter. Pay attention: your adapter has to call cardGridArrayAdapter#getView method.");
                super.setAdapter(adapter);
            }
        }

    /**
     * Set {@link it.gmariotti.cardslib.library.internal.CardArrayAdapter} and layout used by items in ListView
     *
     * @param adapter {@link it.gmariotti.cardslib.library.internal.CardArrayAdapter}
     */
    public void setAdapter(StickyCardGridArrayAdapter adapter) {
        super.setAdapter(adapter);

        //Set Layout used by items
        adapter.setRowLayoutId(list_card_layout_resourceID);
        adapter.setCardExpandListener(this);
        mAdapter = adapter;
    }

        /**
         * You can use this method, if you are using external adapters.
         * Pay attention. The generic adapter#getView() method has to call the cardArrayAdapter#getView() method to work.
         *
         * @param adapter {@link ListAdapter} generic adapter
         * @param cardGridArrayAdapter    {@link it.gmariotti.cardslib.library.internal.CardGridArrayAdapter} cardGridArrayAdapter
         */
        public void setExternalAdapter(ListAdapter adapter, StickyCardGridArrayAdapter cardGridArrayAdapter) {

            setAdapter(adapter);

            mAdapter=cardGridArrayAdapter;
            mAdapter.setRowLayoutId(list_card_layout_resourceID);
            mAdapter.setCardExpandListener(this);
        }

        //--------------------------------------------------------------------------
        // Expand and Collapse animator
        // Don't use this animator in a grid.
        // All cells in the same row should expand/collapse a hidden area of same dimensions.
        //--------------------------------------------------------------------------


        @Override
        public void onCollapseStart(CardViewWrapper cardViewWrapper, View view) {
            //do nothing. Don't use this kind of animation in a grid
            Log.w(TAG,"Don't use this kind of animation in a grid");
        }

        @Override
        public void onExpandStart(CardViewWrapper cardViewWrapper, View view) {
            //do nothing. Don't use this kind of animation in a grid
            Log.w(TAG,"Don't use this kind of animation in a grid");
        }




    @Override
    protected void dispatchDraw(Canvas canvas) {
        try {
            super.dispatchDraw(canvas);
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
    }
}
