package ai.myvocal.tts.v2wss;

import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class LiveProbeTest {
    @Test
    public void liveFullFieldSynthesis() throws Exception {
        String apiKey = System.getenv("MYVOCAL_TTS_API_KEY");
        String voiceId = System.getenv("MYVOCAL_VOICE_ID");
        Assume.assumeTrue(apiKey != null && !apiKey.isEmpty());
        Assume.assumeTrue(voiceId != null && !voiceId.isEmpty());

        final Map<String, Integer> eventCounts =
                new ConcurrentHashMap<String, Integer>();
        final java.util.List<V2TtsException> errors =
                new java.util.ArrayList<V2TtsException>();
        V2TtsListener listener = new V2TtsAdapter() {
            @Override
            public void onEvent(ServerEvent event) {
                Integer count = eventCounts.get(event.getEvent());
                eventCounts.put(event.getEvent(),
                        count == null ? 1 : count + 1);
            }

            @Override
            public void onError(V2TtsException error) {
                errors.add(error);
            }
        };

        String sessionId = "sdk-java-" + System.currentTimeMillis();
        V2TtsClient client = new V2TtsClient(apiKey, listener);
        try {
            client.connect(30, TimeUnit.SECONDS);
            StartRequest request = StartRequest.builder(voiceId)
                    .language("en")
                    .format("mp3")
                    .sampleRate(32000)
                    .bitrate(128000)
                    .channel(1)
                    .voiceSetting(VoiceSetting.builder()
                            .speed(1.0)
                            .volume(1.0)
                            .pitch(0)
                            .emotion("calm")
                            .englishNormalization(true)
                            .latexRead(false)
                            .build())
                    .pronunciationDict(new PronunciationDict(
                            Collections.singletonList(
                                    "MyVocal/My Vocal")))
                    .timbreWeights(Collections.singletonList(
                            new TimbreWeight(voiceId, 100)))
                    .voiceModify(VoiceModify.builder()
                            .pitch(0)
                            .intensity(0)
                            .timbre(0)
                            .soundEffects("robotic")
                            .build())
                    .subtitle(true, "word_streaming")
                    .sessionId(sessionId)
                    .build();
            client.start(request, 30, TimeUnit.SECONDS);
            client.sendText(
                    "MyVocal live Java SDK validation is complete.");
            client.flush(60, TimeUnit.SECONDS);
            CompletionResult result =
                    client.finish(60, TimeUnit.SECONDS);

            assertTrue(result.getAudio().length > 0);
            assertEquals(1000, result.getCloseCode());
            assertEquals(sessionId, result.getSessionId());
            assertTrue(errors.isEmpty());
            assertEquals(Integer.valueOf(1),
                    eventCounts.get("task_finished"));

            JSONObject safe = new JSONObject()
                    .put("sdk", "java")
                    .put("passed", true)
                    .put("audioBytes", result.getAudio().length)
                    .put("audioSha256", sha256(result.getAudio()))
                    .put("closeCode", result.getCloseCode())
                    .put("sessionEcho",
                            sessionId.equals(result.getSessionId()))
                    .put("eventCounts", new JSONObject(eventCounts));
            System.out.println("LIVE_RESULT=" + safe.toString());
        } finally {
            client.close();
        }
    }

    private static String sha256(byte[] value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
        StringBuilder result = new StringBuilder();
        for (byte item : digest) {
            result.append(String.format("%02x", item & 0xff));
        }
        return result.toString();
    }
}
