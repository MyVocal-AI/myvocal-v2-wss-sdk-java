package ai.myvocal.tts.v2wss;

import org.json.JSONObject;

public final class VoiceSetting {
    private final Double speed;
    private final Double volume;
    private final Integer pitch;
    private final String emotion;
    private final Boolean englishNormalization;
    private final Boolean latexRead;

    private VoiceSetting(Builder builder) {
        checkRange("voice_setting.speed", builder.speed, 0.5, 2.0, true);
        checkRange("voice_setting.vol", builder.volume, 0.0, 10.0, false);
        checkRange("voice_setting.pitch", builder.pitch, -12, 12, true);
        if (builder.emotion != null
                && !Values.EMOTIONS.contains(builder.emotion)) {
            throw new IllegalArgumentException(
                    "unsupported emotion: " + builder.emotion);
        }
        this.speed = builder.speed;
        this.volume = builder.volume;
        this.pitch = builder.pitch;
        this.emotion = builder.emotion;
        this.englishNormalization = builder.englishNormalization;
        this.latexRead = builder.latexRead;
    }

    public static Builder builder() {
        return new Builder();
    }

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (speed != null) json.put("speed", speed);
        if (volume != null) json.put("vol", volume);
        if (pitch != null) json.put("pitch", pitch);
        if (emotion != null) json.put("emotion", emotion);
        if (englishNormalization != null) {
            json.put("english_normalization", englishNormalization);
        }
        if (latexRead != null) json.put("latex_read", latexRead);
        return json;
    }

    static void checkRange(String name, Number value, double low,
                           double high, boolean inclusiveLow) {
        if (value == null) return;
        double number = value.doubleValue();
        if ((inclusiveLow ? number < low : number <= low) || number > high) {
            throw new IllegalArgumentException(name + " is out of range");
        }
    }

    public static final class Builder {
        private Double speed;
        private Double volume;
        private Integer pitch;
        private String emotion;
        private Boolean englishNormalization;
        private Boolean latexRead;

        public Builder speed(double value) {
            this.speed = value;
            return this;
        }

        public Builder volume(double value) {
            this.volume = value;
            return this;
        }

        public Builder pitch(int value) {
            this.pitch = value;
            return this;
        }

        public Builder emotion(String value) {
            this.emotion = value;
            return this;
        }

        public Builder englishNormalization(boolean value) {
            this.englishNormalization = value;
            return this;
        }

        public Builder latexRead(boolean value) {
            this.latexRead = value;
            return this;
        }

        public VoiceSetting build() {
            return new VoiceSetting(this);
        }
    }
}
