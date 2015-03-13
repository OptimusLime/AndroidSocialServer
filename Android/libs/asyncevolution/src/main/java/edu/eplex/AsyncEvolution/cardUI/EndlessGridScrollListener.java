package edu.eplex.AsyncEvolution.cardUI;

/**
 * Created by paul on 8/13/14.
 */
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.GridView;
import android.widget.ListView;
import edu.eplex.AsyncEvolution.views.HorizontalListView;
import it.sephiroth.android.library.widget.AbsHListView;
import it.sephiroth.android.library.widget.HListView;

public class EndlessGridScrollListener implements OnScrollListener, AbsHListView.OnScrollListener {

    private GridView gridView;
    private HListView listView;
    private boolean isLoading;
    private boolean hasMorePages;
    private int pageNumber=0;
    private RequestItemsCallback requestItemsCallback;
    private boolean isRefreshing;

    public EndlessGridScrollListener(GridView gridView) {
        this.gridView = gridView;
        this.isLoading = false;
        this.hasMorePages = true;

    }
    public EndlessGridScrollListener(HListView gridView) {
        this.listView = gridView;
        this.isLoading = false;
        this.hasMorePages = true;
    }
    public void setRequestItemsCallback(RequestItemsCallback refreshCallback)
    {
        this.requestItemsCallback=refreshCallback;
    }

    @Override
    public void onScrollStateChanged(AbsHListView absHListView, int i) {

    }

    @Override
    public void onScroll(AbsHListView absHListView,  int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        //do not infinite scroll with 0 items! That don't make no sense.
        if(totalItemCount == 0)
            return;

        if(gridView != null) {

            if (gridView.getLastVisiblePosition() + 1 == totalItemCount && !isLoading) {
                isLoading = true;
                if (hasMorePages && !isRefreshing) {
                    isRefreshing = true;
                    requestItemsCallback.requestItems(pageNumber);
                }
            } else {
                isLoading = false;
            }
        }
        else //list view!
        {
            if (listView.getLastVisiblePosition() + 1 == totalItemCount && !isLoading) {
                isLoading = true;
                if (hasMorePages && !isRefreshing) {
                    isRefreshing = true;
                    requestItemsCallback.requestItems(pageNumber);
                }
            } else {
                isLoading = false;
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        //do not infinite scroll with 0 items! That don't make no sense.
        if(totalItemCount == 0)
            return;

        if(gridView != null) {

            if (gridView.getLastVisiblePosition() + 1 == totalItemCount && !isLoading) {
                isLoading = true;
                if (hasMorePages && !isRefreshing) {
                    isRefreshing = true;
                    requestItemsCallback.requestItems(pageNumber);
                }
            } else {
                isLoading = false;
            }
        }
        else //list view!
        {
            if (listView.getLastVisiblePosition() + 1 == totalItemCount && !isLoading) {
                isLoading = true;
                if (hasMorePages && !isRefreshing) {
                    isRefreshing = true;
                    requestItemsCallback.requestItems(pageNumber);
                }
            } else {
                isLoading = false;
            }
        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    public void noMorePages() {
        this.hasMorePages = false;
    }

    public void notifyMorePages(){
        isRefreshing=false;
        pageNumber=pageNumber+1;
    }

    public interface RequestItemsCallback {
        public void requestItems(int pageNumber);
    }
}