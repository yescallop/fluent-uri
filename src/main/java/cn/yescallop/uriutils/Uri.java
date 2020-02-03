package cn.yescallop.uriutils;

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
     * Creates a Uri from the given encoded URI string.
     *
     * @param str an RFC 3986-compliant, encoded URI
     */
    static Uri from(String str) {
        try {
            return new UriImpl(str);
        } catch (UriSyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Creates a new builder, copying the attributes from this Uri.
     */
    UriBuilder asBuilder();

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
     *
     * @return the path, or null if an encoded slash "%2F" is found
     */
    String path();

    /**
     * Gets the decoded path segments.
     * <p>
     * Empty segments are omitted, e.g. "//".
     * Segment might contain "/" decoded from "%2F".
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

    Uri normalize();

    Uri resolve(Uri uri);

    Uri relativize(Uri uri);

    /**
     * Tells whether this Uri is opaque, that is,
     * the scheme is present, the authority is not
     * present, and the path does not start with "/".
     */
    boolean opaque();

    /**
     * Tells whether this Uri is relative, that is,
     * the scheme is not present.
     */
    boolean relative();

    /**
     * Converts this Uri into {@link URI}.
     */
    URI toURI();
}
