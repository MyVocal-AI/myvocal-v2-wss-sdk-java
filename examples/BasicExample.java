import ai.myvocal.tts.v2wss.CompletionResult;
import ai.myvocal.tts.v2wss.StartRequest;
import ai.myvocal.tts.v2wss.V2TtsAdapter;
import ai.myvocal.tts.v2wss.V2TtsClient;
import ai.myvocal.tts.v2wss.VoiceSetting;

import java.util.concurrent.TimeUnit;

public final class BasicExample {
    public static void main(String[] args) throws Exception {
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
            CompletionResult result =
                    client.finish(60, TimeUnit.SECONDS);
            System.out.println("audio_bytes="
                    + result.getAudio().length
                    + " close_code=" + result.getCloseCode());
        } finally {
            client.close();
        }
    }
}
