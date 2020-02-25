package cn.yescallop.uriutils;

import java.net.IDN;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.*;

import static cn.yescallop.uriutils.CharUtils.*;

/**
 * Implementation of {@link Uri}.
 *
 * @author Scallop Ye
 */
final class UriImpl implements Uri {

    private String string;

    private String scheme;
    private String encodedUserInfo;
    private String encodedHost;
    private int port = -1;
    private String encodedPath;
    private String encodedQuery;
    private String encodedFragment;

    private String userInfo;
    private String host;
    private String path;
    private String fragment;

    private UriImpl() {
        // private access
    }

    UriImpl(UriBuilderImpl b) {
        scheme = b.scheme;
        encodedPath = b.pathBuilder != null ?
                b.pathBuilder.toString() : b.path;
        if (b.host == null && encodedPath.startsWith("//"))
            // When authority is not present, the path cannot
            // begin with two slash characters ("//") (Section 3).
            throw new IllegalArgumentException("Path begins with // when authority is not present");
        if (!encodedPath.isEmpty()
                && encodedPath.charAt(0) != '/') { // path-rootless
            // When authority is present, the path must
            // either be empty or begin with a slash ("/") character (Section 3).
            if (b.host != null)
                throw new IllegalArgumentException("Path is rootless when authority is present");

            correctNoSchemePath();
        }

        if (b.encodedHost != null) {
            encodedHost = b.encodedHost;
        } else if (b.host != null) {
            host = b.host;
            if (host.indexOf(':') >= 0) {
                try {
                    checkIpv6Address(host, 0, host.length(), false);
                } catch (UriSyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
                int pct = host.indexOf('%');
                if (pct >= 0) { // scoped
                    int len = host.length();
                    String zoneId = encode(host.substring(pct + 1, len), L_ZONE_ID, H_ZONE_ID);
                    StringBuilder sb = new StringBuilder(pct + zoneId.length() + 5);
                    sb.append('[');
                    sb.append(host, 0, pct);
                    sb.append("%25");
                    sb.append(zoneId);
                    sb.append(']');
                    encodedHost = sb.toString();
                } else { // not scoped
                    encodedHost = '[' + host + ']';
                }
            } else {
                boolean dnsCompatible = b.hostEncodingOption == HostEncodingOption.DNS_COMPATIBLE;
                if (dnsCompatible) {
                    encodedHost = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED);
                    checkDnsHost(encodedHost);
                } else {
                    encodedHost = encode(host, L_REG_NAME, H_REG_NAME);
                }
            }
        }
        if (encodedHost != null) {
            encodedUserInfo = b.userInfo;
            port = b.port;
        }
        encodedQuery = b.queryBuilder != null ?
                b.queryBuilder.toString() : b.query;
        encodedFragment = b.fragment;
    }

    UriImpl(String s) throws UriSyntaxException {
        new Parser(s).parse();
    }

    @Override
    public Builder asBuilder() {
        UriBuilderImpl b = new UriBuilderImpl();
        b.scheme = scheme;
        b.userInfo = encodedUserInfo;
        b.encodedHost = encodedHost;
        b.port = port;
        b.path = encodedPath;
        b.query = encodedQuery;
        b.fragment = encodedFragment;
        return b;
    }

    @Override
    public String scheme() {
        return scheme;
    }

    @Override
    public String userInfo() {
        if (userInfo == null && encodedUserInfo != null)
            userInfo = decode(encodedUserInfo);
        return userInfo;
    }

    @Override
    public String encodedUserInfo() {
        return encodedUserInfo;
    }

    @Override
    public String host() {
        if (host == null && encodedHost != null) {
            int len = encodedHost.length();
            if (len >= 2 && encodedHost.charAt(0) == '['
                    && encodedHost.charAt(len - 1) == ']') {
                host = decode(encodedHost.substring(1, len - 1));
            } else host = decode(IDN.toUnicode(encodedHost, IDN.ALLOW_UNASSIGNED));
        }
        return host;
    }

