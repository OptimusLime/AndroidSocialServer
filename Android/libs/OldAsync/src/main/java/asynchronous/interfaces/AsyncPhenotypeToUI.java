package asynchronous.interfaces;

import android.app.Activity;

import com.fasterxml.jackson.databind.JsonNode;

import bolts.Task;

/**
 * Created by paul on 8/14/14.
 */
public interface AsyncPhenotypeToUI<PhenoClass, UIClass> {

    Task<UIClass> asyncPhenotypeToUI(Activity act, String wid, PhenoClass phenotype, JsonNode params);
}
