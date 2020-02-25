package cn.yescallop.uriutils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for character checking and en/decoding.
 *
 * @author Scallop Ye
 * @see <a href="https://www.ietf.org/rfc/rfc3986.html#section-2">Section 2: Characters, RFC 3986</a>
 */
public final class CharUtils {

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

    /**
     * Tells whether the given character is permitted by the given mask pair.
     *
     * @param c a char
     * @param lowMask low mask
     * @param highMask high mask
     * @return true if the character is permitted, or else false
     */
    public static boolean match(char c, long lowMask, long highMask) {
        if (c == 0) // 0 doesn't have a slot in the mask. So, it never matches.
            return false;
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c ^ 64)) & highMask) != 0;
        return false;
    }

    // Character-class masks from RFC 3986, which are tested in CharUtilsTest

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
    public static final long L_SUB_DELIMS = 0x28001FD200000000L;
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

    // RFC 6874: ZoneID = 1*( unreserved / pct-encoded )
    public static final long L_ZONE_ID = L_UNRESERVED | L_PCT_ENCODED;
    public static final long H_ZONE_ID = H_UNRESERVED | H_PCT_ENCODED;

    // -- Escaping and encoding --

    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendEscape(StringBuilder sb, byte b) {
        sb.append('%');
        sb.append(HEX_DIGITS[(b >> 4) & 0x0F]);
        sb.append(HEX_DIGITS[b & 0x0F]);
    }

    private static void appendEncoded(StringBuilder sb, CharBuffer cb, ByteBuffer bb) {
        CharsetEncoder enc = ENCODER.get();
        bb.clear();
        enc.reset();
        enc.encode(cb, bb, true);
        enc.flush(bb);
        bb.flip();

        while (bb.hasRemaining()) {
            appendEscape(sb, bb.get());
        }
    }

    /**
     * Encodes any characters in a string that are not permitted
     * by the given mask pair.
     *
     * @param s an input string
     * @param lowMask low mask
     * @param highMask high mask
     * @return the encoded string
     */
    public static String encode(String s, long lowMask, long highMask) {
        return encode(s, lowMask, highMask, false);
    }

    /**
     * Encodes any characters in a string that are not permitted
     * by the given mask pair.
     *
     * @param s an input string
     * @param lowMask low mask
     * @param highMask high mask
     * @param encodeSpaceAsPlus whether encoding space (" ") as plus ("+")
     * @return the encoded string
     */
    public static String encode(String s,
                                long lowMask, long highMask,
                                boolean encodeSpaceAsPlus) {
        if ((lowMask & L_PCT_ENCODED) == 0)
            throw new IllegalArgumentException("Mask pair not for encoding");

        StringBuilder sb = null;
        char[] ca = null;
        CharBuffer cb = null;
        ByteBuffer bb = null;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x80) {
                if (!match(c, lowMask, highMask)) {
                    if (sb == null) {
                        sb = new StringBuilder(len + 16);
                        sb.append(s, 0, i);
                    }
                    if (encodeSpaceAsPlus && c == ' ') {
                        sb.append('+');
                    } else appendEscape(sb, (byte) c);
                } else if (sb != null) {
                    sb.append(c);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(len + 16);
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
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    // -- Decoding --

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        throw new IllegalArgumentException("Malformed percent-encoded octet");
    }

    private static byte decode(char c1, char c2) {
        return (byte) ((decode(c1) << 4) | decode(c2));
    }

    /**
     * Decodes a percent-encoded string.
     *
     * @param s an input string
     * @return the decoded string
     */
    public static String decode(String s) {
        return decode(s, false, true);
    }

    /**
     * Decodes a percent-encoded string.
     *
     * @param s an input string
     * @param decodePlusAsSpace whether decoding plus ("+") as space (" ")
     * @return the decoded string
     */
    public static String decode(String s, boolean decodePlusAsSpace) {
        return decode(s, decodePlusAsSpace, true);
    }

    static String decode(String s, boolean decodePlusAsSpace, boolean allowEncodedSlash) {
        int n = s.length();
        if (n == 0) return s;

        ByteBuffer bb = null;
        CharBuffer cb = null;
        CharsetDecoder dec = null;

        char c = s.charAt(0);

        for (int i = 0; i < n; ) {
            if (c != '%') {
                if (decodePlusAsSpace && c == '+') {
                    c = ' ';
                    if (cb == null) {
                        cb = CharBuffer.allocate(n);
                        cb.put(s, 0, i);
                    }
                }
                if (cb != null)
                    cb.put(c);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                continue;
            }
            if (bb == null) {
                bb = ByteBuffer.allocate(n);
                dec = DECODER.get();
            }
            if (cb == null) {
                cb = CharBuffer.allocate(n);
                cb.put(s, 0, i);
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

        return cb == null ? s : cb.flip().toString();
    }

    // -- Scanning and checking --

    private static void fail(String input, String reason, int p) {
        throw new IllegalArgumentException(new UriSyntaxException(input, reason, p));
    }

    private static void failUSE(String input, String reason, int p) throws UriSyntaxException {
        throw new UriSyntaxException(input, reason, p);
    }

    private static void failUSEExpecting(String input, String expected, int p) throws UriSyntaxException {
        throw new UriSyntaxException(input, "Expected " + expected, p);
    }

    // Scans a potential escape sequence, starting at the given position,
    // with the given first char (i.e., charAt(start) == c).
    private static boolean scanPctEncoded(String input,
                                          int start, int n, char first) {
        if (first == '%') {
            // Process escape pair
            if ((start + 3 <= n)
                    && match(input.charAt(start + 1), L_HEXDIG, H_HEXDIG)
                    && match(input.charAt(start + 2), L_HEXDIG, H_HEXDIG)) {
                return true;
            }
            fail(input, "Malformed percent-encoded octet", start);
        }
        return false;
    }

    // Scans the given char, starting at the given position.
    private static int scan(String input, int start, int n, char ch) {
        int p = start;
        while (p < n) {
            char cur = input.charAt(p);
            if (cur == ch)
                break;
            p++;
        }
        return p;
    }

    // Scans chars that match the given mask pair.
    private static int scan(String input,
                            int start, int n, long lowMask, long highMask) {
        int p = start;
        boolean allowPctEncoded = (lowMask & L_PCT_ENCODED) != 0;
        while (p < n) {
            char c = input.charAt(p);
            if (match(c, lowMask, highMask)) {
                p++;
                continue;
            }
            if (allowPctEncoded) {
                boolean enc = scanPctEncoded(input, p, n, c);
                if (enc) {
                    p += 3;
                    continue;
                }
            }
            break;
        }
        return p;
    }

    // Checks that each of the chars in [start, end) matches the given mask.
    static void checkChars(String input, int start, int end,
                           long lowMask, long highMask, String what) {
        int p = scan(input, start, end, lowMask, highMask);
        if (p < end)
            fail(input, "Illegal character in " + what, p);
    }

    // Checks that each of the chars in the given string matches the given mask.
    static void checkChars(String input,
                           long lowMask, long highMask, String what) {
        checkChars(input, 0, input.length(), lowMask, highMask, what);
    }

    // Checks that the char at position p matches the given mask.
    static void checkChar(String input, int p,
                          long lowMask, long highMask, String what) {
        if (!match(input.charAt(p), lowMask, highMask))
            fail(input, "Illegal character in " + what, p);
    }

    // Checks that the given host is a legal DNS host.
    // References: RFC 952, 1034, 1123, 2181
    static void checkDnsHost(String host) {
        int len = host.length();
        if (len == 0)
            throw new IllegalArgumentException("Empty host");
        if (len > 253)
            throw new IllegalArgumentException("Host length > 253 for DNS");
        int lastLabelStart = 0;
        boolean lastDash = false;
        char c;
        for (int i = 0; i <= len; i++) {
            if (i == len) {
                if (lastDash) break;
                if (i - lastLabelStart > 63) break;
                return;
            } else if ((c = host.charAt(i)) == '.') {
                if (lastLabelStart == i || lastDash) break;
                if (i - lastLabelStart > 63) break;
                lastLabelStart = i + 1;
                lastDash = false;
            } else if (c == '-') {
                if (lastLabelStart == i) break;
                lastDash = true;
            } else {
                if (!match(c, L_ALPHA | L_DIGIT, H_ALPHA | H_DIGIT))
                    break;
                lastDash = false;
            }
        }
        throw new IllegalArgumentException("Host syntax incompatible for DNS: " + host);
    }

    // Checks that the given substring contains a legal IPv6 address.
    // References: Section 3.2.2, RFC 3986; Section 2, RFC 6874
    static void checkIpv6Address(String s, int start, int n, boolean encoded)
            throws UriSyntaxException {
        int len = n - start;
        if (len < 2) failUSEExpecting(s, "IPv6 address", start);

        int p = scan(s, start, n, '%');
        if (p != n) {
            int z;
            if (encoded) {
                if (p + 2 >= n
                        || s.charAt(p + 1) != '2'
                        || s.charAt(p + 2) != '5') {
                    failUSEExpecting(s, "%25", p);
                }
                z = p + 3;
                if (scan(s, z, n, L_ZONE_ID, H_ZONE_ID) < n)
                    failUSE(s, "Illegal character in zone ID", z);
            } else z = p + 1;
            if (z == n)
                failUSEExpecting(s, "zone ID", z);

            n = p;
        }

        len = n - start;
        // longest: 0000:0000:0000:0000:0000:0000:255.255.255.255
        if (len < 2 || len > 45) failUSEExpecting(s, "IPv6 address", start);

        int minSeqCount = 0;
        int lastColon = start - 1;
        boolean compressed = false;

        for (int i = start; i <= n; i++) {
            char c;
            if (i == n) {
                if (lastColon != n - 1) {
                    minSeqCount++;
                } else if (!compressed) { // ending with single colon
                    failUSE(s, "Malformed IPv6 address", start);
                }
            } else if ((c = s.charAt(i)) == ':') {
                if (i == lastColon + 1) {
                    if (compressed)
                        failUSE(s, "Multiple compressions in IPv6 address", lastColon);
                    if (i == start) {
                        if (s.charAt(++i) != ':')
                            failUSE(s, "Malformed IPv6 address", start);
                    }
                    compressed = true;
                } else {
                    // hex seq len > 4
                    if (i - lastColon > 5)
                        failUSE(s, "Hex sequence too long", lastColon + 1);
                }
                minSeqCount++;
                lastColon = i;
            } else if (c == '.') {
                if (!isIpv4Address(s, lastColon + 1, n))
                    failUSEExpecting(s, "IPv4 address", lastColon + 1);
                minSeqCount += 2;
                break;
            } else if (!match(c, L_HEXDIG, H_HEXDIG)) {
                failUSE(s, "Illegal character in IPv6 address", i);
            }
        }
        if (minSeqCount > 8)
            failUSE(s, "IPv6 address too long", start);
    }

    // IPv4address = dec-octet "." dec-octet "." dec-octet "." dec-octet
    private static boolean isIpv4Address(String s, int start, int n) {
        int len = n - start;
        // shortest: 0.0.0.0
        // longest: 255.255.255.255
        if (len < 7 || len > 15) return false;

        int lastDot = start - 1;
        int dotCnt = 0;

        for (int i = start; i < n; i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (dotCnt > 3) return false;
                if (!isDecOctet(s, lastDot + 1, i))
                    return false;
                dotCnt++;
                lastDot = i;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return dotCnt == 3 && isDecOctet(s, lastDot + 1, n);
    }

    // dec-octet   = DIGIT                 ; 0-9
    //             / %x31-39 DIGIT         ; 10-99
    //             / "1" 2DIGIT            ; 100-199
    //             / "2" %x30-34 DIGIT     ; 200-249
    //             / "25" %x30-35          ; 250-255
    private static boolean isDecOctet(String s, int start, int n) {
        int len = n - start;
        if (len == 0 || len > 3) return false;
        if (len == 1) return true;

        char c1 = s.charAt(start);
        if (c1 == '0') return false;
        if (len == 2) return true;
        char c2 = s.charAt(start + 1);
        char c3 = s.charAt(start + 2);
        if (c1 == '2') {
            if (c2 == '5') {
                return c3 <= '5';
            } else return c2 <= '5';
        } else return c1 == '1';
    }
}
