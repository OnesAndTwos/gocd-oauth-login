package com.tw.go.plugin.provider.bitbucket;

import com.tw.go.plugin.PluginSettings;

public class BitbucketPluginSettings extends PluginSettings {

    public BitbucketPluginSettings(String serverBaseURL, String consumerKey, String consumerSecret) {
        super(serverBaseURL, consumerKey, consumerSecret);
    }

    public BitbucketPluginSettings() {
        super();
    }
}
