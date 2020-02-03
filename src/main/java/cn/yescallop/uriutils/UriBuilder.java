package cn.yescallop.uriutils;

/**
 * @author Scallop Ye
 */
public interface UriBuilder {

    static UriBuilder newBuilder() {
        // TODO
        return null;
    }

    UriBuilder scheme(String scheme);

    UriBuilder userInfo(String userInfo);

    UriBuilder encodedUserInfo(String encodedUserInfo);

    UriBuilder host(String host);

    /**
     * Marks the host as DNS-friendly, that is,
     * the host will be encoded with {@link java.net.IDN#toASCII(String)},
     * and an exception will be thrown if the host does not match the
     * syntax required by DNS.
     * <p>
     * This mark will also be applied if the scheme needs DNS resolution
     * for the host, e.g. "http", "https", "ftp".
     *
     * @return this builder.
     */
    UriBuilder dnsFriendly();

    UriBuilder port(int port);

    UriBuilder path(String path);

    UriBuilder appendPathSegment(String segment);

    UriBuilder encodedPath(String encodedPath);

    UriBuilder appendQueryParameter(String name, String value);

    UriBuilder encodedQuery(String encodedQuery);

    UriBuilder clearQuery();

    UriBuilder fragment(String fragment);

    UriBuilder encodedFragment(String encodedFragment);

    Uri build();

}
