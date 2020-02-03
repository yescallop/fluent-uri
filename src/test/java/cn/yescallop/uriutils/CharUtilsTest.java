package cn.yescallop.uriutils;

import org.junit.jupiter.api.Test;

import static cn.yescallop.uriutils.CharUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Scallop Ye
 */
public class CharUtilsTest {

    @Test
    public void testConstants() {
        assertEquals(L_DIGIT, lowMask('0', '9'));
        assertEquals(H_DIGIT, highMask('0', '9'));

        assertEquals(L_ALPHA, lowMask('A', 'Z') | lowMask('a', 'z'));
        assertEquals(H_ALPHA, highMask('A', 'Z') | highMask('a', 'z'));

        assertEquals(L_HEXDIG, L_DIGIT | lowMask('A', 'F') | lowMask('a', 'f'));
        assertEquals(H_HEXDIG, H_DIGIT | highMask('A', 'F') | highMask('a', 'f'));

        assertEquals(L_SUB_DELIMS, lowMask("!$&'()*+,;="));
        assertEquals(H_SUB_DELIMS, highMask("!$&'()*+,;="));

        assertEquals(L_UNRESERVED, L_ALPHA | L_DIGIT | lowMask("-._~"));
        assertEquals(H_UNRESERVED, H_ALPHA | H_DIGIT | highMask("-._~"));

        assertEquals(L_PCHAR, L_UNRESERVED | L_PCT_ENCODED | L_SUB_DELIMS | lowMask(":@"));
        assertEquals(H_PCHAR, H_UNRESERVED | H_PCT_ENCODED | L_SUB_DELIMS | highMask(":@"));

        assertEquals(L_SCHEME, L_ALPHA | L_DIGIT | lowMask("+-."));
        assertEquals(H_SCHEME, H_ALPHA | H_DIGIT | highMask("+-."));

        assertEquals(L_USERINFO, L_UNRESERVED | L_PCT_ENCODED | L_SUB_DELIMS | lowMask(":"));
        assertEquals(H_USERINFO, H_UNRESERVED | H_PCT_ENCODED | H_SUB_DELIMS | highMask(":"));

        assertEquals(L_PATH, L_PCHAR | lowMask("/"));
        assertEquals(H_PATH, H_PCHAR | highMask("/"));

        assertEquals(L_QUERY_FRAGMENT, L_PCHAR | lowMask("/?"));
        assertEquals(H_QUERY_FRAGMENT, H_PCHAR | highMask("/?"));

        assertEquals(L_QUERY_PARAM, L_QUERY_FRAGMENT ^ lowMask("&+="));
        assertEquals(H_QUERY_PARAM, H_QUERY_FRAGMENT ^ highMask("&+="));
    }

    @Test
    public void testEncodeDecode() {
        String raw = "😃a 测试1`~!@#$%^&+=";
        String s = encode(raw, L_QUERY_FRAGMENT, H_QUERY_FRAGMENT);
        assertEquals("%F0%9F%98%83a%20%E6%B5%8B%E8%AF%951%60~!@%23$%25%5E&+=", s);
        assertEquals(raw, decode(s));
        s = encode("&+= ", L_QUERY_PARAM, H_QUERY_PARAM, true);
        assertEquals("%26%2B%3D+", s);
        assertEquals("&+= ", decode(s, true));
    }

    // Computes the low-order mask for the characters in the given string
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

    // Computes the high-order mask for the characters in the given string
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
    // between first and last, inclusive
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
    // between first and last, inclusive
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
