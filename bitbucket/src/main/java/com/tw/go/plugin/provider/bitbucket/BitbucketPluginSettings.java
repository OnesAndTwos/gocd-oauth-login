package com.tw.go.plugin.provider.bitbucket;

import com.tw.go.plugin.PluginSettings;

import java.util.Arrays;
import java.util.List;

public class BitbucketPluginSettings extends PluginSettings {

    private List<String> authorizedTeams;

    public BitbucketPluginSettings(String serverBaseURL, String consumerKey, String consumerSecret, String authorizedTeams) {
        super(serverBaseURL, consumerKey, consumerSecret);
        this.authorizedTeams = Arrays.asList(authorizedTeams.split(","));
    }

    public BitbucketPluginSettings() {
        super();
    }

    public List<String> getAuthorizedTeams() {
        return authorizedTeams;
    }
}
