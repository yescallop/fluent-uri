package cn.yescallop.fluenturi;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Immutable URI reference which conforms to
 * <a href="https://www.ietf.org/rfc/rfc3986.html">RFC 3986</a>.
 *
 * @author Scallop Ye
 */
public interface Uri {

    /**
     * Creates a Uri from the given encoded URI-reference string.
     *
     * @param str an RFC 3986-compliant, encoded URI-reference, as defined in Section 4.1
     * @throws UriSyntaxException if the input string violates RFC 3986.
     */
    static Uri from(String str) {
        return new UriImpl(str);
    }

    /**
     * Creates a new empty builder.
     */
    static Builder newBuilder() {
        return new UriBuilderImpl();
    }

    /**
     * Creates a new builder, copying the attributes from this Uri.
     */
    Builder asBuilder();

    /**
     * Gets the scheme of this Uri.
     *
     * @return the scheme, or null if this is a relative URI
     */
    String scheme();

    /**
     * Gets the decoded user information.
     *
     * @return the user info, or null if not present
     */
    String userInfo();

    /**
     * Gets the encoded user information.
     *
     * @return the user info, or null if not present
     */
    String encodedUserInfo();

    /**
     * Gets the decoded host.
     *
     * @return the host, or null if not present
     */
    String host();

    /**
     * Gets the encoded host.
     *
     * @return the host, or null if not present
     */
    String encodedHost();

    /**
     * Gets the port.
     *
     * @return the port, or -1 if not present
     */
    int port();

    /**
     * Gets the decoded path.
     * <p>
     * An encoded slash "%2F" would be decoded as "/".
     *
     * @return the path
     */
    String path();

    /**
     * Gets the decoded path segments.
     * <p>
     * A segment might contain "/" decoded from "%2F".
     *
     * @return the path segments, each without a leading or trailing "/"
     */
    List<String> pathSegments();

    /**
     * Gets the encoded path.
     *
     * @return the path
     */
    String encodedPath();

    /**
     * Gets the encoded query.
     *
     * @return the query, or null if not present
     */
    String encodedQuery();

    /**
     * Gets the decoded query parameters.
     * <p>
     * Empty parameters are omitted, e.g. "&".
     * Empty parameter names or values are allowed, e.g. "=".
     *
     * @return the query parameters, or null if query is not present
     */
    Map<String, List<String>> queryParameters();

    /**
     * Gets the decoded fragment.
     *
     * @return the fragment, or null if not present
     */
    String fragment();

    /**
     * Gets the encoded fragment.
     *
     * @return the fragment, or null if not present
     */
    String encodedFragment();

    /**
     * Normalizes the path of this Uri.
     *
     * @return this Uri if the path needs no normalization,
     * or a new Uri with normalized path.
     */
    Uri normalize();

    /**
     * Resolves the given Uri against this Uri.
     *
     * @param uri a Uri
     * @return the target URI
     */
    Uri resolve(Uri uri);

    /**
     * Resolves the given URI-reference string against this Uri.
     *
     * @param uriStr a URI-reference
     * @return the target URI
     * @throws UriSyntaxException if the input string violates RFC 3986.
     */
    Uri resolve(String uriStr);

    /**
     * Tells whether this Uri is relative, that is,
     * the scheme is not present.
     */
    boolean isRelative();

    /**
     * Converts this Uri into {@link URI}.
     */
    URI toURI();

    /**
     * A builder of {@link Uri}.
     */
    interface Builder {

        /**
         * Encodes and sets the scheme.
         *
         * @param scheme the scheme or null
         * @return this builder
         */
        Builder scheme(String scheme);

        /**
         * Encodes and sets the user information.
         *
         * @param userInfo the user info or null
         * @return this builder
         */
        Builder userInfo(String userInfo);

        /**
         * Sets the encoded user information.
         *
         * @param encodedUserInfo the encoded user info or null
         * @return this builder.
         */
        Builder encodedUserInfo(String encodedUserInfo);

        /**
         * Encodes and sets the host.
         *
         * @param host the host or null
         * @return this builder
         */
        Builder host(String host);

        /**
         * Sets the encoding option for host.
         * <p>
         * The default value is {@link HostEncodingOption#DNS_COMPLIANT}.
         *
         * @param option the encoding option
         * @return this builder
         */
        Builder hostEncodingOption(HostEncodingOption option);

        /**
         * Sets the encoded host.
         *
         * @param encodedHost the encoded host or null
         * @return this builder
         */
        Builder encodedHost(String encodedHost);

        /**
         * Sets the port.
         *
         * @param port the port or -1 if not present
         * @return this builder
         */
        Builder port(int port);

        /**
         * Encodes and sets the path.
         *
         * @param path the path
         * @return this builder.
         */
        Builder path(String path);

        /**
         * Encodes and appends the given path segment to the current path.
         * <p>
         * If the given path segment is empty, a slash "/" is appended.
         * <p>
         * If not, a slash "/" would be appended if the current path is
         * not empty and does not end with slash "/", and then the given
         * path segment is appended.
         *
         * @param segment a path segment
         * @return this builder
         */
        Builder appendPathSegment(String segment);

        /**
         * Sets the encoded path.
         *
         * @param encodedPath the encoded path
         * @return this builder
         * @throws IllegalStateException if the path has already been appended to
         */
        Builder encodedPath(String encodedPath);

        /**
         * Encodes and appends the given query parameter pair to the current query.
         *
         * @param name name of the parameter
         * @param value value of the parameter
         * @return this builder
         */
        Builder appendQueryParameter(String name, String value);

        /**
         * Sets the encoded query.
         *
         * @param encodedQuery the encoded query or null
         * @return this builder
         * @throws IllegalStateException if the query has already been appended to
         */
        Builder encodedQuery(String encodedQuery);

        /**
         * Clears the current query.
         *
         * @return this builder
         * @throws IllegalStateException if the query has already been appended to
         */
        Builder clearQuery();

        /**
         * Encodes and sets the fragment.
         *
         * @param fragment the fragment or null
         * @return this builder
         */
        Builder fragment(String fragment);

        /**
         * Sets the encoded fragment.
         *
         * @param encodedFragment the encoded fragment or null
         * @return this builder
         */
        Builder encodedFragment(String encodedFragment);

        /**
         * Builds the Uri.
         */
        Uri build();
    }

    /**
     * Options for encoding a host.
     */
    enum HostEncodingOption {
        /**
         * Encodes the host with percent-encoded octets.
         */
        PERCENT_ENCODED,
        /**
         * Encodes the host with IDNA encoding described in
         * <a href="https://www.ietf.org/rfc/rfc3490.html">RFC 3490</a>,
         * using the method in {@link java.net.IDN}, and checks whether
         * the encoded hostname is compliant with DNS.
         */
        DNS_COMPLIANT
    }
}
