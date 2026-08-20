package ai.myvocal.tts.v2wss;

import org.json.JSONObject;

public final class TimbreWeight {
    private final String voiceId;
    private final int weight;

    public TimbreWeight(String voiceId, int weight) {
        if (voiceId == null || voiceId.isEmpty()) {
            throw new IllegalArgumentException("voiceId is required");
        }
        VoiceSetting.checkRange("weight", weight, 1, 100, true);
        this.voiceId = voiceId;
        this.weight = weight;
    }

    JSONObject toJson() {
        return new JSONObject()
                .put("voice_id", voiceId)
                .put("weight", weight);
    }
}
