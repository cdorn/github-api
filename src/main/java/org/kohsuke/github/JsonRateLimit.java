package org.kohsuke.github;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: Auto-generated Javadoc
/**
 * The Class JsonRateLimit.
 *
 * @author Kohsuke Kawaguchi
 */
class JsonRateLimit {

    /** The resources. */
    @Nonnull
    final GHRateLimit resources;

    @JsonCreator
    private JsonRateLimit(@Nonnull @JsonProperty(value = "resources", required = true) GHRateLimit resources) {
        Objects.requireNonNull(resources);
        this.resources = resources;
    }
}
