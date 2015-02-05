package cardUI;

/**
 * Created by paul on 8/13/14.
 */
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.GridView;

public class EndlessGridScrollListener implements OnScrollListener {

    private GridView gridView;
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
    public void setRequestItemsCallback(RequestItemsCallback refreshCallback)
    {
        this.requestItemsCallback=refreshCallback;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        //do not infinite scroll with 0 items! That don't make no sense.
        if(totalItemCount == 0)
            return;

        if (gridView.getLastVisiblePosition() + 1 == totalItemCount && !isLoading) {
            isLoading = true;
            if (hasMorePages&&!isRefreshing) {
                isRefreshing=true;
                requestItemsCallback.requestItems(pageNumber);
            }
        } else {
            isLoading = false;
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