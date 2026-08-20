package ai.myvocal.tts.v2wss;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class StartRequestTest {
    @Test
    public void serializesEverySupportedFieldUsingWireNames() {
        StartRequest request = StartRequest.builder("voice-a")
                .language("en")
                .format("mp3")
                .sampleRate(32000)
                .bitrate(128000)
                .channel(1)
                .voiceSetting(VoiceSetting.builder()
                        .speed(1.1)
                        .volume(1.2)
                        .pitch(1)
                        .emotion("calm")
                        .englishNormalization(true)
                        .latexRead(false)
                        .build())
                .pronunciationDict(new PronunciationDict(
                        Collections.singletonList("MyVocal/My Vocal")))
                .timbreWeights(Collections.singletonList(
                        new TimbreWeight("voice-a", 100)))
                .voiceModify(VoiceModify.builder()
                        .pitch(10)
                        .intensity(10)
                        .timbre(10)
                        .soundEffects("robotic")
                        .build())
                .subtitle(true, "word_streaming")
                .sessionId("session-001")
                .build();

        JSONObject wire = request.toJson();

        assertEquals("task_start", wire.getString("event"));
        assertEquals("Skyblight-Turbo", wire.getString("model"));
        assertEquals("dedicated_concurrency", wire.getString("priority"));
        assertTrue(wire.getJSONObject("voice_setting")
                .getBoolean("english_normalization"));
        assertEquals("MyVocal/My Vocal", wire
                .getJSONObject("pronunciation_dict")
                .getJSONArray("tone").getString(0));
        assertEquals("voice-a", wire.getJSONArray("timbre_weights")
                .getJSONObject(0).getString("voice_id"));
        assertEquals("robotic", wire.getJSONObject("voice_modify")
                .getString("sound_effects"));
        assertFalse(wire.has("continuous_sound"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOpus() {
        StartRequest.builder("voice-a").format("opus").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidSampleRate() {
        StartRequest.builder("voice-a").sampleRate(12345).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidChannel() {
        StartRequest.builder("voice-a").channel(3).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsVoiceSettingRange() {
        VoiceSetting.builder().speed(2.1).build();
    }

    @Test
    public void acceptsEveryDocumentedEnumValue() {
        String[] formats = {
                "mp3", "pcm", "wav", "flac", "pcmu_raw", "pcmu_wav"
        };
        for (String format : formats) {
            JSONObject wire = StartRequest.builder("voice-a")
                    .format(format).build().toJson();
            assertEquals(format, wire.getString("format"));
            assertEquals("mp3".equals(format), wire.has("bitrate"));
        }
        int[] sampleRates = {8000, 16000, 22050, 24000, 32000, 44100};
        for (int sampleRate : sampleRates) {
            assertEquals(sampleRate, StartRequest.builder("voice-a")
                    .sampleRate(sampleRate).build().toJson()
                    .getInt("sample_rate"));
        }
        int[] bitrates = {32000, 64000, 128000, 256000};
        for (int bitrate : bitrates) {
            assertEquals(bitrate, StartRequest.builder("voice-a")
                    .bitrate(bitrate).build().toJson().getInt("bitrate"));
        }
        for (int channel : new int[]{1, 2}) {
            assertEquals(channel, StartRequest.builder("voice-a")
                    .channel(channel).build().toJson().getInt("channel"));
        }
        for (String subtitle : new String[]{
                "sentence", "word", "word_streaming"
        }) {
            assertEquals(subtitle, StartRequest.builder("voice-a")
                    .subtitle(true, subtitle).build().toJson()
                    .getString("subtitle_type"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBitrateForNonMp3() {
        StartRequest.builder("voice-a")
                .format("wav").bitrate(128000).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsVoiceModifyForNonMp3() {
        StartRequest.builder("voice-a")
                .format("pcm")
                .voiceModify(VoiceModify.builder().pitch(1).build())
                .build();
    }
}
