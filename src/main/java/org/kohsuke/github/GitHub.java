/*
 * The MIT License
 *
 * Copyright (c) 2010, Kohsuke Kawaguchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.kohsuke.github;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.github.authorization.AuthorizationProvider;
import org.kohsuke.github.authorization.ImmutableAuthorizationProvider;
import org.kohsuke.github.authorization.UserAuthorizationProvider;
import org.kohsuke.github.connector.GitHubConnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// TODO: Auto-generated Javadoc
/**
 * Root of the GitHub API.
 *
 * <h2>Thread safety</h2>
 * <p>
 * This library aims to be safe for use by multiple threads concurrently, although the library itself makes no attempt
 * to control/serialize potentially conflicting operations to GitHub, such as updating &amp; deleting a repository at
 * the same time.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHub {

    /**
     * The Class DependentAuthorizationProvider.
     */
    public static abstract class DependentAuthorizationProvider implements AuthorizationProvider {

        private final AuthorizationProvider authorizationProvider;
        private GitHub baseGitHub;
        private GitHub gitHub;

        /**
         * An AuthorizationProvider that requires an authenticated GitHub instance to provide its authorization.
         *
         * @param authorizationProvider
         *            A authorization provider to be used when refreshing this authorization provider.
         */
        @BetaApi
        protected DependentAuthorizationProvider(AuthorizationProvider authorizationProvider) {
            this.authorizationProvider = authorizationProvider;
        }

        /**
         * Git hub.
         *
         * @return the git hub
         */
        protected synchronized final GitHub gitHub() {
            if (gitHub == null) {
                gitHub = new GitHub.AuthorizationRefreshGitHubWrapper(this.baseGitHub, authorizationProvider);
            }
            return gitHub;
        }

        /**
         * Binds this authorization provider to a github instance.
         *
         * Only needs to be implemented by dynamic credentials providers that use a github instance in order to refresh.
         *
         * @param github
         *            The github instance to be used for refreshing dynamic credentials
         */
        synchronized void bind(GitHub github) {
            if (baseGitHub != null) {
                throw new IllegalStateException("Already bound to another GitHub instance.");
            }
            this.baseGitHub = github;
        }
    }

    private static class AuthorizationRefreshGitHubWrapper extends GitHub {

        private final AuthorizationProvider authorizationProvider;

        AuthorizationRefreshGitHubWrapper(GitHub github, AuthorizationProvider authorizationProvider) {
            super(github.client);
            this.authorizationProvider = authorizationProvider;

            // no dependent authorization providers nest like this currently, but they might in future
            if (authorizationProvider instanceof DependentAuthorizationProvider) {
                ((DependentAuthorizationProvider) authorizationProvider).bind(this);
            }
        }

        @Nonnull
        @Override
        Requester createRequest() {
            try {
                // Override
                return super.createRequest().setHeader("Authorization", authorizationProvider.getEncodedAuthorization())
                        .rateLimit(RateLimitTarget.NONE);
            } catch (IOException e) {
                throw new GHException("Failed to create requester to refresh credentials", e);
            }
        }
    }

    private static class LoginLoadingUserAuthorizationProvider implements UserAuthorizationProvider {
        private final AuthorizationProvider authorizationProvider;
        private final GitHub gitHub;
        private String login;
        private boolean loginLoaded = false;

        LoginLoadingUserAuthorizationProvider(AuthorizationProvider authorizationProvider, GitHub gitHub) {
            this.gitHub = gitHub;
            this.authorizationProvider = authorizationProvider;
        }

        @Override
        public String getEncodedAuthorization() throws IOException {
            return authorizationProvider.getEncodedAuthorization();
        }

        @Override
        public String getLogin() {
            synchronized (this) {
                if (!loginLoaded) {
                    loginLoaded = true;
                    try {
                        GHMyself u = gitHub.setMyself();
                        if (u != null) {
                            login = u.getLogin();
                        }
                    } catch (IOException e) {
                    }
                }
                return login;
            }
        }
    }
    private static final Logger LOGGER = Logger.getLogger(GitHub.class.getName());

    /**
     * Obtains the credential from "~/.github" or from the System Environment Properties.
     *
     * @return the git hub
     * @throws IOException
     *             the io exception
     */
    public static GitHub connect() throws IOException {
        return GitHubBuilder.fromCredentials().build();
    }

    /**
     * Connect git hub.
     *
     * @param login
     *            the login
     * @param oauthAccessToken
     *            the oauth access token
     * @return the git hub
     * @throws IOException
     *             the io exception
     */
    public static GitHub connect(String login, String oauthAccessToken) throws IOException {
        return new GitHubBuilder().withOAuthToken(oauthAccessToken, login).build();
    }

    /**
     * Connects to GitHub anonymously.
     * <p>
     * All operations that require authentication will fail.
     *
     * @return the git hub
     * @throws IOException
     *             the io exception
     */
    public static GitHub connectAnonymously() throws IOException {
        return new GitHubBuilder().build();
    }

    /**
     * Connects to GitHub Enterprise anonymously.
     * <p>
     * All operations that require authentication will fail.
     *
     * @param apiUrl
     *            the api url
     * @return the git hub
     * @throws IOException
     *             the io exception
     */
    public static GitHub connectToEnterpriseAnonymously(String apiUrl) throws IOException {
        return new GitHubBuilder().withEndpoint(apiUrl).build();
    }

    /**
     * Version that connects to GitHub Enterprise.
     *
     * @param apiUrl
     *            The URL of GitHub (or GitHub Enterprise) API endpoint, such as "https://api.github.com" or
     *            "http://ghe.acme.com/api/v3". Note that GitHub Enterprise has <code>/api/v3</code> in the URL. For
     *            historical reasons, this parameter still accepts the bare domain name, but that's considered
     *            deprecated.
     * @param login
     *            the login
     * @param oauthAccessToken
     *            the oauth access token
     * @return the git hub
     * @throws IOException
     *             the io exception
     */
    public static GitHub connectToEnterpriseWithOAuth(String apiUrl, String login, String oauthAccessToken)
            throws IOException {
        return new GitHubBuilder().withEndpoint(apiUrl).withOAuthToken(oauthAccessToken, login).build();
    }

    /**
     * Connect using o auth git hub.
     *
     * @param oauthAccessToken
     *            the oauth access token
     * @return the git hub
     * @throws IOException
     *             the io exception
     */
    public static GitHub connectUsingOAuth(String oauthAccessToken) throws IOException {
        return new GitHubBuilder().withOAuthToken(oauthAccessToken).build();
    }

    /**
     * Connect using o auth git hub.
     *
     * @param githubServer
     *            the github server
     * @param oauthAccessToken
     *            the oauth access token
     * @return the git hub
     * @throws IOException
     *             the io exception
     */
    public static GitHub connectUsingOAuth(String githubServer, String oauthAccessToken) throws IOException {
        return new GitHubBuilder().withEndpoint(githubServer).withOAuthToken(oauthAccessToken).build();
    }

    /**
     * Gets an {@link ObjectReader} that can be used to convert JSON into library data objects.
     *
     * If you must manually create library data objects from JSON, the {@link ObjectReader} returned by this method is
     * the only supported way of doing so.
     *
     * WARNING: Objects generated from this method have limited functionality. They will not throw when being crated
     * from valid JSON matching the expected object, but they are not guaranteed to be usable beyond that. Use with
     * extreme caution.
     *
     * @return an {@link ObjectReader} instance that can be further configured.
     */
    @Nonnull
    public static ObjectReader getMappingObjectReader() {
        return GitHubClient.getMappingObjectReader(GitHub.offline());
    }

    /**
     * Gets an {@link ObjectWriter} that can be used to convert data objects in this library to JSON.
     *
     * If you must convert data object in this library to JSON, the {@link ObjectWriter} returned by this method is the
     * only supported way of doing so. This {@link ObjectWriter} can be used to convert any library data object to JSON
     * without throwing an exception.
     *
     * WARNING: While the JSON generated is generally expected to be stable, it is not part of the API of this library
     * and may change without warning. Use with extreme caution.
     *
     * @return an {@link ObjectWriter} instance that can be further configured.
     */
    @Nonnull
    public static ObjectWriter getMappingObjectWriter() {
        return GitHubClient.getMappingObjectWriter();
    }

    /**
     * An offline-only {@link GitHub} useful for parsing event notification from an unknown source.
     * <p>
     * All operations that require a connection will fail.
     *
     * @return An offline-only {@link GitHub}.
     */
    public static GitHub offline() {
        try {
            return new GitHubBuilder().withEndpoint("https://api.github.invalid")
                    .withConnector(GitHubConnector.OFFLINE)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("The offline implementation constructor should not connect", e);
        }
    }

    @Nonnull
    private final GitHubClient client;

    @CheckForNull
    private GHMyself myself;

    private GitHub(GitHubClient client) {        
        this.client = client;
    }
    
    /**
     * Creates a client API root object.
     *
     * <p>
     * Several different combinations of the login/oauthAccessToken/password parameters are allowed to represent
     * different ways of authentication.
     *
     * <dl>
     * <dt>Log in anonymously
     * <dd>Leave all three parameters null and you will be making HTTP requests without any authentication.
     *
     * <dt>Log in with password
     * <dd>Specify the login and password, then leave oauthAccessToken null. This will use the HTTP BASIC auth with the
     * GitHub API.
     *
     * <dt>Log in with OAuth token
     * <dd>Specify oauthAccessToken, and optionally specify the login. Leave password null. This will send OAuth token
     * to the GitHub API. If the login parameter is null, The constructor makes an API call to figure out the user name
     * that owns the token.
     *
     * <dt>Log in with JWT token
     * <dd>Specify jwtToken. Leave password null. This will send JWT token to the GitHub API via the Authorization HTTP
     * header. Please note that only operations in which permissions have been previously configured and accepted during
     * the GitHub App will be executed successfully.
     * </dl>
     *
     * @param apiUrl
     *            The URL of GitHub (or GitHub enterprise) API endpoint, such as "https://api.github.com" or
     *            "http://ghe.acme.com/api/v3". Note that GitHub Enterprise has <code>/api/v3</code> in the URL. For
     *            historical reasons, this parameter still accepts the bare domain name, but that's considered
     *            deprecated.
     * @param connector
     *            a connector
     * @param rateLimitHandler
     *            rateLimitHandler
     * @param abuseLimitHandler
     *            abuseLimitHandler
     * @param rateLimitChecker
     *            rateLimitChecker
     * @param authorizationProvider
     *            a authorization provider
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressFBWarnings(value = { "CT_CONSTRUCTOR_THROW" }, justification = "internal constructor")
    GitHub(String apiUrl,
            GitHubConnector connector,
            GitHubRateLimitHandler rateLimitHandler,
            GitHubAbuseLimitHandler abuseLimitHandler,
            GitHubRateLimitChecker rateLimitChecker,
            AuthorizationProvider authorizationProvider) throws IOException {
        if (authorizationProvider instanceof DependentAuthorizationProvider) {
            ((DependentAuthorizationProvider) authorizationProvider).bind(this);
        } else if (authorizationProvider instanceof ImmutableAuthorizationProvider
                && authorizationProvider instanceof UserAuthorizationProvider) {
            UserAuthorizationProvider provider = (UserAuthorizationProvider) authorizationProvider;
            if (provider.getLogin() == null && provider.getEncodedAuthorization() != null
                    && provider.getEncodedAuthorization().startsWith("token")) {
                authorizationProvider = new LoginLoadingUserAuthorizationProvider(provider, this);
            }
        }

       

        this.client = new GitHubClient(apiUrl,
                connector,
                rateLimitHandler,
                abuseLimitHandler,
                rateLimitChecker,
                authorizationProvider);

        // Ensure we have the login if it is available
        // This preserves previously existing behavior. Consider removing in future.
        if (authorizationProvider instanceof LoginLoadingUserAuthorizationProvider) {
            ((LoginLoadingUserAuthorizationProvider) authorizationProvider).getLogin();
        }
    }

    /**
     * Tests the connection.
     *
     * <p>
     * Verify that the API URL and credentials are valid to access this GitHub.
     *
     * <p>
     * This method returns normally if the endpoint is reachable and verified to be GitHub API URL. Otherwise this
     * method throws {@link IOException} to indicate the problem.
     *
     * @throws IOException
     *             the io exception
     */
    public void checkApiUrlValidity() throws IOException {
        client.checkApiUrlValidity();
    }

    /**
     * Check auth gh authorization.
     *
     * @param clientId
     *            the client id
     * @param accessToken
     *            the access token
     * @return the gh authorization
     * @throws IOException
     *             the io exception
     * @see <a href="https://developer.github.com/v3/oauth_authorizations/#check-an-authorization">Check an
     *      authorization</a>
     */
    public GHAuthorization checkAuth(@Nonnull String clientId, @Nonnull String accessToken) throws IOException {
        return createRequest().withUrlPath("/applications/" + clientId + "/tokens/" + accessToken)
                .fetch(GHAuthorization.class);
    }

    

    /**
     * Create or get auth gh authorization.
     *
     * @param clientId
     *            the client id
     * @param clientSecret
     *            the client secret
     * @param scopes
     *            the scopes
     * @param note
     *            the note
     * @param noteUrl
     *            the note url
     * @return the gh authorization
     * @throws IOException
     *             the io exception
     * @see <a href=
     *      "https://developer.github.com/v3/oauth_authorizations/#get-or-create-an-authorization-for-a-specific-app">docs</a>
     */
    public GHAuthorization createOrGetAuth(String clientId,
            String clientSecret,
            List<String> scopes,
            String note,
            String noteUrl) throws IOException {
        Requester requester = createRequest().with("client_secret", clientSecret)
                .with("scopes", scopes)
                .with("note", note)
                .with("note_url", noteUrl);

        return requester.method("PUT").withUrlPath("/authorizations/clients/" + clientId).fetch(GHAuthorization.class);
    }

    
    /**
     * Creates a new authorization.
     * <p>
     * The token created can be then used for {@link GitHub#connectUsingOAuth(String)} in the future.
     *
     * @param scope
     *            the scope
     * @param note
     *            the note
     * @param noteUrl
     *            the note url
     * @return the gh authorization
     * @throws IOException
     *             the io exception
     * @see <a href="http://developer.github.com/v3/oauth/#create-a-new-authorization">Documentation</a>
     */
    public GHAuthorization createToken(Collection<String> scope, String note, String noteUrl) throws IOException {
        Requester requester = createRequest().with("scopes", scope).with("note", note).with("note_url", noteUrl);

        return requester.method("POST").withUrlPath("/authorizations").fetch(GHAuthorization.class);
    }

    /**
     * Creates a new authorization using an OTP.
     * <p>
     * Start by running createToken, if exception is thrown, prompt for OTP from user
     * <p>
     * Once OTP is received, call this token request
     * <p>
     * The token created can be then used for {@link GitHub#connectUsingOAuth(String)} in the future.
     *
     * @param scope
     *            the scope
     * @param note
     *            the note
     * @param noteUrl
     *            the note url
     * @param OTP
     *            the otp
     * @return the gh authorization
     * @throws IOException
     *             the io exception
     * @see <a href="http://developer.github.com/v3/oauth/#create-a-new-authorization">Documentation</a>
     */
    public GHAuthorization createToken(Collection<String> scope, String note, String noteUrl, Supplier<String> OTP)
            throws IOException {
        try {
            return createToken(scope, note, noteUrl);
        } catch (GHOTPRequiredException ex) {
            String OTPstring = OTP.get();
            Requester requester = createRequest().with("scopes", scope).with("note", note).with("note_url", noteUrl);
            // Add the OTP from the user
            requester.setHeader("x-github-otp", OTPstring);
            return requester.method("POST").withUrlPath("/authorizations").fetch(GHAuthorization.class);
        }
    }

    /**
     * Delete auth.
     *
     * @param id
     *            the id
     * @throws IOException
     *             the io exception
     * @see <a href="https://developer.github.com/v3/oauth_authorizations/#delete-an-authorization">Delete an
     *      authorization</a>
     */
    public void deleteAuth(long id) throws IOException {
        createRequest().method("DELETE").withUrlPath("/authorizations/" + id).send();
    }

    /**
     * Gets api url.
     *
     * @return the api url
     */
    public String getApiUrl() {
        return client.getApiUrl();
    }

    

    /**
     * Gets the current full rate limit information from the server.
     *
     * For some versions of GitHub Enterprise, the {@code /rate_limit} endpoint returns a {@code 404 Not Found}. In that
     * case, the most recent {@link GHRateLimit} information will be returned, including rate limit information returned
     * in the response header for this request in if was present.
     *
     * For most use cases it would be better to implement a {@link RateLimitChecker} and add it via
     * {@link GitHubBuilder#withRateLimitChecker(RateLimitChecker)}.
     *
     * @return the rate limit
     * @throws IOException
     *             the io exception
     */
    @Nonnull
    public GHRateLimit getRateLimit() throws IOException {
        return client.getRateLimit();
    }
    
    public JsonNode getRawRepository(String repoOwner, String repoName) throws IOException {
        return this.createRequest().withUrlPath("/repos/" + repoOwner + '/' + repoName).fetch(JsonNode.class);
    }

    public JsonNode getRawIssue(int number, String repoOwner, String repoName) throws IOException {
        return this.createRequest().withUrlPath("/repos/" + repoOwner + '/' + repoName+ "/issues/" + number).fetch(JsonNode.class);
    }
     
    /**
     * Is this an anonymous connection.
     *
     * @return {@code true} if operations that require authentication will fail.
     */
    public boolean isAnonymous() {
        return client.isAnonymous();
    }

    /**
     * Ensures that the credential is valid.
     *
     * @return the boolean
     */
    public boolean isCredentialValid() {
        return client.isCredentialValid();
    }

    /**
     * Is this an always offline "connection".
     *
     * @return {@code true} if this is an always offline "connection".
     */
    public boolean isOffline() {
        return client.isOffline();
    }

    /**
     * Returns the most recently observed rate limit data or {@code null} if either there is no rate limit (for example
     * GitHub Enterprise) or if no requests have been made.
     *
     * @return the most recently observed rate limit data or {@code null}.
     * @deprecated implement a {@link RateLimitChecker} and add it via
     *             {@link GitHubBuilder#withRateLimitChecker(RateLimitChecker)}.
     */
    @Nonnull
    @Deprecated
    public GHRateLimit lastRateLimit() {
        return client.lastRateLimit();
    }

    
    /**
     * Returns a list of all authorizations.
     *
     * @return the paged iterable
     * @see <a href="https://developer.github.com/v3/oauth_authorizations/#list-your-authorizations">List your
     *      authorizations</a>
     */
    public PagedIterable<GHAuthorization> listMyAuthorizations() {
        return createRequest().withUrlPath("/authorizations").toIterable(GHAuthorization[].class, null);
    }


   

    /**
     * Gets the current rate limit while trying not to actually make any remote requests unless absolutely necessary.
     *
     * @return the current rate limit data.
     * @throws IOException
     *             if we couldn't get the current rate limit data.
     * @deprecated implement a {@link RateLimitChecker} and add it via
     *             {@link GitHubBuilder#withRateLimitChecker(RateLimitChecker)}.
     */
    @Nonnull
    @Deprecated
    public GHRateLimit rateLimit() throws IOException {
        return client.rateLimit(RateLimitTarget.CORE);
    }

    /**
     * Render a Markdown document in raw mode.
     *
     * <p>
     * It takes a Markdown document as plaintext and renders it as plain Markdown without a repository context (just
     * like a README.md file is rendered – this is the simplest way to preview a readme online).
     *
     * @param text
     *            the text
     * @return the reader
     * @throws IOException
     *             the io exception
     * @see GHRepository#renderMarkdown(String, MarkdownMode) GHRepository#renderMarkdown(String, MarkdownMode)
     */
    public Reader renderMarkdown(String text) throws IOException {
        return new InputStreamReader(
                createRequest().method("POST")
                        .with(new ByteArrayInputStream(text.getBytes("UTF-8")))
                        .contentType("text/plain;charset=UTF-8")
                        .withUrlPath("/markdown/raw")
                        .fetchStream(Requester::copyInputStream),
                "UTF-8");
    }

    /**
     * Reset auth gh authorization.
     *
     * @param clientId
     *            the client id
     * @param accessToken
     *            the access token
     * @return the gh authorization
     * @throws IOException
     *             the io exception
     * @see <a href="https://developer.github.com/v3/oauth_authorizations/#reset-an-authorization">Reset an
     *      authorization</a>
     */
    public GHAuthorization resetAuth(@Nonnull String clientId, @Nonnull String accessToken) throws IOException {
        return createRequest().method("POST")
                .withUrlPath("/applications/" + clientId + "/tokens/" + accessToken)
                .fetch(GHAuthorization.class);
    }

    /**
     * Creates a request to GitHub GraphQL API.
     *
     * @param query
     *            the query for the GraphQL
     * @return the requester
     */
    @Nonnull
    Requester createGraphQLRequest(String query) {
        return createRequest().method("POST")
                .rateLimit(RateLimitTarget.GRAPHQL)
                .with("query", query)
                .withUrlPath("/graphql");
    }
    
    
    @Nonnull
    public JsonNode queryGraphQL(String query) throws IOException {
    	return createGraphQLRequest(query).fetch(JsonNode.class);
    }

    /**
     * Creates the request.
     *
     * @return the requester
     */
    @Nonnull
    Requester createRequest() {
        Requester requester = new Requester(client);
        requester.injectMappingValue(this);
        if (!this.getClass().equals(GitHub.class)) {
            // For classes that extend GitHub, treat them still as a GitHub instance
            requester.injectMappingValue(GitHub.class.getName(), this);
        }
        return requester;
    }

    public GHMyself setMyself() throws IOException {
        synchronized (this) {
            if (this.myself == null) {
                this.myself = createRequest().withUrlPath("/user").fetch(GHMyself.class);
            }
            return myself;
        }
    }
    
    /**
     * Gets the client.
     *
     * @return the client
     */
    @Nonnull
    GitHubClient getClient() {
        return client;
    }

}
