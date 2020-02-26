# fluent-uri

fluent-uri is an [RFC 3986](https://www.ietf.org/rfc/rfc3986.html) compliant fluent-designed Java library for URI parsing, building, resolving and normalizing.

This project is still under development, contributions are welcome.

![Java CI](https://github.com/yescallop/uriutils/workflows/Java%20CI/badge.svg)

## Features
 - Strict syntax validating
 - Immutable URI references presented as **Uri** instances
 - Mutable builders which can be created from **Uri** instances
 - Building **Uri**s with raw / encoded components, path segments and query parameters
 - Supporting IDNA encoding ([RFC 3490](https://www.ietf.org/rfc/rfc3490.html)) and DNS syntax checking for host component
 - Supporting IPv6 scoped addresses ([RFC 6874](https://www.ietf.org/rfc/rfc6874.html))
 - Reference resolution ([Section 5](https://www.ietf.org/rfc/rfc3986.html#section-5))
 - Path normalization ([Section 5.2.4](https://www.ietf.org/rfc/rfc3986.html#section-5.2.4))
 - Utilities for percent en/decoding ([Section 2.1](https://www.ietf.org/rfc/rfc3986.html#section-2.1))

## Attention
 - URI components are not decoded until getters for decoded components are called.
 - Path component is always present, thus never `null`,
 while, if not present, port and the other components could be `-1` or `null`.
 - Occurrence of encoded slash "**%2F**" in the path is permitted, but literally decoded as "**/**" in `Uri#path()`,
 while `Uri#pathSegments()` handles it correctly.

## Examples

### Parsing
Hierarchical example:
```java
Uri u = Uri.from("http://u%40@xn--h28h.com:80/fo%20o//bar/?k=v+1&k=v%262&k2&=#%23");
u.scheme(); // 'http'
u.encodedUserInfo(); // 'u%40'
u.userInfo(); // 'u@'
u.encodedHost(); // 'xn--h28h.com'
u.host(); // '😃.com'
u.port(); // 80
u.encodedPath(); // '/fo%20o//bar/'
u.path(); // '/fo o//bar/'
u.pathSegments(); // {'fo o', '', 'bar', ''}
u.encodedQuery(); // 'k=v+1&k=v%262&k2&='
u.queryParameters(); // {'k'={'v 1', 'v&2'}, 'k2'={null}, ''={''}}
u.encodedFragment(); // '%23'
u.fragment(); // '#'
```
Opaque example:
```java
Uri u = Uri.from("mailto:user@example.com?subject=test&body=parse");
u.scheme(); // 'mailto'
u.encodedUserInfo(); u.userInfo(); u.encodedHost(); u.host(); // null
u.port(); // -1
u.encodedPath(); u.path(); // 'user@example.com'
u.encodedQuery(); // 'subject=test&body=parse'
u.queryParameters(); // {'subject'={'test'}, 'body'={'parse'}}
u.encodedFragment(); u.fragment(); // null
```
### Building
```java
Uri.newBuilder()
    .scheme("http")
    .encodedUserInfo("u%40")
    .host("😃.com").port(80)
    .path("/fo o")
    .appendPathSegment("")
    .appendPathSegment("")
    .appendPathSegment("bar")
    .appendPathSegment("")
    .encodedQuery("k=v+1")
    .appendQueryParameter("k", "v&2")
    .appendQueryParameter("k2", null)
    .appendQueryParameter("", "")
    .fragment("#").build();
// 'http://u%40@xn--h28h.com:80/fo%20o//bar/?k=v+1&k=v%262&k2&=#%23'
```

### Resolving
```java
Uri u = Uri.from("http://a/b/c/d;p?q#r");
u.resolve("g"); // 'http://a/b/c/g'
u.resolve("../g"); // 'http://a/b/g'
u.resolve("."); // 'http://a/b/c/'
u.resolve("/"); // 'http://a/'
```

### Normalizing
```java
Uri.from("a/b/../../").normalize(); // '.'
Uri.from("a/./../b/./c/d/..").normalize(); // 'b/c/'
Uri.from("http://a/b/c/../d").normalize(); // 'http://a/b/d'
```
