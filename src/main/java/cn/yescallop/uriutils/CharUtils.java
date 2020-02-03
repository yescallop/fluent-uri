package cn.yescallop.uriutils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for character checking and en/decoding
 *
 * @author Scallop Ye
 */
public class CharUtils {

    // Thread-local UTF-8 encoder and decoder
    private static final ThreadLocal<CharsetEncoder> ENCODER =
            ThreadLocal.withInitial(StandardCharsets.UTF_8::newEncoder);
    private static final ThreadLocal<CharsetDecoder> DECODER =
            ThreadLocal.withInitial(() -> StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE));

    private CharUtils() {
        // no instance
    }

    // Tells whether the given character is permitted by the given mask pair
    public static boolean match(char c, long lowMask, long highMask) {
        if (c == 0) // 0 doesn't have a slot in the mask. So, it never matches.
            return false;
        if ((c & 0xFFC0) == 0) // c < 64
            return ((1L << c) & lowMask) != 0;
        if ((c & 0xFF80) == 0) // c < 128
            return ((1L << (c ^ 64)) & highMask) != 0;
        return false;
    }

    // Character-class masks from RFC 3986, which are tested in CharUtilsTest.

    public static final long L_DIGIT = 0x3FF000000000000L;
    public static final long H_DIGIT = 0L;

    public static final long L_ALPHA = 0L;
    public static final long H_ALPHA = 0x7FFFFFE07FFFFFEL;

    // HEXDIG        = DIGIT / "A" / "B" / "C" / "D" / "E" / "F" /
    //                         "a" / "b" / "c" / "d" / "e" / "f"
    public static final long L_HEXDIG = L_DIGIT;
    public static final long H_HEXDIG = 0x7E0000007EL;

    // sub-delims    = "!" / "$" / "&" / "'" / "(" / ")" /
    //                 "*" / "+" / "," / ";" / "="
    public static final long L_SUB_DELIMS = 0x28001fd200000000L;
    public static final long H_SUB_DELIMS = 0L;

    // unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
    public static final long L_UNRESERVED = L_ALPHA | L_DIGIT | 0x600000000000L;
    public static final long H_UNRESERVED = H_ALPHA | H_DIGIT | 0x4000000080000000L;

    // The zero'th bit is used to indicate that percent-encoded octets
    // are allowed; this is handled by the scanEscape method below.
    public static final long L_PCT_ENCODED = 1L;
    public static final long H_PCT_ENCODED = 0L;

    // pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
    public static final long L_PCHAR
            = L_UNRESERVED | L_PCT_ENCODED | L_SUB_DELIMS | 0x400000000000000L;
    public static final long H_PCHAR
            = H_UNRESERVED | H_PCT_ENCODED | L_SUB_DELIMS | 1L;

    // scheme        = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
    public static final long L_SCHEME = L_ALPHA | L_DIGIT | 0x680000000000L;
    public static final long H_SCHEME = H_ALPHA | H_DIGIT;

    // userinfo      = *( unreserved / pct-encoded / sub-delims / ":" )
    public static final long L_USERINFO
            = L_UNRESERVED | L_PCT_ENCODED | L_SUB_DELIMS | 0x400000000000000L;
    public static final long H_USERINFO
            = H_UNRESERVED | H_PCT_ENCODED | H_SUB_DELIMS;

    // reg-name      = *( unreserved / pct-encoded / sub-delims )
    public static final long L_REG_NAME
            = L_UNRESERVED | L_PCT_ENCODED | L_SUB_DELIMS;
    public static final long H_REG_NAME
            = H_UNRESERVED | H_PCT_ENCODED | L_SUB_DELIMS;

    // All valid path characters
    public static final long L_PATH = L_PCHAR | 0x800000000000L;
    public static final long H_PATH = H_PCHAR;

    // query         = *( pchar / "/" / "?" )
    // fragment      = *( pchar / "/" / "?" )
    public static final long L_QUERY_FRAGMENT = L_PCHAR | 0x8000800000000000L;
    public static final long H_QUERY_FRAGMENT = H_PCHAR;

    // query-param   = query with "&+=" removed
    public static final long L_QUERY_PARAM = L_QUERY_FRAGMENT ^ 0x2000084000000000L;
    public static final long H_QUERY_PARAM = H_QUERY_FRAGMENT;

    // -- Escaping and encoding --

    private static final char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendEscape(StringBuilder sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[b & 0x0f]);
    }

    private static void appendEncoded(StringBuilder sb, CharBuffer cb, ByteBuffer bb) {
        CharsetEncoder enc = ENCODER.get();
        bb.clear();
        enc.reset();
        enc.encode(cb, bb, true);
        enc.flush(bb);
        bb.flip();

        while (bb.hasRemaining()) {
            byte b = bb.get();
            if ((b & 0x80) != 0) // (int) b >= 0x80
                appendEscape(sb, b);
            else
                sb.append((char) b);
        }
    }

    public static String encode(String s, long lowMask, long highMask) {
        return encode(s, lowMask, highMask, false);
    }

    // Encodes any characters in s that are not permitted
    // by the given mask pair
    public static String encode(String s,
                                long lowMask, long highMask,
                                boolean encodeSpaceAsPlus) {
        StringBuilder sb = null;
        char[] ca = null;
        CharBuffer cb = null;
        ByteBuffer bb = null;
        boolean allowNonASCII = ((lowMask & L_PCT_ENCODED) != 0);
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if ((c & 0xFF80) == 0) { // c < 0x80
                if (!match(c, lowMask, highMask)) {
                    if (sb == null) {
                        sb = new StringBuilder();
                        sb.append(s, 0, i);
                    }
                    if (encodeSpaceAsPlus && c == ' ') {
                        sb.append('+');
                    } else appendEscape(sb, (byte) c);
                } else {
                    if (sb != null)
                        sb.append(c);
                }
            } else if (allowNonASCII) {
                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append(s, 0, i);
                }
                if (ca == null) {
                    ca = new char[2];
                    cb = CharBuffer.wrap(ca);
                    bb = ByteBuffer.allocate(4);
                }

                ca[0] = c;
                int limit;
                if (Character.isHighSurrogate(c)) {
                    ca[1] = s.charAt(++i);
                    limit = 2;
                } else limit = 1;

                cb.limit(limit);
                cb.position(0);
                appendEncoded(sb, cb, bb);
            } else {
                if (sb != null)
                    sb.append(c);
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte) ((decode(c1) << 4) | decode(c2));
    }

    public static String decode(String s) {
        return decode(s, false, true);
    }

    public static String decode(String s, boolean decodePlusAsSpace) {
        return decode(s, decodePlusAsSpace, true);
    }

    static String decode(String s, boolean decodePlusAsSpace, boolean allowEncodedSlash) {
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0) {
            if (decodePlusAsSpace) // just do simple replace
                return s.replace('+', ' ');
            else return s;
        }

        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = DECODER.get();

        char c = s.charAt(0);

        for (int i = 0; i < n; ) {
            if (c != '%') {
                if (decodePlusAsSpace && c == '+') c = ' ';
                cb.put(c);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            while (true) {
                byte b = decode(s.charAt(++i), s.charAt(++i));
                if (!allowEncodedSlash && b == 0x2F)
                    return null;
                bb.put(b);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                if (c != '%')
                    break;
            }
            bb.flip();
            dec.reset();
            dec.decode(bb, cb, true);
            dec.flush(cb);
        }

        return cb.flip().toString();
    }

}
