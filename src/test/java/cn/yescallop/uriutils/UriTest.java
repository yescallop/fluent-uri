package cn.yescallop.uriutils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Scallop Ye
 */
public class UriTest {

    private static final String ALL = "http://us%20er:in%40fo@xn--h28h.com:8080" +
            "/pa%20th/%E6%B5%8B/%E8%AF%95" +
            "?k%201=v%261&%E9%94%AE=v%3D2#?%23";

    @Test
    public void testParse() throws UriSyntaxException {
        Uri u = Uri.from(ALL);
        assertEquals("http", u.scheme());
        assertEquals("us%20er:in%40fo", u.encodedUserInfo());
        assertEquals("us er:in@fo", u.userInfo());
        assertEquals("xn--h28h.com", u.encodedHost());
        assertEquals("😃.com", u.host());
        assertEquals(8080, u.port());
        assertEquals("/pa%20th/%E6%B5%8B/%E8%AF%95", u.encodedPath());
        assertEquals("/pa th/测/试", u.path());

        List<String> segments = u.pathSegments();
        assertEquals(3, segments.size());
        assertEquals("pa th", segments.get(0));
        assertEquals("测", segments.get(1));
        assertEquals("试", segments.get(2));

        assertEquals("k%201=v%261&%E9%94%AE=v%3D2", u.encodedQuery());

        Map<String, List<String>> params = u.queryParameters();
        assertEquals(2, params.size());
        assertEquals("v&1", params.get("k 1").get(0));
        assertEquals("v=2", params.get("键").get(0));

        assertEquals("?%23", u.encodedFragment());
        assertEquals("?#", u.fragment());

        // Opaque
        u = Uri.from("urn:opaque");
        assertTrue(u.opaque());

        // Empty
        u = Uri.from("");
        assertNull(u.scheme());
        assertNull(u.encodedUserInfo());
        assertNull(u.userInfo());
        assertNull(u.encodedHost());
        assertNull(u.host());
        assertEquals(-1, u.port());
        assertTrue(u.encodedPath().isEmpty());
        assertTrue(u.path().isEmpty());
        assertNull(u.encodedQuery());
        assertNull(u.queryParameters());
        assertNull(u.encodedFragment());
        assertNull(u.fragment());
        assertFalse(u.opaque());
        assertTrue(u.relative());

        // With empty userinfo, host and port
        u = Uri.from("//@:");
        assertEquals("", u.encodedUserInfo());
        assertEquals("", u.encodedHost());
        assertEquals(-1, u.port());

        // With empty query and fragment
        u = Uri.from("?#");
        assertTrue(u.encodedQuery().isEmpty());
        assertTrue(u.queryParameters().isEmpty());
        assertTrue(u.encodedFragment().isEmpty());

        // With query whose parameters have no value
        u = Uri.from("?k1&k2");
        params = u.queryParameters();
        assertEquals(2, params.size());
        assertNull(params.get("k1").get(0));
        assertNull(params.get("k2").get(0));

        // Containing encoded slash "%2F" in the path
        u = Uri.from("%2F");
        assertNull(u.path());
        segments = u.pathSegments();
        assertEquals(1, segments.size());
        assertEquals("/", segments.get(0));

        // IPv6 host
        u = Uri.from("//[fe80::ebad:9145:fe66:55cc%25a%2Bb]:80");
        assertEquals("fe80::ebad:9145:fe66:55cc%a+b", u.host());
        assertEquals(80, u.port());
    }

    @Test
    public void testBuild() {
        // All
        Uri u = Uri.newBuilder()
                .scheme("http")
                .userInfo("us er:in@fo")
                .host("😃.com")
                .port(8080)
                .path("/pa th/测")
                .appendPathSegment("试")
                .appendQueryParameter("k 1", "v&1")
                .appendQueryParameter("键", "v=2")
                .fragment("?#")
                .build();
        assertEquals(ALL, u.toString());

        // Creating builder from Uri
        assertEquals(u, u.asBuilder().build());

        // From encoded components
        u = Uri.newBuilder()
                .scheme("http")
                .encodedUserInfo("us%20er:in%40fo")
                .encodedHost("xn--h28h.com")
                .port(8080)
                .encodedPath("/pa%20th/%E6%B5%8B/%E8%AF%95")
                .encodedQuery("k%201=v%261&%E9%94%AE=v%3D2")
                .encodedFragment("?%23")
                .build();
        assertEquals(ALL, u.toString());

        // Path appending
        u = Uri.newBuilder()
                .path("foo/")
                .appendPathSegment("bar")
                .build();
        assertEquals("foo/bar", u.toString());

        // Append path segment to empty builder
        u = Uri.newBuilder()
                .appendPathSegment("foo")
                .build();
        assertEquals("foo", u.toString());

        // Empty
        u = Uri.newBuilder().build();
        assertEquals("", u.toString());

        // Same-document reference
        u = Uri.newBuilder()
                .appendQueryParameter("k", "v")
                .fragment("f")
                .build();
        assertEquals("?k=v#f", u.toString());

        // Clears query
        u = u.asBuilder().clearQuery().build();
        assertEquals("#f", u.toString());

        // Rootless, with colon in the first segment
        u = Uri.newBuilder()
                .path("te:st")
                .build();
        assertEquals("./te:st", u.toString());

        // Percent-encoding for host
        u = Uri.newBuilder()
                .host("测试")
                .hostEncodingOption(Uri.HostEncodingOption.PERCENT_ENCODED)
                .build();
        assertEquals("//%E6%B5%8B%E8%AF%95", u.toString());

        // IPv6 host
        u = Uri.newBuilder()
                .host("fe80::ebad:9145:fe66:55cc%a+b") // scoped
                .build();
        assertEquals("//[fe80::ebad:9145:fe66:55cc%25a%2Bb]", u.toString());

        u = Uri.newBuilder()
                .host("::1") // not scoped
                .build();
        assertEquals("//[::1]", u.toString());
    }

