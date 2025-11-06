package org.kohsuke.github;


import java.io.IOException;
import java.net.URL;
import java.util.Optional;

// TODO: Auto-generated Javadoc
/**
 * Common part of {@link GHUser} and {@link GHOrganization}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class GHPerson extends GHObject {

    /** The public gists. */
    protected int followers, following, publicRepos, publicGists;

    /** The html url. */
    protected String htmlUrl;

    /** The twitter username. */
    // other fields (that only show up in full data)
    protected String location, blog, email, bio, name, company, type, twitterUsername;

    /** The avatar url. */
    // core data fields that exist even for "small" user data (such as the user info in pull request)
    protected String login, avatarUrl;

    /** The hireable. */
    protected boolean siteAdmin, hireable;

    /** The total private repos. */
    // other fields (that only show up in full data) that require privileged scope
    protected Integer totalPrivateRepos;

    /**
     * Create default GHPerson instance
     */
    public GHPerson() {
    }

    /**
     * Returns a string of the avatar image URL.
     *
     * @return the avatar url
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * Gets the blog URL of this user.
     *
     * @return the blog
     * @throws IOException
     *             the io exception
     */
    public String getBlog() throws IOException {
        populate();
        return blog;
    }

    /**
     * Gets the company name of this user, like "Sun Microsystems, Inc."
     *
     * @return the company
     * @throws IOException
     *             the io exception
     */
    public String getCompany() throws IOException {
        populate();
        return company;
    }



    /**
     * Gets the e-mail address of the user.
     *
     * @return the email
     * @throws IOException
     *             the io exception
     */
    public String getEmail() throws IOException {
        populate();
        return email;
    }

    /**
     * Gets followers count.
     *
     * @return the followers count
     * @throws IOException
     *             the io exception
     */
    public int getFollowersCount() throws IOException {
        populate();
        return followers;
    }

    /**
     * Gets following count.
     *
     * @return the following count
     * @throws IOException
     *             the io exception
     */
    public int getFollowingCount() throws IOException {
        populate();
        return following;
    }

    /**
     * Gets the html url.
     *
     * @return the html url
     */
    public URL getHtmlUrl() {
        return GitHubClient.parseURL(htmlUrl);
    }

    /**
     * Gets the location of this user, like "Santa Clara, California".
     *
     * @return the location
     * @throws IOException
     *             the io exception
     */
    public String getLocation() throws IOException {
        populate();
        return location;
    }

    /**
     * Gets the login ID of this user, like 'kohsuke'.
     *
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Gets the human-readable name of the user, like "Kohsuke Kawaguchi".
     *
     * @return the name
     * @throws IOException
     *             the io exception
     */
    public String getName() throws IOException {
        populate();
        return name;
    }

    /**
     * Gets public gist count.
     *
     * @return the public gist count
     * @throws IOException
     *             the io exception
     */
    public int getPublicGistCount() throws IOException {
        populate();
        return publicGists;
    }

    /**
     * Gets public repo count.
     *
     * @return the public repo count
     * @throws IOException
     *             the io exception
     */
    public int getPublicRepoCount() throws IOException {
        populate();
        return publicRepos;
    }

    /**
     * Gets total private repo count.
     *
     * @return the total private repo count
     * @throws IOException
     *             the io exception
     */
    public Optional<Integer> getTotalPrivateRepoCount() throws IOException {
        populate();
        return Optional.ofNullable(totalPrivateRepos);
    }

    /**
     * Gets the Twitter Username of this user, like "GitHub".
     *
     * @return the Twitter username
     * @throws IOException
     *             the io exception
     */
    public String getTwitterUsername() throws IOException {
        populate();
        return twitterUsername;
    }

    /**
     * Gets the type. This is either "User" or "Organization".
     *
     * @return the type
     * @throws IOException
     *             the io exception
     */
    public String getType() throws IOException {
        if (type == null) {
            populate();
        }
        return type;
    }



    /**
     * Gets the siteAdmin field.
     *
     * @return the siteAdmin field
     * @throws IOException
     *             the io exception
     */
    public boolean isSiteAdmin() throws IOException {
        populate();
        return siteAdmin;
    }

   

    

    /**
     * Fully populate the data by retrieving missing data.
     * <p>
     * Depending on the original API call where this object is created, it may not contain everything.
     *
     * @throws IOException
     *             the io exception
     */
    protected synchronized void populate() throws IOException {
        
        if (isOffline()) {
            return; // cannot populate, will have to live with what we have
        }
        URL url = getUrl();
        if (url != null) {
            root().createRequest().setRawUrlPath(url.toString()).fetchInto(this);
        }
    }
}

