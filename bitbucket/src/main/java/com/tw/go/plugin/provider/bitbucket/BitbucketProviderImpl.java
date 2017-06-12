package com.tw.go.plugin.provider.bitbucket;

import com.thoughtworks.go.plugin.api.logging.Logger;
import org.brickred.socialauth.AbstractProvider;
import org.brickred.socialauth.Contact;
import org.brickred.socialauth.Permission;
import org.brickred.socialauth.Profile;
import org.brickred.socialauth.exception.AccessTokenExpireException;
import org.brickred.socialauth.exception.SocialAuthException;
import org.brickred.socialauth.exception.UserDeniedPermissionException;
import org.brickred.socialauth.oauthstrategy.OAuth2;
import org.brickred.socialauth.oauthstrategy.OAuthStrategyBase;
import org.brickred.socialauth.util.AccessGrant;
import org.brickred.socialauth.util.Constants;
import org.brickred.socialauth.util.OAuthConfig;
import org.brickred.socialauth.util.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@SuppressWarnings("unused")
public class BitbucketProviderImpl extends AbstractProvider {

    private static Logger LOGGER = Logger.getLoggerFor(BitbucketProviderImpl.class);

    private final HashMap<String, String> endpoints;
    private final OAuth2 authenticationStrategy;
    private final OAuthConfig config;

    private Permission scope;
    private AccessGrant accessGrant;
    private Profile profile;

    public BitbucketProviderImpl(OAuthConfig providerConfig) throws Exception {
        config = providerConfig;

        if (config.getCustomPermissions() != null) {
            scope = Permission.CUSTOM;
        }

        endpoints = new HashMap<>();
        endpoints.put(Constants.OAUTH_AUTHORIZATION_URL, providerConfig.getAuthenticationUrl());
        endpoints.put(Constants.OAUTH_ACCESS_TOKEN_URL, providerConfig.getAccessTokenUrl());
        authenticationStrategy = new OAuth2(config, endpoints);
        authenticationStrategy.setPermission(scope);
        authenticationStrategy.setScope(getScope());
    }

    @Override
    public Profile getUserProfile() throws Exception {
        String profilesResponse = authenticationStrategy.executeFeed("https://api.bitbucket.org/2.0/user").getResponseBodyAsString(Constants.ENCODING);
        String emailsResponse = authenticationStrategy.executeFeed("https://api.bitbucket.org/2.0/user/emails").getResponseBodyAsString(Constants.ENCODING);
        String teamsResponse = authenticationStrategy.executeFeed("https://api.bitbucket.org/2.0/teams?role=member").getResponseBodyAsString(Constants.ENCODING);

        JSONObject profiles = new JSONObject(profilesResponse);
        JSONObject emails = new JSONObject(emailsResponse);
        JSONObject teams = new JSONObject(teamsResponse);

        BitBucketProfile profile = new BitBucketProfile();

        profile.setDisplayName(profiles.getString("username"));
        profile.setFullName(profiles.getString("display_name"));

        JSONArray emailValues = emails.getJSONArray("values");

        for(Object emailObject : emailValues) {
            JSONObject email = (JSONObject) emailObject;
            if(email.getBoolean("is_primary")) {
                profile.setEmail(email.getString("email"));
            }
        }

        JSONArray teamValues = teams.getJSONArray("values");

        for(Object teamObject : teamValues) {
            JSONObject team = (JSONObject) teamObject;
            profile.addTeam(team.getString("username"));
        }

        return profile;
    }

    @Override
    public String getLoginRedirectURL(String successUrl) throws Exception {
        return authenticationStrategy.getLoginRedirectURL(successUrl);
    }

    @Override
    public Profile verifyResponse(final Map<String, String> requestParams) throws Exception {
        return doVerifyResponse(requestParams);
    }

    @Override
    public void logout() {
        profile = null;
        accessGrant = null;
        authenticationStrategy.logout();
    }

    @Override
    public void setPermission(Permission permission) {
        LOGGER.debug(format("Permission requested : %s", permission.toString()));
        this.scope = permission;
        authenticationStrategy.setPermission(this.scope);
        authenticationStrategy.setScope(getScope());
    }

    @Override
    public Response api(String url, String methodType, Map<String, String> params, Map<String, String> headerParams, String body) throws Exception {
        LOGGER.info(format("Calling API function for url: %s", url));

        try {
            return authenticationStrategy.executeFeed(url, methodType, params, headerParams, body);
        } catch (Exception e) {
            throw new SocialAuthException(format("Error while making request to URL : %s", url), e);
        }

    }

    private Profile doVerifyResponse(final Map<String, String> requestParams) throws Exception {
        LOGGER.info("Retrieving Access Token in verify response function");

        if (requestParams.get("error_reason") != null && "user_denied".equals(requestParams.get("error_reason"))) {
            throw new UserDeniedPermissionException();
        }

        accessGrant = authenticationStrategy.verifyResponse(requestParams, "POST");

        if (accessGrant != null) {
            LOGGER.debug("Access grant available");

            profile = new Profile();
            profile.setValidatedId(accessGrant.getKey());
            return profile;
        } else {
            throw new SocialAuthException("Access token not found");
        }
    }

    @Override
    public AccessGrant getAccessGrant() {
        return accessGrant;
    }

    @Override
    public String getProviderId() {
        return config.getId();
    }

    @Override
    public void setAccessGrant(final AccessGrant accessGrant) throws AccessTokenExpireException, SocialAuthException {
        this.accessGrant = accessGrant;
        authenticationStrategy.setAccessGrant(accessGrant);
    }

    private String getScope() {
        if (Permission.CUSTOM.equals(scope) && config.getCustomPermissions() != null) {
            return config.getCustomPermissions();
        } else {
            return null;
        }
    }

    @Override
    protected OAuthStrategyBase getOauthStrategy() {
        return authenticationStrategy;
    }

    @Override
    protected List<String> getPluginsList() {
        return new ArrayList<>();
    }

    @Override
    public Response updateStatus(String msg) throws Exception {
        throw notSupported("Update Status");
    }

    @Override
    public List<Contact> getContactList() throws Exception {
        throw notSupported("Get Contact List");
    }

    @Override
    public Response uploadImage(final String message, final String fileName, final InputStream inputStream) throws Exception {
        throw notSupported("Upload Image");
    }

    private SocialAuthException notSupported(String action) {
        LOGGER.warn("WARNING: Not implemented for BitBucket");
        return new SocialAuthException(format("%s is not implemented for BitBucket", action));
    }

    private Profile getProfile(String accessKey) {
        Profile profile = new Profile();
        profile.setValidatedId(accessKey);
        profile.setProviderId("bitbucket");
        return profile;
    }
}
