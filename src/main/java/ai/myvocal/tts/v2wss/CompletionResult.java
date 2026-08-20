package ai.myvocal.tts.v2wss;

public final class CompletionResult {
    private final byte[] audio;
    private final int closeCode;
    private final String sessionId;

    public CompletionResult(byte[] audio, int closeCode, String sessionId) {
        this.audio = audio.clone();
        this.closeCode = closeCode;
        this.sessionId = sessionId;
    }

    public byte[] getAudio() {
        return audio.clone();
    }

    public int getCloseCode() {
        return closeCode;
    }

    public String getSessionId() {
        return sessionId;
    }
}
