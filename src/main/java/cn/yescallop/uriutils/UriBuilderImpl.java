package cn.yescallop.uriutils;

import cn.yescallop.uriutils.Uri.HostEncodingOption;

import java.util.Objects;

import static cn.yescallop.uriutils.CharUtils.*;

/**
 * Implementation of {@link Uri.Builder}.
 *
 * @author Scallop Ye
 */
class UriBuilderImpl implements Uri.Builder {

    String scheme;
    String userInfo;
    String host;
    String encodedHost;
    HostEncodingOption hostEncodingOption = HostEncodingOption.DNS_COMPATIBLE;
    int port = -1;
    String path = "";
    StringBuilder pathBuilder;
    String query;
    StringBuilder queryBuilder;
    String fragment;

    UriBuilderImpl() {
        // package-private access
    }

    @Override
    public Uri.Builder scheme(String scheme) {
        if (scheme != null) {
            if (scheme.isEmpty())
                throw new IllegalArgumentException("Empty scheme");
            checkChar(scheme, 0, L_ALPHA, H_ALPHA, "scheme");
            checkChars(scheme, 1, scheme.length(), L_SCHEME, H_SCHEME, "scheme");
        }
        this.scheme = scheme;
        return this;
    }

    @Override
    public Uri.Builder userInfo(String userInfo) {
        if (userInfo != null)
            userInfo = encode(userInfo, L_USERINFO, H_USERINFO);
        this.userInfo = userInfo;
        return this;
    }

    @Override
    public Uri.Builder encodedUserInfo(String encodedUserInfo) {
        if (encodedUserInfo != null)
            checkChars(encodedUserInfo, L_USERINFO, H_USERINFO, "userinfo");
        userInfo = encodedUserInfo;
        return this;
    }

    @Override
    public Uri.Builder host(String host) {
        this.host = host;
        encodedHost = null;
        return this;
    }

    @Override
    public Uri.Builder hostEncodingOption(HostEncodingOption option) {
        hostEncodingOption = Objects.requireNonNull(option);
        return this;
    }

    @Override
    public Uri.Builder encodedHost(String encodedHost) {
        if (encodedHost != null) {
            int len = encodedHost.length();
            if (len >= 2 && encodedHost.charAt(0) == '['
                    && encodedHost.charAt(len - 1) == ']') {
                try {
                    checkIpv6Address(encodedHost, 1, len - 1, true);
                } catch (UriSyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                checkChars(encodedHost, L_REG_NAME, H_REG_NAME, "host");
            }
        }
        this.encodedHost = encodedHost;
        host = null;
        return this;
    }

    @Override
    public Uri.Builder port(int port) {
        if (port < -1)
            throw new IllegalArgumentException();
        this.port = port;
        return this;
    }

    @Override
    public Uri.Builder path(String path) {
        if (path != null)
            path = encode(path, L_PATH, H_PATH);
        this.path = path;
        pathBuilder = null;
        return this;
    }

    @Override
    public Uri.Builder appendPathSegment(String segment) {
        Objects.requireNonNull(segment);
        segment = encode(segment, L_PCHAR, H_PCHAR);
        if (pathBuilder == null) {
            int len = path.length();
            pathBuilder = new StringBuilder(len + segment.length() + 16);
            pathBuilder.append(path);
            if (!path.isEmpty() && path.charAt(len - 1) != '/')
                pathBuilder.append('/');
        }
        pathBuilder.append(segment);
        return this;
    }

    @Override
    public Uri.Builder encodedPath(String encodedPath) {
        if (pathBuilder != null)
            throw new IllegalStateException("path already appended to");
        if (encodedPath != null)
            checkChars(encodedPath, L_PATH, H_PATH, "path");
        this.path = encodedPath;
        return this;
    }

    @Override
    public Uri.Builder appendQueryParameter(String name, String value) {
        if (name == null || value == null)
            throw new NullPointerException();
        name = encode(name, L_QUERY_PARAM, H_QUERY_PARAM);
        value = encode(value, L_QUERY_PARAM, H_QUERY_PARAM);
        if (queryBuilder == null) {
            int len = query == null ? 0 : query.length();
            queryBuilder = new StringBuilder(
                    len + name.length() + value.length() + 16);
            if (query != null) queryBuilder.append(query);
        } else {
            queryBuilder.append('&');
        }
        queryBuilder.append(name);
        queryBuilder.append('=');
        queryBuilder.append(value);
        return this;
    }

    @Override
    public Uri.Builder encodedQuery(String encodedQuery) {
        if (queryBuilder != null)
            throw new IllegalStateException("query already appended to");
        if (encodedQuery != null)
            checkChars(encodedQuery, L_QUERY_FRAGMENT, H_QUERY_FRAGMENT, "query");
        query = encodedQuery;
        return this;
    }

    @Override
    public Uri.Builder clearQuery() {
        if (queryBuilder != null)
            throw new IllegalStateException("query already appended to");
        query = null;
        return this;
    }

    @Override
    public Uri.Builder fragment(String fragment) {
        if (fragment != null)
            fragment = encode(fragment, L_QUERY_FRAGMENT, L_QUERY_FRAGMENT);
        this.fragment = fragment;
        return this;
    }

    @Override
    public Uri.Builder encodedFragment(String encodedFragment) {
        if (encodedFragment != null)
            checkChars(encodedFragment, L_QUERY_FRAGMENT, H_QUERY_FRAGMENT, "fragment");
        fragment = encodedFragment;
        return this;
    }

    @Override
    public Uri build() {
        return new UriImpl(this);
    }
}
