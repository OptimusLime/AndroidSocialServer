package edu.eplex.androidsocialclient.Utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * FileUtilities helps us perform file related tasks in Android
 */
public class FileUtilities {

    final static String TAG = "FileUtilities";

    //This function returns a storage directory given a folder name
    //This function would need to be adjusted for targeting Pre-Froyo (API 8) Android devices
    public static File getStorageDirectoryFromName(String folderName)
    {
        //We essentially append the directory for pictures + our directory name, then return as a file object
        return new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ),
                folderName
        );
    }

    //creates an image file according to a time stamp and folder location
    public static File createImageFile(String folderLocation, String imageNamePrefix, String imageNameSuffix) throws IOException {
        // Create an image file name
        //First we grab a timestamp of the current time
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        //then we prepend our prefix before the timestamp
        String imageFileName = imageNamePrefix + timeStamp + "_";

        //we get the current directory location
        File albumF = getExternalStorageDirectory(folderLocation);

        //Finally, we create a blank file at the appropriate location
        File imageF = File.createTempFile(imageFileName, imageNameSuffix, albumF);

        //we pass back the file object reference to the file we just created
        return imageF;
    }

    //Creates file object representing a given external storage folder directory
    public static File getExternalStorageDirectory(String folderLocation)
    {
        //We will return a file object representing the external folder or NULL in case of error
        File storageDir = null;

        //We must check if the external storage is mounted on this device
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            //we attempt to create the File object representing this folder location
            storageDir = getStorageDirectoryFromName(folderLocation);

            //if we did not fail
            if (storageDir != null) {

                //we must create the storage directory if it doesn't exist
                if (! storageDir.mkdirs()) {

                    //if after our attempts, it still doesn't exist, something went wrong
                    if (! storageDir.exists()){
                        Log.d(TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            //if no external storage, our method will return null
            Log.v(TAG, "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    //From Android documentation: http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    //Allows the loading of a bitmap from file with a certain requested width and height
    //It's meant to allow for more efficient bitmap reads into memory
    public static Bitmap decodeSampledBitmapFromResource(String strPath, int maxDimension) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();

        //we decode the bitmap, but only looking for the true width/height
        options.inJustDecodeBounds = true;

        //here we decode into options
        BitmapFactory.decodeFile(strPath, options);

        int rawHeight = options.outHeight;
        int rawWidth = options.outWidth;

        double scale = (double)maxDimension/Math.min(rawHeight, rawWidth);

        //load the scaled version -- thx
        int reqWidth = (int)Math.round(scale*rawWidth);
        int reqHeight = (int)Math.round(scale*rawHeight);

        // Calculate inSampleSize -- this will determine how our bitmap is cropped during actual decode
        options.inSampleSize = calculateInSampleSize(options,reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        //decode to the appropriate size
        return BitmapFactory.decodeFile(strPath, options);
    }

    //From Android documentation: http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    //Allows the loading of a bitmap from file with a certain requested width and height
    //It's meant to allow for more efficient bitmap reads into memory
    public static Bitmap decodeSampledBitmapFromResource(String strPath, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();

        //we decode the bitmap, but only looking for the true width/height
        options.inJustDecodeBounds = true;

        //here we decode into options
        BitmapFactory.decodeFile(strPath, options);

        // Calculate inSampleSize -- this will determine how our bitmap is cropped during actual decode
        options.inSampleSize = calculateInSampleSize(options,reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        //decode to the appropriate size
        return BitmapFactory.decodeFile(strPath, options);
    }

    //From documentation: http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    static int calculateInSampleSize(BitmapFactory.Options options,
                                                 int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


}
