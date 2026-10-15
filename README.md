# MyVocal V2 WebSocket TTS SDK for Java

Stream text to speech with `Skyblight-Turbo`. Requires Java 8 or later, an enterprise TTS API Key with dedicated concurrency, and a voice ID.

## Install

Download and extract [SDK v1.0.2](https://github.com/MyVocal-AI/myvocal-v2-wss-sdk-java/releases/tag/v1.0.2). The ZIP includes the SDK and runtime dependencies in `lib/`. For Maven, install the SDK with the supplied POM:

```bash
mvn install:install-file -Dfile=lib/myvocal-v2-wss-sdk-1.0.2.jar -DpomFile=pom.xml
```

Add this dependency to your application:

```xml
<dependency>
  <groupId>ai.myvocal</groupId>
  <artifactId>myvocal-v2-wss-sdk</artifactId>
  <version>1.0.2</version>
</dependency>
```

Maven resolves runtime dependencies using the installed POM. For a manual classpath, include all JARs in `lib/`; see [runtime dependencies](DEPENDENCIES.md).

## Run the example

Set your API Key and voice ID:

```bash
export MYVOCAL_TTS_API_KEY='YOUR_API_KEY'
export MYVOCAL_VOICE_ID='YOUR_VOICE_ID'
```

PowerShell:

```powershell
$env:MYVOCAL_TTS_API_KEY='YOUR_API_KEY'
$env:MYVOCAL_VOICE_ID='YOUR_VOICE_ID'
```

From the extracted folder, on macOS/Linux:

```bash
javac -cp "lib/*" examples/BasicExample.java
java -cp "lib/*:examples" BasicExample
```

On Windows:

```powershell
javac -cp "lib/*" examples/BasicExample.java
java -cp "lib/*;examples" BasicExample
```

Running the example makes a real synthesis request and consumes character usage. It prints the final audio byte count and close code. To save or play audio, use the callbacks or the completed audio result described in the usage guide.

## Use in your application

Connect, start a task, send text fragments, and call finish. Finish waits for `task_finished` and Close Code `1000`. Sentence-end events and individual audio-final flags do not indicate completion of the whole task.

- [Usage guide](USAGE.md): options, audio callbacks, subtitles, cancellation, timeouts, and errors.
- [API reference](https://api-docs.myvocal.ai/3891085w0): request and response messages.
- [Character usage](https://api-docs.myvocal.ai/336818975e0).

The SDK does not automatically replay submitted text after a connection failure.

Licensed under [Apache License 2.0](LICENSE). Dependency notices are in [NOTICE](NOTICE).