    @Override
    public String encodedHost() {
        return encodedHost;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String path() {
        if (path == null)
            path = decode(encodedPath, false, false);
        return path;
    }

    @Override
    public List<String> pathSegments() {
        String path = path();
        boolean encoded;
        if (path != null) {
            encoded = false;
        } else {
            path = encodedPath;
            encoded = true;
        }
        int len = path.length();
        if (len == 0)
            return new ArrayList<>(0);
        int p = 0;
        int i = 0;
        if (path.charAt(0) == '/') {
            p = 1;
            i = 1;
        }
        List<String> res = new ArrayList<>();
        for (; i <= len; i++) {
            if (i == len || path.charAt(i) == '/') {
                String seg = path.substring(p, i);
                if (encoded) seg = decode(seg);
                res.add(seg);
                p = i + 1;
            }
        }
        return res;
    }

    @Override
    public String encodedPath() {
        return encodedPath;
    }

    @Override
    public String encodedQuery() {
        return encodedQuery;
    }

    @Override
    public Map<String, List<String>> queryParameters() {
        if (encodedQuery == null)
            return null;
        int len = encodedQuery.length();
        int equals = -1; // =
        int and = -1; // &
        Map<String, List<String>> res = new LinkedHashMap<>();
        for (int i = 0; i <= len; i++) {
            char c;
            if (i == len || (c = encodedQuery.charAt(i)) == '&') {
                if (i - 1 == and) { // skip empty params
                    and = i;
                    continue;
                }

                String value = null;
                if (equals >= 0)
                    value = decode(encodedQuery.substring(equals + 1, i), true);
                else equals = i;
                String name = decode(encodedQuery.substring(and + 1, equals), true);

                res.computeIfAbsent(name, n -> new ArrayList<>(1))
                        .add(value);

                equals = -1;
                and = i;
            } else if (c == '=') equals = i;
        }
        return res;
    }

    @Override
    public String fragment() {
        if (fragment == null && encodedFragment != null)
            fragment = decode(encodedFragment);
        return fragment;
    }

    @Override
    public String encodedFragment() {
        return encodedFragment;
    }

    @Override
    public Uri normalize() {
        String normalizedPath = normalizePath(encodedPath);
        // If normalized, the length of path would be less.
        if (normalizedPath.length() == encodedPath.length())
            return this;
        UriImpl r = new UriImpl();
        r.scheme = scheme;
        r.encodedUserInfo = encodedUserInfo;
        r.encodedHost = encodedHost;
        r.port = port;
        r.encodedPath = normalizedPath;
        r.encodedQuery = encodedQuery;
        r.encodedFragment = encodedFragment;
        r.correctNoSchemePath();
        return r;
    }

    @Override
    public Uri resolve(Uri uri) {
        if (!(uri instanceof UriImpl))
            throw new IllegalArgumentException();
        return resolve(this, (UriImpl) uri);
    }

    @Override
    public Uri resolve(String uriStr) throws UriSyntaxException {
        return resolve(this, new UriImpl(uriStr));
    }

    @Override
    public boolean isRelative() {
        return scheme == null;
    }

    @Override
    public String toString() {
        if (string == null)
            buildString();
        return string;
    }

    @Override
    public URI toURI() {
        return URI.create(toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UriImpl)) return false;
        return toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    private void correctNoSchemePath() {
        // When scheme is not present, a rootless path
        // must not contain any colon in its first segment,
        // to bypass which a dot segment needs to precede the path (Section 4.2).
        if (scheme == null // path-noscheme
                && !isLegalNoSchemePath(encodedPath)) {
            encodedPath = "./" + encodedPath;
        }
    }

    private static boolean isLegalNoSchemePath(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == ':')
                return false;
            if (c == '/')
                return true;
        }
        return true;
    }

    private static Uri resolve(UriImpl base, UriImpl ref) {
        if (base.isRelative())
            throw new UnsupportedOperationException("Resolving against relative URI");
        UriImpl t = new UriImpl();

        if (ref.scheme != null) {
            t.scheme = ref.scheme;
            t.encodedUserInfo = ref.encodedUserInfo;
            t.encodedHost = ref.encodedHost;
            t.port = ref.port;
            t.encodedPath = normalizePath(ref.encodedPath);
            t.encodedQuery = ref.encodedQuery;
        } else {
            if (ref.encodedHost != null) {
                t.encodedUserInfo = ref.encodedUserInfo;
                t.encodedHost = ref.encodedHost;
                t.port = ref.port;
                t.encodedPath = normalizePath(ref.encodedPath);
                t.encodedQuery = ref.encodedQuery;
            } else {
                if (ref.encodedPath.isEmpty()) {
                    t.encodedPath = base.encodedPath;
                    if (ref.encodedQuery != null) {
                        t.encodedQuery = ref.encodedQuery;
                    } else {
                        t.encodedQuery = base.encodedQuery;
                    }
                } else {
                    if (ref.encodedPath.charAt(0) == '/') {
                        t.encodedPath = normalizePath(ref.encodedPath);
                    } else if (base.encodedHost != null && base.encodedPath.isEmpty()) {
                        t.encodedPath = '/' + ref.encodedPath;
                    } else {
                        t.encodedPath = normalizePath(
                                mergePaths(base.encodedPath, ref.encodedPath));
                    }
                    t.encodedQuery = ref.encodedQuery;
                }
                t.encodedUserInfo = base.encodedUserInfo;
                t.encodedHost = base.encodedHost;
                t.port = base.port;
            }
            t.scheme = base.scheme;
        }
        t.encodedFragment = ref.encodedFragment;
        return t;
    }

    private static String mergePaths(String base, String ref) {
        int len = base.length();
        if (len == 0) return ref;

        if (base.charAt(len - 1) == '/')
            return base + ref;
        int i = len - 2;
        while (i >= 0) {
            if (base.charAt(i) == '/') {
                i++;
                StringBuilder sb = new StringBuilder(i + ref.length());
                sb.append(base, 0, i);
                sb.append(ref);
                return sb.toString();
            }
            i--;
        }
        return ref;
    }

    private static String normalizePath(String path) {
        int len = path.length();
        if (len == 0) return path;
        CharBuffer cb = CharBuffer.allocate(len);

        int segStart = 0;
        int dotCnt = 0;
        boolean absolute = path.charAt(0) == '/';
        int limit = 0;

        int i = absolute ? 1 : 0;
        for (; i <= len; i++) {
            char c;
            boolean end = i == len;
            if (end || (c = path.charAt(i)) == '/') {
                int ss = segStart;
                int dc = dotCnt;
                segStart = i;
                dotCnt = 0;
                int pos = cb.position();
                if (dc == 1) {
                    if (end && (absolute || pos != 0))
                        cb.put('/');
                    continue;
                }
                if (dc == 2) {
                    if (absolute || pos != limit) {
                        pos = rewind(cb, limit);
                        if (end && (absolute || pos != 0))
                            cb.put('/');
                        continue;
                    }
                    limit = pos + i - ss;
                }
                // append segment
                cb.put(path, ss, i);
            } else if (c == '.') {
                if (dotCnt != -1) dotCnt++;
            } else {
                dotCnt = -1;
            }
        }
        cb.flip();
        len = cb.length();
        // skip the leading "/" if path is relative
        if (len != 0 && !absolute && cb.get(0) == '/') {
            cb.position(1);
            len--;
        }
        // if path is not empty and len is zero,
        // the result should be "."
        if (len == 0) return ".";
        return cb.toString();
    }

    private static int rewind(CharBuffer cb, int limit) {
        int pos = cb.position();
        if (pos == 0) return 0;
        int i = pos - 1;
        while (i > limit) {
            if (cb.get(i) == '/')
                break;
            i--;
        }
        cb.position(i);
        return i;
    }

    private void buildString() {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (encodedHost != null) {
            sb.append("//");
            if (encodedUserInfo != null) {
                sb.append(encodedUserInfo);
                sb.append('@');
            }
            sb.append(encodedHost);
            if (port >= 0) {
                sb.append(':');
                sb.append(port);
            }
        }
        sb.append(encodedPath);
        if (encodedQuery != null) {
            sb.append('?');
            sb.append(encodedQuery);
        }
        if (encodedFragment != null) {
            sb.append('#');
            sb.append(encodedFragment);
        }
        string = sb.toString();
    }

    // Four of the most general delimiters
    private static final char[] DELIMS = {':', '/', '?', '#'};

    private class Parser {

        private final String input;

        private Parser(String s) {
            input = s;
            string = s;
        }

        // -- Methods for throwing URISyntaxException in various ways --

        private void fail(String reason, int p) throws UriSyntaxException {
            throw new UriSyntaxException(input, reason, p);
        }

        private void failExpecting(String expected, int p) throws UriSyntaxException {
            fail("Expected " + expected, p);
        }

        // Tells whether start < end and, if so, whether charAt(start) == c
        private boolean at(int start, int end, char c) {
            return (start < end) && (input.charAt(start) == c);
        }

        // Scans the delimiters defined in DELIMS
        private int[] scanDelims() {
            int len = input.length();
            int[] res = new int[DELIMS.length];
            Arrays.fill(res, len);
            for (int i = 0; i < len; i++) {
                char ch = input.charAt(i);
                for (int j = 0; j < DELIMS.length; j++) {
                    if (res[j] == len && ch == DELIMS[j]) {
                        res[j] = i;
                    }
                }
            }
            return res;
        }

        // Scans the given char
        private int scan(int start, int n, char ch) {
            int p = start;
            while (p < n) {
                char cur = input.charAt(p);
                if (cur == ch)
                    break;
                p++;
            }
            return p;
        }

        // Scans the given char backwards
        private int scanBack(int start, int end, char ch, char stop) {
            for (int i = start; i >= end; i--) {
                char cur = input.charAt(i);
                if (cur == ch) {
                    return i;
                } else if (cur == stop) {
                    return -1;
                }
            }
            return -1;
        }

        // Scans a potential escape sequence, starting at the given position,
        // with the given first char (i.e., charAt(start) == c).
        private boolean scanPctEncoded(int start, int n, char first)
                throws UriSyntaxException {
            if (first == '%') {
                // Process escape pair
                if ((start + 3 <= n)
                        && match(input.charAt(start + 1), L_HEXDIG, H_HEXDIG)
                        && match(input.charAt(start + 2), L_HEXDIG, H_HEXDIG)) {
                    return true;
                }
                fail("Malformed percent-encoded octet", start);
            }
            return false;
        }

        // Scans chars that match the given mask pair
        private int scan(int start, int n, long lowMask, long highMask)
                throws UriSyntaxException {
            int p = start;
            boolean allowPctEncoded = (lowMask & L_PCT_ENCODED) != 0;
            while (p < n) {
                char c = input.charAt(p);
                if (match(c, lowMask, highMask)) {
                    p++;
                    continue;
                }
                if (allowPctEncoded) {
                    boolean enc = scanPctEncoded(p, n, c);
                    if (enc) {
                        p += 3;
                        continue;
                    }
                }
                break;
            }
            return p;
        }

        // Checks that each of the chars in [start, end) matches the given mask
        private void checkChars(int start, int end,
                                long lowMask, long highMask,
                                String what)
                throws UriSyntaxException {
            int p = scan(start, end, lowMask, highMask);
            if (p < end)
                fail("Illegal character in " + what, p);
        }

        // Checks that the char at position p matches the given mask
        private void checkChar(int p,
                               long lowMask, long highMask,
                               String what)
                throws UriSyntaxException {
            if (!match(input.charAt(p), lowMask, highMask))
                fail("Illegal character in " + what, p);
        }

        // Parses the input string
        private void parse() throws UriSyntaxException {
            int n = input.length();
            if (n == 0) {
                path = encodedPath = "";
                return;
            }

            int[] delims = scanDelims();
            int colon = delims[0];
            int slash = delims[1];
            int qMark = delims[2]; // question mark
            int sharp = delims[3];

            if (colon == 0)
                failExpecting("scheme", 0);
            if (sharp < qMark) qMark = n;

            int p = 0;
            if (colon < slash && colon < qMark && colon < sharp) {
                checkChar(0, L_ALPHA, H_ALPHA, "scheme");
                checkChars(1, colon, L_SCHEME, H_SCHEME, "scheme");
                scheme = input.substring(0, colon);
                p = colon + 1;
            }
            boolean hasQuery = qMark != n;
            int hierEnd = hasQuery ? qMark : sharp;
            parseHierPart(p, hierEnd);

            if (hasQuery) { // query available
                p = qMark + 1;
                checkChars(p, sharp, L_QUERY_FRAGMENT, H_QUERY_FRAGMENT, "query");
                encodedQuery = input.substring(p, sharp);
            }
            if (sharp != n) { // fragment available
                p = sharp + 1;
                checkChars(p, n, L_QUERY_FRAGMENT, H_QUERY_FRAGMENT, "fragment");
                encodedFragment = input.substring(p, n);
            }
        }

        // Parses the hier-part
        private void parseHierPart(int start, int n) throws UriSyntaxException {
            int p = start;
            if (at(p, n, '/') && at(p + 1, n, '/')) {
                p += 2;
                int authEnd = scan(p, n, '/');
                parseAuthority(p, authEnd);
                p = authEnd;
            }
            checkChars(p, n, L_PATH, H_PATH, "path");
            encodedPath = input.substring(p, n);
        }

        // Parses the authority
        private void parseAuthority(int start, int n) throws UriSyntaxException {
            int p = start;
            int at = scan(p, n, '@');
            if (at != n) {
                checkChars(p, at, L_USERINFO, H_USERINFO, "userinfo");
                encodedUserInfo = input.substring(p, at);
                p = at + 1;
            }
            int colon = scanBack(n - 1, p, ':', ']');
            if (colon >= 0) {
                if (colon != n - 1) {
                    checkChars(colon + 1, n, L_DIGIT, H_DIGIT, "port");
                    port = Integer.parseInt(input.substring(colon + 1, n));
                }
                n = colon;
            }
            if (at(p, n, '[') && at(n - 1, n, ']')) {
                checkIpv6Address(input, p + 1, n - 1, true);
            } else {
                checkChars(p, n, L_REG_NAME, H_REG_NAME, "host");
            }
            encodedHost = input.substring(p, n);
        }
    }
}
