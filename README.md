# MyVocal V2 WebSocket TTS SDK for Java

The SDK is compiled with `--release 8` and delivered as one thin SDK Jar.

Formally tested runtimes:

- Java 8
- Java 11
- Java 17
- Java 21
- Java 25

Other Java versions rely on JVM upward bytecode compatibility and are not
tested separately.

## Dependencies

This is a thin Jar. The application must supply OkHttp, Okio, and JSON-java at
runtime; running the SDK Jar alone will produce `NoClassDefFoundError`.

From the downloaded ZIP, install the SDK Jar into the local Maven repository:

```bash
mvn install:install-file -Dfile=lib/myvocal-v2-wss-sdk-1.0.1.jar -DgroupId=ai.myvocal -DartifactId=myvocal-v2-wss-sdk -Dversion=1.0.1 -Dpackaging=jar
```

Then declare:

```xml
<dependency>
  <groupId>ai.myvocal</groupId>
  <artifactId>myvocal-v2-wss-sdk</artifactId>
  <version>1.0.1</version>
</dependency>
<dependency>
  <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp</artifactId>
  <version>3.14.9</version>
</dependency>
<dependency>
  <groupId>org.json</groupId>
  <artifactId>json</artifactId>
  <version>20231013</version>
</dependency>
```

The `ai.myvocal` coordinate is available to other projects only after the
local install above or a future public registry release.

## Example

```java
V2TtsClient client = new V2TtsClient(
        System.getenv("MYVOCAL_TTS_API_KEY"),
        new V2TtsAdapter());
try {
    client.connect(30, TimeUnit.SECONDS);
    client.start(StartRequest.builder(
                    System.getenv("MYVOCAL_VOICE_ID"))
            .language("en")
            .voiceSetting(VoiceSetting.builder()
                    .speed(1.0)
                    .volume(1.0)
                    .pitch(0)
                    .emotion("calm")
                    .build())
            .subtitle(true, "word_streaming")
            .sessionId("java-example-001")
            .build(), 30, TimeUnit.SECONDS);
    client.sendText("Hello from MyVocal.");
    client.flush(30, TimeUnit.SECONDS);
    CompletionResult result = client.finish(60, TimeUnit.SECONDS);
    System.out.println(result.getAudio().length);
} finally {
    client.close();
}
```

No JDK internal APIs are used. TLS certificate and hostname verification use
the JVM and OkHttp defaults and are never disabled.

`connect()` succeeds on the WebSocket handshake. Its legacy return value may
be `null` when `connected_success` arrives later; use the connection state and
listener events instead. Listener callbacks run synchronously on the OkHttp
WebSocket reader, must return quickly, and must not call blocking client
operations. Callback exceptions are reported to `onError` without changing
the TTS protocol result.

## License

Apache License 2.0. See `LICENSE` and `NOTICE` in the SDK package.
