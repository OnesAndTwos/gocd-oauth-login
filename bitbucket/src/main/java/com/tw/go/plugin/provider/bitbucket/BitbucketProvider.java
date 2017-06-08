package com.tw.go.plugin.provider.bitbucket;

import com.tw.go.plugin.User;
import com.tw.go.plugin.provider.Provider;
import com.tw.go.plugin.util.Util;
import org.brickred.socialauth.Permission;
import org.brickred.socialauth.Profile;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.tw.go.plugin.OAuthLoginPlugin.PLUGIN_SETTINGS_CONSUMER_KEY;
import static com.tw.go.plugin.OAuthLoginPlugin.PLUGIN_SETTINGS_CONSUMER_SECRET;
import static com.tw.go.plugin.OAuthLoginPlugin.PLUGIN_SETTINGS_SERVER_BASE_URL;

public class BitbucketProvider implements Provider<BitbucketPluginSettings> {

    private static final String IMAGE = Util.pluginImage();
    private static final String PLUGIN_ID = Util.pluginId();

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getName() {
        return "bitbucket";
    }

    @Override
    public String getImageURL() {
        return IMAGE;
    }

    @Override
    public String getProviderName() {
        return "bitbucket";
    }

    @Override
    public Permission getAuthPermission() {
        return Permission.AUTHENTICATE_ONLY;
    }

    @Override
    public User getUser(Profile profile) {
        String emailId = profile.getEmail();
        String fullName = profile.getFullName();
        return new User(emailId, fullName, emailId);
    }

    @Override
    public List<User> searchUser(BitbucketPluginSettings pluginSettings, String searchTerm) {
        return null;
    }

    @Override
    public boolean authorize(BitbucketPluginSettings pluginSettings, User user) {
        return true;
    }

    @Override
    public Properties configure(BitbucketPluginSettings pluginSettings) {
        Properties properties = new Properties();

        properties.put("socialauth.bitbucket", "com.tw.go.plugin.provider.bitbucket.BitbucketProviderImpl");
        properties.put("bitbucket.consumer_key", pluginSettings.getConsumerKey());
        properties.put("bitbucket.consumer_secret", pluginSettings.getConsumerSecret());

        properties.put("bitbucket.authentication_url", "https://bitbucket.org/site/oauth2/authorize");
        properties.put("bitbucket.access_token_url", "https://bitbucket.org/site/oauth2/access_token");
        properties.put("bitbucket.server_base_url", pluginSettings.getServerBaseURL());

        return properties;
    }

    @Override
    public BitbucketPluginSettings pluginSettings(Map<String, String> responseBodyMap) {
        return new BitbucketPluginSettings(
                responseBodyMap.get(PLUGIN_SETTINGS_SERVER_BASE_URL),
                responseBodyMap.get(PLUGIN_SETTINGS_CONSUMER_KEY),
                responseBodyMap.get(PLUGIN_SETTINGS_CONSUMER_SECRET)
        );
    }
}
