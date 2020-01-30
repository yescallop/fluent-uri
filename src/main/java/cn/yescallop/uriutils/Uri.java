package cn.yescallop.uriutils;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Scallop Ye
 */
public interface Uri {

    static Uri from(String str) {
        // TODO
        return null;
    }

    UriBuilder asBuilder();

    String scheme();

    String schemeSpecificPart();

    String encodedSchemeSpecificPart();

    String authority();

    String encodedAuthority();

    String userInfo();

    String encodedUserInfo();

    String host();

    String encodedHost();

    int port();

    String path();

    List<String> pathSegments();

    String encodedPath();

    String query();

    String encodedQuery();

    List<String> queryParameters(String name);

    Map<String, List<String>> queryParameters();

    String fragment();

    String encodedFragment();

    Uri normalize();

    boolean opaque();

    boolean absolute();

    URI toURI();
}
