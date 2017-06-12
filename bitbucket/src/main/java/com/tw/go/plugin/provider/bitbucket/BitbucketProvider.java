package com.tw.go.plugin.provider.bitbucket;

import com.tw.go.plugin.User;
import com.tw.go.plugin.provider.Provider;
import com.tw.go.plugin.util.JSONUtils;
import com.tw.go.plugin.util.Util;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.brickred.socialauth.Permission;
import org.brickred.socialauth.Profile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.tw.go.plugin.OAuthLoginPlugin.LOGGER;
import static com.tw.go.plugin.OAuthLoginPlugin.PLUGIN_SETTINGS_CONSUMER_KEY;
import static com.tw.go.plugin.OAuthLoginPlugin.PLUGIN_SETTINGS_CONSUMER_SECRET;
import static com.tw.go.plugin.OAuthLoginPlugin.PLUGIN_SETTINGS_SERVER_BASE_URL;
import static java.util.Collections.emptyList;

public class BitbucketProvider implements Provider<BitbucketPluginSettings> {

    private static final String IMAGE = Util.pluginImage();
    private static final String PLUGIN_ID = Util.pluginId();
    public static final String PLUGIN_SETTINGS_AUTHOIZED_TEAMS = "authorized_teams";

    private OkHttpClient client = new OkHttpClient();

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
        return new BitBucketUser(profile.getEmail(), profile.getFullName(), profile.getEmail(), getTeams(profile));
    }

    @Override
    public List<User> searchUser(BitbucketPluginSettings pluginSettings, String searchTerm) {
        return null;
    }

    @Override
    public boolean authorize(BitbucketPluginSettings pluginSettings, User user) {
        if(user.getClass().isInstance(BitBucketUser.class)) {
            BitBucketUser bitBucketUser = (BitBucketUser) user;
            return bitBucketUser.belongsToOneOfTheTeams(pluginSettings.getAuthorizedTeams());
        }

        return false;
    }

    @Override
    public Properties configure(BitbucketPluginSettings pluginSettings) {
        Properties properties = new Properties();

        properties.put("socialauth.bitbucket", "com.tw.go.plugin.provider.bitbucket.BitbucketProviderImpl");
        properties.put("bitbucket.consumer_key", pluginSettings.getConsumerKey());
        properties.put("bitbucket.consumer_secret", pluginSettings.getConsumerSecret());

        properties.put("bitbucket.authentication_url", "https://bitbucket.org/site/oauth2/authorize");
        properties.put("bitbucket.access_token_url", "https://bitbucket.org/site/oauth2/access_token");
        properties.put("bitbucket.access_token_url", "https://bitbucket.org/site/oauth2/access_token");
        properties.put("bitbucket.server_base_url", pluginSettings.getServerBaseURL());

        return properties;
    }

    @Override
    public BitbucketPluginSettings pluginSettings(Map<String, String> responseBodyMap) {
        return new BitbucketPluginSettings(
                responseBodyMap.get(PLUGIN_SETTINGS_SERVER_BASE_URL),
                responseBodyMap.get(PLUGIN_SETTINGS_CONSUMER_KEY),
                responseBodyMap.get(PLUGIN_SETTINGS_CONSUMER_SECRET),
                responseBodyMap.get(PLUGIN_SETTINGS_AUTHOIZED_TEAMS)
        );
    }

    private List<String> getTeams(Profile profile) {
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api.bitbucket.org")
                .addPathSegments("2.0/teams")
                .addQueryParameter("access_token", profile.getValidatedId())
                .build();

        Request request = new Request.Builder().url(httpUrl.url()).build();

        try {
            Response response = client.newCall(request).execute();
            Map<String, Object> teamResponse = (Map<String, Object>) JSONUtils.fromJSON(response.body().string());
            List<Map<String, Object>> teams = (List<Map<String, Object>>) teamResponse.get("values");

            List<String> teamUsernames = emptyList();
            for (Map<String, Object> team : teams) {
                teamUsernames.add(team.get("username").toString());
            }

            return teamUsernames;

        } catch (IOException e) {
            LOGGER.error("Error occurred while trying to perform get teams for user", e);
            throw new RuntimeException("Error occurred while trying to perform get teams for user", e);
        }
    }

}