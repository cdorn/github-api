package org.kohsuke.github;

import com.fasterxml.jackson.databind.JsonNode;

// TODO: Auto-generated Javadoc
/**
 * The Class GHIssueQueryBuilder.
 */
public abstract class GHIssueDependencyBuilder extends GHQueryBuilder<JsonNode> {

	
	public static class ForAbsolutelyAllInRepo extends GHIssueDependencyBuilder {

        public ForAbsolutelyAllInRepo(GitHub root, String owner, String repoName) {
            super(root, owner, repoName, -1);
        }
      
        @Override
        public String getApiUrl() {
            return "/repos/" + owner + '/' + repoName+ "/issues";
        }      
    }
	
    public static class ForBlocking extends GHIssueDependencyBuilder {

        public ForBlocking(GitHub root, String owner, String repoName, int number) {
            super(root, owner, repoName, number);
        }
      
        @Override
        public String getApiUrl() {
            return "/repos/" + owner + '/' + repoName+ "/issues/" + number + "/dependencies/blocking";
        }      
    }


    public static class ForBlockedBy extends GHIssueDependencyBuilder {

        public ForBlockedBy(GitHub root, String owner, String repoName, int number) {
            super(root, owner, repoName, number);
        }
      
        @Override
        public String getApiUrl() {
            return "/repos/" + owner + '/' + repoName+ "/issues/" + number + "/dependencies/blocked_by";
        }      
    }
    
    public static class ForSubissues extends GHIssueDependencyBuilder {

        public ForSubissues(GitHub root, String owner, String repoName, int number) {
            super(root, owner, repoName, number);
        }
      
        @Override
        public String getApiUrl() {
            return "/repos/" + owner + '/' + repoName+ "/issues/" + number + "/sub_issues";
        }      
    }
    /**
     * Instantiates a new GH issue query builder.
     *
     * @param root
     *            the root
     */
    GHIssueDependencyBuilder(GitHub root, String owner, String repoName, int number) {
    	super(root);
		this.owner = owner;
    	this.repoName = repoName;
		this.number = number;
		
    }

    protected final String owner;
    protected final String repoName;
    protected final int number;

    /**
     * Gets the api url.
     *
     * @return the api url
     */
    public abstract String getApiUrl();

    /**
     * Page size gh issue query builder.
     *
     * @param pageSize
     *            the page size
     * @return the gh issue query builder
     */
    public GHIssueDependencyBuilder pageSize(int pageSize) {
        req.with("per_page", pageSize);
        return this;
    }

    /**
     * List.
     *
     * @return the paged iterable
     */
    @Override
    public PagedIterable<JsonNode> list() {
        return req.withUrlPath(getApiUrl()).toIterable(JsonNode[].class, null);
    }
}
