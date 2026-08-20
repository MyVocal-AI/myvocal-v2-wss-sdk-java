package ai.myvocal.tts.v2wss;

public interface V2TtsListener {
    void onEvent(ServerEvent event);

    void onAudio(byte[] audio, ServerEvent event);

    void onError(V2TtsException error);
}
