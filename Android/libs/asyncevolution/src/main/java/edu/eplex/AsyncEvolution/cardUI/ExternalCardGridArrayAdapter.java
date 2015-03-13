package edu.eplex.AsyncEvolution.cardUI;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.view.CardGridView;
import it.gmariotti.cardslib.library.view.base.CardViewWrapper;

/**
 * Created by paul on 3/12/15.
 */
public class ExternalCardGridArrayAdapter extends CardGridArrayAdapter {

    protected ListAdapter externalAdapter;
    private boolean callingExternalAdapter = false;

    public ExternalCardGridArrayAdapter(Context context, List<Card> cards) {
        super(context, cards);
    }
    public void setExternalAdapter(ListAdapter adapter)
    {
        this.externalAdapter = adapter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(externalAdapter == null)
           return super.getView(position, convertView, parent);

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
    protected void setupSwipeableAnimation(Card card, CardViewWrapper cardView) {
        super.setupSwipeableAnimation(card, cardView);
    }

    @Override
    protected void setupExpandCollapseListAnimation(CardViewWrapper cardView) {
        super.setupExpandCollapseListAnimation(cardView);
    }

    @Override
    public CardGridView getCardGridView() {
        return super.getCardGridView();
    }

    @Override
    public void setCardGridView(CardGridView cardGridView) {
        super.setCardGridView(cardGridView);
    }
}
