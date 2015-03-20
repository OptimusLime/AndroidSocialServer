package edu.eplex.androidsocialclient.MainUI.Filters;

import android.graphics.Bitmap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.logging.Filter;
import bolts.Continuation;
import bolts.Task;
import edu.eplex.androidsocialclient.MainUI.Cache.BitmapCacheManager;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.FastNEATJava.utils.cuid;

/**
 * Created by paul on 3/15/15.
 * This is the object that houses the genotype and the image references
 */
public class FilterComposite {

    //what is our genotype that defines our full filter!
    FilterArtifact filterArtifact;

    //are there any frozen params?
    HashSet<Integer> frozenArtifactGenomes;

    //what image are we filtering???
    String imageURL;

    //where is our filtered image saved? At first this points to our image URL, then eventually we save!
    String filteredImageURL;

    //whats our name
    String readableName;

    //what's our unique identifier? So we can keep a hash map and be referenced quickly --
    //this will never change -- readable name can be changed though
    String uniqueID;

    //How are we cropping the image? What's the base image right now?
    @JsonIgnore
    Bitmap currentBitmap;

    //what is our filtered version? Applied to the current bitmap
    @JsonIgnore
    Bitmap filteredBitmap;

    //this is what the thumbnail looks like -- it's the unfiltered version
    @JsonIgnore
    Bitmap thumbnailBitmap;

    //this is our thumbnail, but it's a filtered version -- useful for browsing quickly
    @JsonIgnore
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
    public void setImageURL(String imageURL){this.imageURL = imageURL;}
    public String getFilteredImageURL()
    {
        return filteredImageURL;
    }
    public void setFilteredImageURL(String filteredImageURL){this.filteredImageURL = filteredImageURL;}
    public void setReadableName(String readableName){this.readableName = readableName;}
    public String getReadableName()
    {
        return readableName;
    }
    public void setUuid(String uniqueID){this.uniqueID  = uniqueID;}
    public String getUniqueID()
    {
        return uniqueID;
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

    public FilterComposite(){}
    public FilterComposite(String imageURL, String uniqueID, String readableName)
    {
        //create an empty artifact plz! Defaults galore!
        setClassConstruction(imageURL, null, uniqueID, readableName);
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
        this.uniqueID = uniqueID;
        this.filterArtifact = artifact;
        this.readableName = readableName;

        frozenArtifactGenomes = new HashSet<>();
    }

    public void setFrozenArtifactGenomes(HashSet<Integer> frozenArtifactGenomes){this.frozenArtifactGenomes = frozenArtifactGenomes;}
    public HashSet<Integer> getFrozenArtifactGenomes()
    {
        return frozenArtifactGenomes;
    }

    public void freezeArtifactGenome(Integer wid)
    {
        this.frozenArtifactGenomes.add(wid);
    }
    public void unfreezeArtifactGenome(Integer wid)
    {
        this.frozenArtifactGenomes.remove(wid);
    }
    public void unfreezeAll()
    {
        this.frozenArtifactGenomes.clear();
    }
    public void freezeAllButOne(Integer wid)
    {
        ArrayList<NeatGenome> allGenomes = this.filterArtifact.genomeFilters;
        for(int i=0; i < allGenomes.size(); i++)
            if(i != wid)
                this.frozenArtifactGenomes.add(i);
    }

    public void clearFilterBitmapMemory()
    {
        //recycle each filtereted bitmap we own -- if it isn't already recycled
        if(this.filteredBitmap != null && this.filteredBitmap != this.currentBitmap && !this.filteredBitmap.isRecycled())
            this.filteredBitmap.recycle();

        if(this.filteredThumbnailBitmap != null && !this.filteredThumbnailBitmap.isRecycled())
            this.filteredThumbnailBitmap.recycle();

    }

    public FilterComposite clone()
    {
        FilterComposite other = new FilterComposite();
        other.replaceWithFilter(this);
        //make sure it's a true clone of the artifact
        other.filterArtifact = ((FilterArtifact)this.filterArtifact.clone());
        other.setUuid(this.getUniqueID());
        return other;
    }

    public void replaceWithFilter(FilterComposite filter)
    {
//        if(this.filteredBitmap != null && this.filteredBitmap != filter.filteredBitmap && !this.filteredBitmap.isRecycled())
//            this.filteredBitmap.recycle();
//
//        if(this.filteredThumbnailBitmap != null && this.filteredThumbnailBitmap != filter.filteredThumbnailBitmap && !this.filteredThumbnailBitmap.isRecycled())
//            this.filteredThumbnailBitmap.recycle();

        //are there any frozen params?
        this.filterArtifact = filter.filterArtifact;
        this.frozenArtifactGenomes = filter.frozenArtifactGenomes;

        //what image are we filtering???
        this.imageURL = filter.imageURL;

        //where is our filtered image saved? At first this points to our image URL, then eventually we save!
        this.filteredImageURL = filter.filteredImageURL;

        //whats our name
        this.readableName = filter.readableName;

        //what's our unique identifier? So we can keep a hash map and be referenced quickly --
        //this will never change -- readable name can be changed though
//        this.uuid = filter.uuid;

        //How are we cropping the image? What's the base image right now?
        this.currentBitmap = filter.currentBitmap;

        //what is our filtered version? Applied to the current bitmap
        this.filteredBitmap = filter.filteredBitmap;

        //this is what the thumbnail looks like -- it's the unfiltered version
        this.thumbnailBitmap = filter.thumbnailBitmap;

        //this is our thumbnail, but it's a filtered version -- useful for browsing quickly
        this.filteredThumbnailBitmap = filter.filteredThumbnailBitmap;
    }



}
