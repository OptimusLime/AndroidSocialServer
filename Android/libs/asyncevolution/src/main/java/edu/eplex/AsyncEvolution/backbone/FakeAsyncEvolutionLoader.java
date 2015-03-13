package edu.eplex.AsyncEvolution.backbone;


import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;

import eplex.win.winBackbone.Artifact;
import eplex.win.winBackbone.AsyncEvolutionLoader;
import eplex.win.winBackbone.AsyncLoadCallback;

/**
 * Created by paul on 8/13/14.
 */
public class FakeAsyncEvolutionLoader implements AsyncEvolutionLoader{

    private ArrayList<Node> fakeNodes()
    {
        ArrayList<Node> nodes = new ArrayList<Node>();

        int fakeNodeCount = 1 + ((int)Math.floor(Math.random()*1000))%10;

        for(int i=0; i < fakeNodeCount; i++) {
            Node n = new Node();
            n.gid = "node-" + i + "-" +  (int) (Math.random() * 100000);
            nodes.add(n);
        }

        return nodes;
    }

    private ArrayList<Connection> fakeConnections()
    {
        ArrayList<Connection> conns = new ArrayList<Connection>();

        int fakeConnCount = 3 + ((int)Math.floor(Math.random()*1000))%18;

        for(int i=0; i < fakeConnCount; i++) {

            Connection c = new Connection();
            c.gid = "conn-" + i + "-" + (int) (Math.random() * 100000);
            c.sourceID = "node-" + (int) (Math.random() * 100000);
            c.targetID = "node-" + (int) (Math.random() * 100000);
            c.weight = Math.random() * 2 - 1;
            conns.add(c);
        }

        return conns;
    }

    ArrayList<Artifact> fakeSeeds()
    {
        ArrayList<Artifact> seeds =  new ArrayList<Artifact>();

        int fakeSeedCount = 1 + ((int)Math.floor(Math.random()*1000))%4;

        for(int i=0; i < fakeSeedCount; i++)
        {
            FakeArtifact fa = new FakeArtifact();
            FakeGenome g = new FakeGenome();

            //grab random fake nodes
            g.setNodes(fakeNodes());
            //random fake conns
            g.setConnections(fakeConnections());
            fa.genome = g;

            fa.setWID("wonky-seed" + i);

            seeds.add(fa);
        }

        return seeds;
    }

    @Override
    public void loadSeeds(final AsyncLoadCallback callback) {

        //fake delay the callback here :)
        new Handler(

                Looper.getMainLooper() // Optional, to run task on UI thread.

        ).postDelayed(new Runnable() {

            @Override
            public void run() {

                // Do the task...
                ArrayList<Artifact> seeds = fakeSeeds();
                callback.loadedSeeds(seeds);
            }

        }, 100);
//
    }
}
