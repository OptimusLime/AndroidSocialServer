package edu.eplex.AsyncEvolution.backbone;


import java.io.NotActiveException;
import java.util.List;

import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.ArtifactOffspringGenerator;

/**
 * Created by paul on 8/8/14.
 */
public class FakeArtifactOffspringGenerator implements ArtifactOffspringGenerator {

    @Override
    public void clearSession() {

    }

    public Artifact createArtifactFromParents(List<Artifact> parents)
    {
        if(parents.size() == 0)
            return null;

        int selIx = (int)Math.floor(Math.random()*parents.size());

        //return a clone of one of the parents, please!
        Artifact clone = parents.get(selIx).clone();
        //add clone info to our WID -- totally a fake thing to do
        clone.setWID("clone-" + (int)(Math.random()*Integer.MAX_VALUE) + "-p-" + clone.wid());
        return clone;
    }
}
