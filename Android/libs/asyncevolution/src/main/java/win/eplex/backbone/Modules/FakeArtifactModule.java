package win.eplex.backbone.Modules;

import dagger.Module;
import dagger.Provides;
import eplex.win.winBackbone.Genome;
import win.eplex.backbone.FakeArtifact;
import win.eplex.backbone.FakeGenome;

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
