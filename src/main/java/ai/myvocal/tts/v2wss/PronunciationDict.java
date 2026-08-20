package ai.myvocal.tts.v2wss;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PronunciationDict {
    private final List<String> tone;

    public PronunciationDict(List<String> tone) {
        if (tone == null) {
            throw new IllegalArgumentException("tone is required");
        }
        for (String item : tone) {
            if (item == null || item.isEmpty()) {
                throw new IllegalArgumentException(
                        "tone must contain non-empty strings");
            }
        }
        this.tone = Collections.unmodifiableList(
                new ArrayList<String>(tone));
    }

    JSONObject toJson() {
        return new JSONObject().put("tone", new JSONArray(tone));
    }
}
