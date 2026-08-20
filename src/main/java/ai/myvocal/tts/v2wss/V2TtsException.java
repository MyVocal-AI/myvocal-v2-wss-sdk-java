package ai.myvocal.tts.v2wss;

public final class V2TtsException extends Exception {
    private final String errorCode;
    private final Integer statusCode;
    private final String statusMessage;

    public V2TtsException(String message) {
        this(message, null, null, null, null);
    }

    public V2TtsException(String message, Throwable cause) {
        this(message, null, null, null, cause);
    }

    public V2TtsException(String message, String errorCode,
                          Integer statusCode, String statusMessage) {
        this(message, errorCode, statusCode, statusMessage, null);
    }

    private V2TtsException(String message, String errorCode,
                           Integer statusCode, String statusMessage,
                           Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
