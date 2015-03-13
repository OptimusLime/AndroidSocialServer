/*
 * ******************************************************************************
 *   Copyright (c) 2013-2014 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package edu.eplex.AsyncEvolution.cardUI.cards;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import edu.eplex.AsyncEvolution.R;

/**
 * Simple colored card
 *
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class StickyHeaderCard extends Card {

    protected String mTitle;
    protected int count;

    TextView mTitleView;
    LinearLayout mImageLayout;

    public StickyHeaderCard(Context context) {
        this(context, R.layout.sticky_card_inner_base);
    }

    public StickyHeaderCard(Context context, int innerLayout) {
        super(context, innerLayout);
        init();
    }

    private void init() {

        //Add ClickListener
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                Toast.makeText(getContext(), "Click Listener card=" + count, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {

        //Retrieve elements
        TextView title = (TextView) parent.findViewById(R.id.sticky_card_color_inner_simple_title);

        if (title != null)
            title.setText(mTitle);

        mTitleView = title;

        mImageLayout = (LinearLayout)parent.findViewById(R.id.sticky_card_image_list);

    }


    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;

        if(mTitleView != null && title!=null)
            mTitleView.setText(title);
    }

    public void setParents(List<Bitmap> bitmapImages)
    {
        if(mImageLayout == null)
            return;

        //pull out the children views, we're going to replace them!
        mImageLayout.removeAllViews();

        LayoutInflater mInflator = LayoutInflater.from(getContext());

        //inflate our collection of parents please!
        for(Bitmap circleParent : bitmapImages) {
            ImageView view = (ImageView) mInflator.inflate(R.layout.stick_card_header_circle_bitmap, null);
            view.setImageBitmap(circleParent);
            mImageLayout.addView(view);
        }

    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

}
