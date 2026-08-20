package ai.myvocal.tts.v2wss;

import org.json.JSONObject;

public final class VoiceModify {
    private final Integer pitch;
    private final Integer intensity;
    private final Integer timbre;
    private final String soundEffects;

    private VoiceModify(Builder builder) {
        VoiceSetting.checkRange(
                "voice_modify.pitch", builder.pitch, -100, 100, true);
        VoiceSetting.checkRange(
                "voice_modify.intensity", builder.intensity, -100, 100, true);
        VoiceSetting.checkRange(
                "voice_modify.timbre", builder.timbre, -100, 100, true);
        if (builder.soundEffects != null
                && !Values.SOUND_EFFECTS.contains(builder.soundEffects)) {
            throw new IllegalArgumentException(
                    "unsupported sound effect: " + builder.soundEffects);
        }
        this.pitch = builder.pitch;
        this.intensity = builder.intensity;
        this.timbre = builder.timbre;
        this.soundEffects = builder.soundEffects;
    }

    public static Builder builder() {
        return new Builder();
    }

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (pitch != null) json.put("pitch", pitch);
        if (intensity != null) json.put("intensity", intensity);
        if (timbre != null) json.put("timbre", timbre);
        if (soundEffects != null) json.put("sound_effects", soundEffects);
        return json;
    }

    public static final class Builder {
        private Integer pitch;
        private Integer intensity;
        private Integer timbre;
        private String soundEffects;

        public Builder pitch(int value) {
            this.pitch = value;
            return this;
        }

        public Builder intensity(int value) {
            this.intensity = value;
            return this;
        }

        public Builder timbre(int value) {
            this.timbre = value;
            return this;
        }

        public Builder soundEffects(String value) {
            this.soundEffects = value;
            return this;
        }

        public VoiceModify build() {
            return new VoiceModify(this);
        }
    }
}
