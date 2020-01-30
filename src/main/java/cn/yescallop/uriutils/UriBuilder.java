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

    UriBuilder host(String host);

    UriBuilder port(int port);

    UriBuilder path(String path);

    UriBuilder appendPathSegment(String segment);

    UriBuilder appendQueryParameter(String name, String value);

    UriBuilder clearQuery();

    UriBuilder fragment(String fragment);

    Uri build();

}
