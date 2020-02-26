package cn.yescallop.fluenturi;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static cn.yescallop.fluenturi.CharUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Scallop Ye
 */
public class CharUtilsTest {

    @Test
    public void testConstants() {
        assertEquals(lowMask('0', '9'), L_DIGIT);
        assertEquals(highMask('0', '9'), H_DIGIT);

        assertEquals(lowMask('A', 'Z') | lowMask('a', 'z'), L_ALPHA);
        assertEquals(highMask('A', 'Z') | highMask('a', 'z'), H_ALPHA);

        assertEquals(L_DIGIT | lowMask('A', 'F') | lowMask('a', 'f'), L_HEXDIG);
        assertEquals(H_DIGIT | highMask('A', 'F') | highMask('a', 'f'), H_HEXDIG);

        assertEquals(lowMask("!$&'()*+,;="), L_SUB_DELIMS);
        assertEquals(highMask("!$&'()*+,;="), H_SUB_DELIMS);

        assertEquals(L_ALPHA | L_DIGIT | lowMask("-._~"), L_UNRESERVED);
        assertEquals(H_ALPHA | H_DIGIT | highMask("-._~"), H_UNRESERVED);

        assertEquals(L_UNRESERVED | L_PCT_ENCODED | L_SUB_DELIMS | lowMask(":@"), L_PCHAR);
        assertEquals(H_UNRESERVED | H_PCT_ENCODED | L_SUB_DELIMS | highMask(":@"), H_PCHAR);

        assertEquals(L_ALPHA | L_DIGIT | lowMask("+-."), L_SCHEME);
        assertEquals(H_ALPHA | H_DIGIT | highMask("+-."), H_SCHEME);

        assertEquals(L_UNRESERVED | L_PCT_ENCODED | L_SUB_DELIMS | lowMask(":"), L_USERINFO);
        assertEquals(H_UNRESERVED | H_PCT_ENCODED | H_SUB_DELIMS | highMask(":"), H_USERINFO);

        assertEquals(L_PCHAR | lowMask("/"), L_PATH);
        assertEquals(H_PCHAR | highMask("/"), H_PATH);

        assertEquals(L_PCHAR | lowMask("/?"), L_QUERY_FRAGMENT);
        assertEquals(H_PCHAR | highMask("/?"), H_QUERY_FRAGMENT);

        assertEquals(L_QUERY_FRAGMENT ^ lowMask("&+="), L_QUERY_PARAM);
        assertEquals(H_QUERY_FRAGMENT ^ highMask("&+="), H_QUERY_PARAM);
    }

    @Test
    public void testEncodeDecode() {
        String raw = "😃a 测试1`~!@#$%^&+=";
        String s = encode(raw, L_QUERY_FRAGMENT, H_QUERY_FRAGMENT);
        assertEquals("%F0%9F%98%83a%20%E6%B5%8B%E8%AF%951%60~!@%23$%25%5E&+=", s);
        assertEquals(raw, decode(s.toLowerCase()));
        s = encode("&+= ", L_QUERY_PARAM, H_QUERY_PARAM, true);
        assertEquals("%26%2B%3D+", s);
        assertEquals("&+= ", decode(s, true));
        s = decode("a+b+c+d+e", true);
        assertEquals("a b c d e", s);

        assertThrows(IllegalArgumentException.class,
                () -> decode("%EX"),
                "Malformed percent-encoded octet");
    }

    @Test
    public void testCheckHostname() {
        byte[] b = new byte[66];
        Arrays.fill(b, (byte) 'a');
        b[64] = (byte) '.';
        String[] compliantHosts = new String[]{
                "a", "a.", "A-a", "a-A.B-b", "1.1.a",
                new String(b, 0, 63, StandardCharsets.US_ASCII)
        };
        String[] nonCompliantHosts = new String[]{
                "", ".", ".a", "a-", "-a",
                "a.-a.a", "a.a-.a", "a..a",
                "a@a", "a_a.com", "1", "a.1.", "1.1.1.1", "a.b.1",
                new String(new byte[254], StandardCharsets.US_ASCII),
                new String(b, 0, 65, StandardCharsets.US_ASCII),
                new String(b, StandardCharsets.US_ASCII)
        };

        for (String s : compliantHosts) {
            checkHostname(s);
        }
        for (String s : nonCompliantHosts) {
            try {
                checkHostname(s);
            } catch (IllegalArgumentException e) {
                continue;
            }
            fail("Exception not thrown: " + s);
        }
    }

    @Test
    public void testCheckIpv6Address() {
        String[] legals = new String[]{
                "::", "1234:5678:90AB:CDEF:1234:5678:90AB:CDEF",
                "0:a:b:c:d:e:f:0", "::0:0:0:0:0:0:0", "0:0:0:0:0:0:0::",
                "a:b::c:d", "::cd", "FFFF::1.1.1.1", "::1.1.1.1",
                "a:b:c:d:e::1.1.1.1", "a:b::255.133.244.255"
        };
        String[] illegals = new String[]{
                ":0", "0:", ":::", "::cd::", "0:a:b:c:d:e:f:g", "0:a:b:c:d:e:f:0:0",
                "a:b::255.255.255.256", "a:b:c:d:e:f::1.1.1.1",
                "a:b::1.2.3.a", "a:b::01.2.3.4", "aaaaa::",
                "::111.111.111", "::1.1.1.1.1", "::1.1.1"
        };
        String[] legalZoneIds = new String[]{
                "0", "1", "en1", "eth0", "0a-.~_%20"
        };
        String[] illegalZoneIds = new String[]{
                "", "<>", "a^b"
        };

        for (String s : legals) {
            try {
                checkIpv6Address(s, 0, s.length(), false);
            } catch (UriSyntaxException e) {
                fail(e);
            }
        }
        for (String s : illegals) {
            try {
                checkIpv6Address(s, 0, s.length(), false);
            } catch (UriSyntaxException e) {
                continue;
            }
            fail("Exception not thrown: " + s);
        }

        for (String s : legalZoneIds) {
            try {
                checkIpv6Address("::%" + s, 0, s.length() + 3, false);
                checkIpv6Address("::%25" + s, 0, s.length() + 5, true);
            } catch (UriSyntaxException e) {
                fail(e);
            }
        }
        for (String s : illegalZoneIds) {
            try {
                checkIpv6Address("::%25" + s, 0, s.length() + 5, true);
            } catch (UriSyntaxException e) {
                continue;
            }
            fail("Exception not thrown:" + s);
        }

        assertThrows(UriSyntaxException.class,
                () -> checkIpv6Address("::%0", 0, 4, true),
                "Expected %25 at index 2: ::%0");
    }

    // Computes the low-order mask for the characters in the given string.
    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64)
                m |= (1L << c);
        }
        return m;
    }

    // Computes the high-order mask for the characters in the given string.
    private static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128))
                m |= (1L << (c - 64));
        }
        return m;
    }

    // Computes a low-order mask for the characters
    // between first and last, inclusive.
    private static long lowMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        if (f == l) return 0L;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Computes a high-order mask for the characters
    // between first and last, inclusive.
    private static long highMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        if (f == l) return 0L;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

}
