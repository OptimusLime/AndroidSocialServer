package asynchronous.implementation;

import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eplex.win.FastCPPNJava.utils.MathUtils;
import eplex.win.FastNEATJava.genome.NeuronGeneStruct;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.FastNEATJava.utils.cuid;
import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.ArtifactOffspringGenerator;
import win.eplex.backbone.NEATArtifact;

/**
 * Created by paul on 8/8/14.
 */

//this object handles mating genomes together -- so it has our neat parameters
//and session objects as well -- which can be cleared at any time
public class SyncLocalOffspringGenerator implements ArtifactOffspringGenerator {

    Map<String, NeuronGeneStruct> newNodeTable = new HashMap<String, NeuronGeneStruct>();
    Map<String, String> newConnectionTable = new HashMap<String, String>();
    NeatParameters np;

    public SyncLocalOffspringGenerator(NeatParameters np)
    {
        this.np = np;
    }

    @Override
    public void clearSession() {

        newNodeTable.clear();
        newConnectionTable.clear();
    }

    public Artifact asexualMating(Artifact parent)
    {
        //return a clone of one of the parents, please!
        Artifact clone = parent.clone();

        NEATArtifact na = (NEATArtifact)clone;

        //grab the unique identifier for the genome
        String gParent = na.genome.gid;

        //create an asexual offspring object!
        na.genome = na.genome.createOffspringAsexual(newNodeTable, newConnectionTable, np);

        //set the parent! For tracking purposes
        na.genome.parents = Arrays.asList(gParent);

        //add some extra mutations for good show!
        for(int i=0; i < 5; i++)
            na.genome.mutate(newNodeTable, newConnectionTable, np);

        //set the new clone artifact id as a new uuid
        clone.setWID(cuid.getInstance().generate());

        //set the parent as the chosen object! DUH!
        clone.setParents(Arrays.asList(parent.wid()));

        return clone;
    }

    public Artifact sexualMating(Artifact parent, Artifact parent2)
    {
        //return a clone of one of the parents, please!
        Artifact clone = parent.clone();

        NEATArtifact o1 = (NEATArtifact)clone;
        NEATArtifact p2 = (NEATArtifact)parent2;

        String p1GID= o1.genome.gid;
        //create an asexual offspring object!
        o1.genome = o1.genome.createOffspringSexual(p2.genome, np);

        //set the parent! For tracking purposes
        o1.genome.parents = Arrays.asList(p1GID, p2.genome.gid);

        //add some extra mutations for good show!
        for(int i=0; i < 5; i++)
            o1.genome.mutate(newNodeTable, newConnectionTable, np);

        //set the new clone artifact id as a new uuid
        clone.setWID(cuid.getInstance().generate());

        //set the parent as the chosen object! DUH!
        clone.setParents(Arrays.asList(parent.wid(), parent2.wid()));

        return clone;
    }

    public Artifact createArtifactFromParents(List<Artifact> parents)
    {
        if(parents.size() == 0)
            throw new NotImplementedException("Must have at least 1 parent object");

        if(parents.size() > 1 && MathUtils.nextDouble() < np.pOffspringSexual)
        {
            //double selection process
            //we heavily favor recent parents over older parents
            int selIx = MathUtils.singleThrowCubeWeighted(parents.size());

            int p2SelIx = MathUtils.singleThrowCubeWeighted(parents.size());
            int maxAttempt = 5;
            int atmp = 0;
            while(atmp++ < maxAttempt && p2SelIx == selIx)
                p2SelIx = MathUtils.singleThrowCubeWeighted(parents.size());

            if(p2SelIx == selIx)
                return asexualMating(parents.get(selIx));
            else
                return sexualMating(parents.get(selIx), parents.get(p2SelIx));

        }
        else
        {
            //we heavily favor recent parents over older parents
            int selIx = MathUtils.singleThrowCubeWeighted(parents.size());
            return asexualMating(parents.get(selIx));
        }
    }
}
