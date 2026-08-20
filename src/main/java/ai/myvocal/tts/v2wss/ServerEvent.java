package ai.myvocal.tts.v2wss;

import org.json.JSONObject;

public final class ServerEvent {
    private final String event;
    private final String sessionId;
    private final String connectId;
    private final String traceId;
    private final Boolean finalEvent;
    private final byte[] audio;
    private final JSONObject data;
    private final JSONObject baseResp;
    private final String errorCode;
    private final String error;
    private final JSONObject raw;

    private ServerEvent(JSONObject json) {
        this.event = json.optString("event", null);
        if (event == null || event.isEmpty()) {
            throw new IllegalArgumentException("server event is missing event");
        }
        this.sessionId = json.optString("session_id", null);
        this.connectId = json.optString("connect_id", null);
        this.traceId = json.optString("trace_id", null);
        this.finalEvent = json.has("is_final")
                ? Boolean.valueOf(json.optBoolean("is_final")) : null;
        this.data = json.optJSONObject("data") == null
                ? new JSONObject() : json.optJSONObject("data");
        this.baseResp = json.optJSONObject("base_resp") == null
                ? new JSONObject() : json.optJSONObject("base_resp");
        this.errorCode = json.has("error_code")
                ? String.valueOf(json.get("error_code")) : null;
        this.error = json.optString("error", null);
        String audioHex = data.optString("audio", "");
        this.audio = decodeHex(audioHex);
        this.raw = new JSONObject(json.toString());
    }

    public static ServerEvent fromJson(String text) {
        return new ServerEvent(new JSONObject(text));
    }

    public String getEvent() {
        return event;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getConnectId() {
        return connectId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Boolean isFinalEvent() {
        return finalEvent;
    }

    public byte[] getAudio() {
        return audio.clone();
    }

    public JSONObject getData() {
        return new JSONObject(data.toString());
    }

    public JSONObject getRaw() {
        return new JSONObject(raw.toString());
    }

    V2TtsException asException() {
        Integer statusCode = baseResp.has("status_code")
                ? Integer.valueOf(baseResp.optInt("status_code")) : null;
        String statusMessage = baseResp.optString("status_msg", null);
        String detail = error != null ? error
                : statusMessage != null ? statusMessage : "task failed";
        String code = errorCode != null ? errorCode
                : statusCode != null ? String.valueOf(statusCode)
                : "protocol_error";
        return new V2TtsException(
                code + ": " + detail, errorCode, statusCode, statusMessage);
    }

    private static byte[] decodeHex(String value) {
        if (value == null || value.isEmpty()) return new byte[0];
        if ((value.length() & 1) != 0) {
            throw new IllegalArgumentException("audio hex has odd length");
        }
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            int high = Character.digit(value.charAt(i), 16);
            int low = Character.digit(value.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("audio is not valid hex");
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }
}
