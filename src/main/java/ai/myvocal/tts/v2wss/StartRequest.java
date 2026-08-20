package ai.myvocal.tts.v2wss;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StartRequest {
    private final String voice;
    private final String language;
    private final String format;
    private final int sampleRate;
    private final Integer bitrate;
    private final int channel;
    private final VoiceSetting voiceSetting;
    private final PronunciationDict pronunciationDict;
    private final List<TimbreWeight> timbreWeights;
    private final VoiceModify voiceModify;
    private final boolean subtitleEnable;
    private final String subtitleType;
    private final String sessionId;

    private StartRequest(Builder builder) {
        if (builder.voice == null || builder.voice.isEmpty()) {
            throw new IllegalArgumentException("voice is required");
        }
        if (!Values.FORMATS.contains(builder.format)) {
            throw new IllegalArgumentException(
                    "unsupported format: " + builder.format);
        }
        if (!Values.SAMPLE_RATES.contains(builder.sampleRate)) {
            throw new IllegalArgumentException(
                    "unsupported sample rate: " + builder.sampleRate);
        }
        if (builder.bitrate != null
                && !Values.BITRATES.contains(builder.bitrate)) {
            throw new IllegalArgumentException(
                    "unsupported bitrate: " + builder.bitrate);
        }
        if (!Values.CHANNELS.contains(builder.channel)) {
            throw new IllegalArgumentException(
                    "unsupported channel: " + builder.channel);
        }
        if (!Values.SUBTITLE_TYPES.contains(builder.subtitleType)) {
            throw new IllegalArgumentException(
                    "unsupported subtitle type: " + builder.subtitleType);
        }
        if (!builder.timbreWeights.isEmpty()
                && (builder.timbreWeights.size() < 1
                || builder.timbreWeights.size() > 4)) {
            throw new IllegalArgumentException(
                    "timbre weights must contain 1 to 4 items");
        }
        if (builder.voiceModify != null && !"mp3".equals(builder.format)) {
            throw new IllegalArgumentException(
                    "voice_modify is supported only for mp3");
        }
        if (builder.bitrate != null && !"mp3".equals(builder.format)) {
            throw new IllegalArgumentException(
                    "bitrate is supported only for mp3");
        }
        this.voice = builder.voice;
        this.language = builder.language;
        this.format = builder.format;
        this.sampleRate = builder.sampleRate;
        this.bitrate = builder.bitrate;
        this.channel = builder.channel;
        this.voiceSetting = builder.voiceSetting;
        this.pronunciationDict = builder.pronunciationDict;
        this.timbreWeights = Collections.unmodifiableList(
                new ArrayList<TimbreWeight>(builder.timbreWeights));
        this.voiceModify = builder.voiceModify;
        this.subtitleEnable = builder.subtitleEnable;
        this.subtitleType = builder.subtitleType;
        this.sessionId = builder.sessionId;
    }

    public static Builder builder(String voice) {
        return new Builder(voice);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject()
                .put("event", "task_start")
                .put("model", "Skyblight-Turbo")
                .put("voice", voice)
                .put("format", format)
                .put("sample_rate", sampleRate)
                .put("channel", channel)
                .put("priority", "dedicated_concurrency")
                .put("subtitle_enable", subtitleEnable)
                .put("subtitle_type", subtitleType);
        if ("mp3".equals(format)) {
            json.put("bitrate", bitrate == null ? 128000 : bitrate);
        }
        if (language != null) json.put("language", language);
        if (voiceSetting != null) {
            json.put("voice_setting", voiceSetting.toJson());
        }
        if (pronunciationDict != null) {
            json.put("pronunciation_dict", pronunciationDict.toJson());
        }
        if (!timbreWeights.isEmpty()) {
            JSONArray items = new JSONArray();
            for (TimbreWeight item : timbreWeights) {
                items.put(item.toJson());
            }
            json.put("timbre_weights", items);
        }
        if (voiceModify != null) {
            json.put("voice_modify", voiceModify.toJson());
        }
        if (sessionId != null) json.put("session_id", sessionId);
        return json;
    }

    String getSessionId() {
        return sessionId;
    }

    public static final class Builder {
        private final String voice;
        private String language;
        private String format = "mp3";
        private int sampleRate = 32000;
        private Integer bitrate;
        private int channel = 1;
        private VoiceSetting voiceSetting;
        private PronunciationDict pronunciationDict;
        private List<TimbreWeight> timbreWeights =
                new ArrayList<TimbreWeight>();
        private VoiceModify voiceModify;
        private boolean subtitleEnable;
        private String subtitleType = "sentence";
        private String sessionId;

        private Builder(String voice) {
            this.voice = voice;
        }

        public Builder language(String value) {
            this.language = value;
            return this;
        }

        public Builder format(String value) {
            this.format = value;
            return this;
        }

        public Builder sampleRate(int value) {
            this.sampleRate = value;
            return this;
        }

        public Builder bitrate(int value) {
            this.bitrate = value;
            return this;
        }

        public Builder channel(int value) {
            this.channel = value;
            return this;
        }

        public Builder voiceSetting(VoiceSetting value) {
            this.voiceSetting = value;
            return this;
        }

        public Builder pronunciationDict(PronunciationDict value) {
            this.pronunciationDict = value;
            return this;
        }

        public Builder timbreWeights(List<TimbreWeight> value) {
            this.timbreWeights = value == null
                    ? new ArrayList<TimbreWeight>()
                    : new ArrayList<TimbreWeight>(value);
            return this;
        }

        public Builder voiceModify(VoiceModify value) {
            this.voiceModify = value;
            return this;
        }

        public Builder subtitle(boolean enabled, String type) {
            this.subtitleEnable = enabled;
            this.subtitleType = type;
            return this;
        }

        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        public StartRequest build() {
            return new StartRequest(this);
        }
    }
}
