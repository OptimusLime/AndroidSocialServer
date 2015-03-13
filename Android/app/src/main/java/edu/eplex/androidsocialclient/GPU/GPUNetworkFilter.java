package edu.eplex.androidsocialclient.GPU;

import jp.co.cyberagent.android.gpuimage.GPUImage3x3TextureSamplingFilter;

/**
 * Created by paul on 3/13/15.
 */
public class GPUNetworkFilter extends GPUImage3x3TextureSamplingFilter {

    public GPUNetworkFilter(String fragmentShader)
    {
        super(fragmentShader);
    }

}
