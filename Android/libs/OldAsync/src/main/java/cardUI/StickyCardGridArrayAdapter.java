package cardUI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import asynchronous.interfaces.AsyncArtifactToUI;
import asynchronous.interfaces.AsyncFetchBitmaps;
import bolts.Continuation;
import bolts.Task;
import cardUI.cards.GridCard;
import cardUI.cards.StickyHeaderCard;
import eplex.win.FastNEATJava.utils.cuid;
import eplex.win.winBackbone.Artifact;
import interfaces.PhenotypeCache;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.view.CardView;
import edu.eplex.asyncevolution.R;

/**
 * Created by paul on 8/19/14.
 */
public class StickyCardGridArrayAdapter extends CardGridArrayAdapter implements StickyGridHeadersSimpleAdapter {

    boolean callingExternalAdapter = false;
    CardView.OnExpandListAnimatorListener mCardExpandListener;
    ListAdapter externalAdapter;

    AsyncFetchBitmaps bitmapFetcher;

    public StickyCardGridArrayAdapter(Context context, List<Card> cards) {
        super(context, cards);
    }

    public void setExternalAdapter(ListAdapter adapter)
    {
        this.externalAdapter = adapter;
    }
    public void setCardExpandListener(CardView.OnExpandListAnimatorListener listener)
    {
        mCardExpandListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(!callingExternalAdapter)
        {
            callingExternalAdapter = true;
            convertView = externalAdapter.getView(position,convertView,parent);
            callingExternalAdapter = false;
            return convertView;
        }
        else
            return super.getView(position, convertView, parent);
    }

    @Override
    protected void setupExpandCollapseListAnimation(CardView cardView) {

        if (cardView == null) return;

        cardView.setOnExpandListAnimatorListener(mCardExpandListener);
    }

    @Override
    public long getHeaderId(int position) {
        Card card = getItem(position);
        return position;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        LayoutInflater mInflater = LayoutInflater.from(getContext());

        CardView cardView;
        final StickyHeaderCard colorCard;

        if (convertView == null) {

            View view = mInflater.inflate(R.layout.stick_card_header, null);

            cardView = (CardView) view.findViewById(R.id.sticky_header_card_id);
//            Card card = getItem(position);
//            char headerChar = card.getTitle().subSequence(0, 1).charAt(0);

            colorCard = new StickyHeaderCard(getContext());
            cardView.setCard(colorCard);
            convertView = view;
        } else {
            cardView = (CardView) convertView.findViewById(R.id.sticky_header_card_id);
            colorCard = (StickyHeaderCard) cardView.getCard();
        }

        GridCard gc = (GridCard) getItem(position);

        colorCard.setTitle("Parents : ");
        colorCard.setBackgroundResourceId(R.drawable.sticky_card_background);

        List<String> allParents = bitmapFetcher.syncRetrieveParents(gc.wid);

        final List<Bitmap> bitmaps = new ArrayList<Bitmap>();

        List<String> remaining = new ArrayList<String>();

        for (String parentWID : allParents) {
            Bitmap pCache = bitmapFetcher.syncRetrieveBitmap(parentWID);
            if (pCache == null) {
                remaining.add(parentWID);
            }
            else
                bitmaps.add(pCache);
        }
        if (remaining.size() == 0)
        {
            //we ahve all the bitmaps! send them to our sticky color card
            colorCard.setParents(bitmaps);
        }
        else
        {
            //otherwise, we need to async fetch some parent bitmaps -- then set them!
            bitmapFetcher.asyncFetchParentBitmaps(remaining)
                    .continueWith(new Continuation<Map<String, Bitmap>, Void>() {
                        @Override
                        public Void then(Task<Map<String, Bitmap>> task) throws Exception {
                            //now we have our matches, let's grab the parent values
                            if(task.isCancelled())
                            {
                                throw new RuntimeException("Converting object to UI was cancelled!");
                            }
                            else if(task.isFaulted())
                            {
                                Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                                throw task.getError();
                            }
                            //great success!
                            else {
                                Map<String, Bitmap> parentBitmaps = task.getResult();
                                for (Bitmap parent : parentBitmaps.values())
                                    bitmaps.add(parent);

                                //all done, now send it along to our stikcy header
                                colorCard.setParents(bitmaps);
                            }

                            return null;
                        }
                    });
        }


        return convertView;
    }

    public void setBitmapCacheAndFetch(AsyncFetchBitmaps bitmapFetcher) {

      this.bitmapFetcher = bitmapFetcher;

    }
}
