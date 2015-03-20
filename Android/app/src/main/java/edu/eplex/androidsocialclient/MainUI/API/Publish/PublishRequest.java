package edu.eplex.androidsocialclient.MainUI.API.Publish;

import java.util.List;
import java.util.Map;

import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;

/**
 * Created by paul on 3/19/15.
 */
public class PublishRequest {
    public String uuid;
    public String accessToken;
    public Map<String, FilterArtifact> filterArtifacts;
}
