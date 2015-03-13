package edu.eplex.AsyncEvolution.backbone.Modules;

import dagger.Module;
import dagger.Provides;
import eplex.win.winBackbone.Genome;
import edu.eplex.AsyncEvolution.backbone.FakeArtifact;
import edu.eplex.AsyncEvolution.backbone.FakeGenome;

/**
 * Created by paul on 8/13/14.
 */
@Module(
        injects={FakeArtifact.class}
)
public class FakeArtifactModule {

    @Provides
    Genome provideGenome(){
        return new FakeGenome();
    }

}
