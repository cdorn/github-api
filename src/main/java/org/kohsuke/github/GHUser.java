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


import java.io.IOException;
import java.util.Optional;

// TODO: Auto-generated Javadoc
/**
 * Represents an user of GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
public class GHUser extends GHPerson {

    /** The suspendedAt */
    private String suspendedAt;

    /** The ldap dn. */
    protected String ldapDn;

    /**
     * Create default GHUser instance
     */
    public GHUser() {
    }

    /**
     * Equals.
     *
     * @param obj
     *            the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GHUser) {
            GHUser that = (GHUser) obj;
            return this.login.equals(that.login);
        }
        return false;
    }

    /**
     * Follow this user.
     *
     * @throws IOException
     *             the io exception
     */
    public void follow() throws IOException {
        root().createRequest().method("PUT").withUrlPath("/user/following/" + login).send();
    }

    /**
     * Gets the bio.
     *
     * @return the bio
     */
    public String getBio() {
        return bio;
    }

    

    /**
     * Gets LDAP information for user.
     *
     * @return The LDAP information
     * @throws IOException
     *             the io exception
     * @see <a href=
     *      "https://docs.github.com/en/enterprise-server@3.3/admin/identity-and-access-management/authenticating-users-for-your-github-enterprise-server-instance/using-ldap">Github
     *      LDAP</a>
     */
    public Optional<String> getLdapDn() throws IOException {
        super.populate();
        return Optional.ofNullable(ldapDn);
    }

    
    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return login.hashCode();
    }

    /**
     * Returns true if this user is marked as hireable, false otherwise.
     *
     * @return if the user is marked as hireable
     */
    public boolean isHireable() {
        return hireable;
    }

    /**
     * Lists the users who are following this user.
     *
     * @return the paged iterable
     */
    public PagedIterable<GHUser> listFollowers() {
        return listUser("followers");
    }

    /**
     * Lists the users that this user is following.
     *
     * @return the paged iterable
     */
    public PagedIterable<GHUser> listFollows() {
        return listUser("following");
    }

    /**
     * Unfollow this user.
     *
     * @throws IOException
     *             the io exception
     */
    public void unfollow() throws IOException {
        root().createRequest().method("DELETE").withUrlPath("/user/following/" + login).send();
    }


    private PagedIterable<GHUser> listUser(final String suffix) {
        return root().createRequest().withUrlPath(getApiTailUrl(suffix)).toIterable(GHUser[].class, null);
    }

    /**
     * Gets the api tail url.
     *
     * @param tail
     *            the tail
     * @return the api tail url
     */
    String getApiTailUrl(String tail) {
        if (tail.length() > 0 && !tail.startsWith("/"))
            tail = '/' + tail;
        return "/users/" + login + tail;
    }
}
