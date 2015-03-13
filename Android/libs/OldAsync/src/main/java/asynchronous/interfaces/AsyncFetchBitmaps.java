package asynchronous.interfaces;

import android.graphics.Bitmap;

import java.util.List;
import java.util.Map;

import bolts.Task;
import eplex.win.winBackbone.Artifact;

/**
 * Created by paul on 8/20/14.
 */
public interface AsyncFetchBitmaps {

    public List<String> syncRetrieveParents(String wid);
    public Bitmap syncRetrieveBitmap(String wid);
    public Task<Map<String, Bitmap>> asyncFetchParentBitmaps(List<String> parents);
}
