package org.kohsuke.github;

// TODO: Auto-generated Javadoc
/**
 * Represents the account that's logging into GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
public class GHMyself extends GHUser {

    /**
     * Type of repositories returned during listing.
     */
    public enum RepositoryListFilter {

        /** All public and private repositories that current user has access or collaborates to. */
        ALL,

        /** Public and private repositories that current user is a member. */
        MEMBER,

        /** Public and private repositories owned by current user. */
        OWNER,

        /** Private repositories that current user has access or collaborates to. */
        PRIVATE,

        /** Public repositories that current user has access or collaborates to. */
        PUBLIC;
    }

    /**
     * Create default GHMyself instance
     */
    public GHMyself() {
    }



}

