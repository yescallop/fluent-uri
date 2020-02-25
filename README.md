# fluent-uri

fluent-uri is an [RFC 3986](https://www.ietf.org/rfc/rfc3986.html) compliant fluent-designed Java library for URI parsing, building, resolving and normalizing.

![Java CI](https://github.com/yescallop/uriutils/workflows/Java%20CI/badge.svg)

## Features
 - Strict syntax validating
 - Immutable URI references presented as **Uri** instances
 - Mutable builders which can be created from **Uri** instances
 - Building **Uri**s with raw / encoded components, path segments and query parameters
 - Supporting IDNA encoding ([RFC 3490]((https://www.ietf.org/rfc/rfc3490.html))) and DNS syntax checking for host component
 - Supporting IPv6 scoped addresses ([RFC 6874](https://www.ietf.org/rfc/rfc6874.html))
 - Reference resolution ([Section 5](https://www.ietf.org/rfc/rfc3986.html#section-5))
 - Path normalization ([Section 5.2.4](https://www.ietf.org/rfc/rfc3986.html#section-5.2.4))
 - Utilities for percent en/decoding ([Section 2.1](https://www.ietf.org/rfc/rfc3986.html#section-2.1))

This project is still under development, contributions are welcome.