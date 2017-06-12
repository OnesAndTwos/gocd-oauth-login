package com.tw.go.plugin.provider.bitbucket;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.tw.go.plugin.util.JSONUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@SuppressWarnings("unused")
public class BitbucketProviderImpl extends AbstractProvider {

    private static Logger LOGGER = Logger.getLoggerFor(BitbucketProviderImpl.class);

    private OkHttpClient client = new OkHttpClient();

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

            for (Map.Entry<String, Object> entry : accessGrant.getAttributes().entrySet()) {
                LOGGER.info(format("%s : %s", entry.getKey(), entry.getValue()));
            }

            LOGGER.info(format("THE KEY: : %s", accessGrant.getKey()));
            LOGGER.info(format("PROVIDER ID: : %s", accessGrant.getProviderId()));
            LOGGER.info(format("THE PERMISSION SCOPE: : %s", accessGrant.getPermission().getScope()));
            LOGGER.info(format("THE SECRET: : %s", accessGrant.getSecret()));

            profile = new Profile();
            profile.setFirstName("Michael");
            profile.setLastName("Kay");
            profile.setEmail("mkay@thoughtworks.com");

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
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api.bitbucket.org")
                .addPathSegments("2.0/user")
                .addQueryParameter("access_token", profile.getValidatedId())
                .build();

        Request request = new Request.Builder().url(httpUrl.url()).build();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            Map<String, Object> userResponse = (Map<String, Object>) JSONUtils.fromJSON(response.body().string());

            Profile profile = new Profile();
            profile.setFullName(userResponse.get("display_name").toString());
            profile.setDisplayName(userResponse.get("display_name").toString());
            profile.setValidatedId(accessKey);
            profile.setProviderId("bitbucket");
            profile.setEmail(getEmail(accessKey));

            return profile;

        } catch (IOException e) {
            LOGGER.error("Error occurred while trying to perform get user", e);
            throw new RuntimeException("Error occurred while trying to perform get user", e);
        }
    }

    private String getEmail(String accessKey) {
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api.bitbucket.org")
                .addPathSegments("2.0/emails")
                .addQueryParameter("access_token", profile.getValidatedId())
                .build();

        Request request = new Request.Builder().url(httpUrl.url()).build();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            Map<String, Object> emailResponse = (Map<String, Object>) JSONUtils.fromJSON(response.body().string());
            List<Map<String, Object>> emails = (List<Map<String, Object>>) emailResponse.get("values");

            for(Map<String, Object> email : emails) {
                if(Boolean.parseBoolean(email.get("is_primary").toString())) {
                    return email.get("email").toString();
                }
            }

            throw new RuntimeException("No primary email on BitBucket account");

        } catch (IOException e) {
            LOGGER.error("Error occurred while trying to perform get email", e);
            throw new RuntimeException("Error occurred while trying to perform get email", e);
        }
    }
}