    @Test
    public void testParsingExceptions() {
        assertUSE(() -> Uri.from("%EX"), "Malformed percent-encoded octet");
        // Empty scheme
        assertUSE(() -> Uri.from(":"), "Expected scheme");
        // Illegal scheme
        assertUSE(() -> Uri.from("_:"), "Illegal character in scheme");
        // Illegal userinfo
        assertUSE(() -> Uri.from("a://<@a"), "Illegal character in userinfo");
        // Illegal host
        assertUSE(() -> Uri.from("a://<"), "Illegal character in host");
        // Illegal port
        assertUSE(() -> Uri.from("a://a:-1"), "Illegal character in port");
        // Illegal path
        assertUSE(() -> Uri.from("a:<"), "Illegal character in path");
        // Illegal query
        assertUSE(() -> Uri.from("?<"), "Illegal character in query");
        // Illegal fragment
        assertUSE(() -> Uri.from("#<"), "Illegal character in fragment");
    }

    @Test
    public void testBuilderExceptions() {
        Uri.Builder b = Uri.newBuilder();

        // Empty scheme
        assertIAE(() -> b.scheme(""), "Empty scheme");
        // Scheme starting with non-alphabet character
        assertIAE(() -> b.scheme("2333"));
        // Illegal scheme
        assertIAE(() -> b.scheme("te_st"));
        // Illegal userinfo
        assertIAE(() -> b.encodedUserInfo("@"));
        // Illegal IPv6 address
        b.host("IL:LE:GA:L");
        assertIAE(b::build, "Illegal character in IPv6 address");
        // Illegal encoded IPv6 address
        assertIAE(() -> b.encodedHost("[ILLEGAL]"), "Illegal character in IPv6 address");
        // Illegal port
        assertIAE(() -> b.port(-2));
        // Illegal query
        assertIAE(() -> b.encodedQuery("#"));
        // Illegal fragment
        assertIAE(() -> b.encodedFragment("#"));
        // Malformed percent-encoded octet
        assertIAE(() -> b.encodedFragment("%EX"), "Malformed percent-encoded octet");
        // Attempting to set an encoded path after appending to the path
        b.appendPathSegment("a");
        assertThrows(IllegalStateException.class,
                () -> b.encodedPath("a"),
                "path already appended to");
        // Attempting to set an encoded query after appending to the query
        b.appendQueryParameter("k", "v");
        assertThrows(IllegalStateException.class,
                () -> b.encodedQuery("a"),
                "query already appended to");
        // Attempting to clear the query after appending to it
        assertThrows(IllegalStateException.class,
                b::clearQuery,
                "query already appended to");

        b.host("host").path("rootless");
        assertIAE(b::build, "Rootless path with authority present");
    }

    private static void assertIAE(Executable e) {
        assertIAE(e, null);
    }

    private static void assertIAE(Executable e, String reason) {
        try {
            e.execute();
        } catch (Throwable t) {
            if (t instanceof IllegalArgumentException) {
                if (reason == null) return;
                Throwable cause = t.getCause();
                if (cause == null) {
                    assertEquals(reason, t.getMessage());
                } else {
                    UriSyntaxException use = (UriSyntaxException) t.getCause();
                    assertEquals(reason, use.reason());
                }
                return;
            }
            fail("Not IAE", t);
        }
        fail("Exception not thrown");
    }

    private static void assertUSE(Executable e, String reason) {
        try {
            e.execute();
        } catch (Throwable t) {
            if (t instanceof UriSyntaxException) {
                assertEquals(reason, ((UriSyntaxException) t).reason());
                return;
            }
            fail("Not USE", t);
        }
        fail("Exception not thrown");
    }

}
