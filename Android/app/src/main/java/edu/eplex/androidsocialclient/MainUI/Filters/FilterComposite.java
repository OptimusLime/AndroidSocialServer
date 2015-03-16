package edu.eplex.androidsocialclient.MainUI.Filters;

import android.graphics.Bitmap;

import java.util.concurrent.Callable;
import java.util.logging.Filter;
import bolts.Continuation;
import bolts.Task;
import edu.eplex.androidsocialclient.MainUI.Cache.BitmapCacheManager;
import eplex.win.FastNEATJava.utils.cuid;

/**
 * Created by paul on 3/15/15.
 * This is the object that houses the genotype and the image references
 */
public class FilterComposite {

    //what is our genotype that defines our full filter!
    FilterArtifact filterArtifact;

    //what image are we filtering???
    String imageURL;

    //where is our filtered image saved? At first this points to our image URL, then eventually we save!
    String filteredImageURL;

    //whats our name
    String readableName;

    //what's our unique identifier? So we can keep a hash map and be referenced quickly --
    //this will never change -- readable name can be changed though
    String uuid;

    //How are we cropping the image? What's the base image right now?
    Bitmap currentBitmap;

    //what is our filtered version? Applied to the current bitmap
    Bitmap filteredBitmap;

    //this is what the thumbnail looks like -- it's the unfiltered version
    Bitmap thumbnailBitmap;

    //this is our thumbnail, but it's a filtered version -- useful for browsing quickly
    Bitmap filteredThumbnailBitmap;

    public Bitmap getThumbnailBitmap()
    {
        return thumbnailBitmap;
    }

    public Bitmap getFilteredThumbnailBitmap()
    {
        return filteredThumbnailBitmap;
    }

    public Bitmap getCurrentBitmap()
    {
        return currentBitmap;
    }

    public Bitmap getFilteredBitmap()
    {
        return  filteredBitmap;
    }

    public String getImageURL()
    {
        return imageURL;
    }

    public String getFilteredImageURL()
    {
        return filteredImageURL;
    }

    public String getReadableName()
    {
        return readableName;
    }
    public String getUniqueID()
    {
        return uuid;
    }


    public FilterArtifact getFilterArtifact()
    {
        return  filterArtifact;
    }
    public void setFilterArtifact(FilterArtifact filterArtifact)
    {
        this.filterArtifact = filterArtifact;
    }

    public Task<Void> AsyncApplyFilterToImage()
    {
        //take the filter and reapply it to the image -- this is async -- test it and forget it :)
        return Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                return null;
            }
        });
    }

    public FilterComposite(String imageURL, String uniqueID, String readableName)
    {
        //create an empty artifact plz! Defaults galore!
        setClassConstruction(imageURL, new FilterArtifact(), uniqueID, readableName);
    }
    public FilterComposite(String imageURL, FilterArtifact artifact, String uniqueID, String readableName)
    {
        //set the values up -- need all 4 unfortunately
        setClassConstruction(imageURL, artifact, uniqueID, readableName);
    }
    public FilterComposite(String imageURL, FilterArtifact artifact, String readableName)
    {
        //set the values up -- need all 4 unfortunately
        setClassConstruction(imageURL, artifact, cuid.getInstance().generate(), readableName);
    }

    public void setFilteredBitmap(Bitmap bm)
    {
        //repalce this filtered bitmap -- plz
        if(this.filteredBitmap != null && !this.filteredBitmap.isRecycled())
            BitmapCacheManager.getInstance().replaceCachedBitmap(this.getImageURL(), this.filteredBitmap.getWidth(), this.filteredBitmap.getHeight(), true, bm);

        //then save the new image as our filtered image
        this.filteredBitmap = bm;

    }
    public void setCurrentBitmap(Bitmap bm)
    {
        this.currentBitmap = bm;
    }
    public void setThumbnailBitmap(Bitmap bm){this.thumbnailBitmap = bm;}
    public void setFilteredThumbnailBitmap(Bitmap bm){this.filteredThumbnailBitmap = bm;}


    //set our internal strings and what have you
    void setClassConstruction(String imageURL, FilterArtifact artifact, String uniqueID, String readableName)
    {
        //set evertything
        this.imageURL = imageURL;
        //dupe it up when first created
        this.filteredImageURL = imageURL;
        this.uuid = uniqueID;
        this.filterArtifact = artifact;
        this.readableName = readableName;
    }


}
