package cn.yescallop.uriutils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO: More tests
 *
 * @author Scallop Ye
 */
public class UriTest {

    @Test
    public void testUnusual() {
        // Empty
        Uri uri = Uri.from("");
        assertNull(uri.scheme());
        assertNull(uri.encodedUserInfo());
        assertNull(uri.userInfo());
        assertNull(uri.encodedHost());
        assertNull(uri.host());
        assertEquals(uri.port(), -1);
        assertTrue(uri.encodedPath().isEmpty());
        assertTrue(uri.path().isEmpty());
        assertNull(uri.encodedQuery());
        assertNull(uri.queryParameters());
        assertNull(uri.encodedFragment());
        assertNull(uri.fragment());
        assertFalse(uri.opaque());
        assertTrue(uri.relative());

        // With empty query and fragment
        uri = Uri.from("?#");
        assertTrue(uri.encodedQuery().isEmpty());
        assertTrue(uri.queryParameters().isEmpty());
        assertTrue(uri.encodedFragment().isEmpty());

        // Containing encoded slash "%2F" in the path
        uri = Uri.from("%2F");
        assertNull(uri.path());
        List<String> segments = uri.pathSegments();
        assertEquals(segments.size(), 1);
        assertEquals(segments.get(0), "/");
    }

}
