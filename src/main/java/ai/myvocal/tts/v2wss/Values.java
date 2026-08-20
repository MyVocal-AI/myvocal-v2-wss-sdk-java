package ai.myvocal.tts.v2wss;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class Values {
    static final Set<String> FORMATS = strings(
            "mp3", "pcm", "wav", "flac", "pcmu_raw", "pcmu_wav");
    static final Set<Integer> SAMPLE_RATES = integers(
            8000, 16000, 22050, 24000, 32000, 44100);
    static final Set<Integer> BITRATES = integers(
            32000, 64000, 128000, 256000);
    static final Set<Integer> CHANNELS = integers(1, 2);
    static final Set<String> EMOTIONS = strings(
            "happy", "sad", "angry", "fearful", "disgusted",
            "surprised", "calm");
    static final Set<String> SOUND_EFFECTS = strings(
            "spacious_echo", "auditorium_echo", "lofi_telephone",
            "robotic");
    static final Set<String> SUBTITLE_TYPES = strings(
            "sentence", "word", "word_streaming");

    private Values() {
    }

    private static Set<String> strings(String... values) {
        return Collections.unmodifiableSet(
                new HashSet<String>(Arrays.asList(values)));
    }

    private static Set<Integer> integers(Integer... values) {
        return Collections.unmodifiableSet(
                new HashSet<Integer>(Arrays.asList(values)));
    }
}
