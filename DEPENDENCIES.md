# Runtime dependencies

The SDK ZIP includes these libraries in `lib/`:

| Dependency | Version |
| --- | --- |
| `com.squareup.okhttp3:okhttp` | `3.14.9` |
| `com.squareup.okio:okio` | `1.17.2` |
| `org.json:json` | `20231013` |

For Maven, install the SDK with the supplied POM as shown in [README](README.md); Maven resolves these dependencies automatically. For a manual classpath, include the SDK JAR and all dependency JARs from `lib/`.
