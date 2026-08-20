# Runtime Dependencies

`myvocal-v2-wss-sdk-1.0.1.jar` is a thin SDK Jar. The business application
must provide:

```text
com.squareup.okhttp3:okhttp:3.14.9
org.json:json:20231013
```

OkHttp also resolves:

```text
com.squareup.okio:okio:1.17.2
```

Maven and Gradle users should declare the first two dependencies normally and
allow their build tool to resolve transitive dependencies.

Do not replace the default JVM/OkHttp trust manager or hostname verifier.
