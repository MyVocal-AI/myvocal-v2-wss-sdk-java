package ai.myvocal.tts.v2wss;

public class V2TtsAdapter implements V2TtsListener {
    @Override
    public void onEvent(ServerEvent event) {
    }

    @Override
    public void onAudio(byte[] audio, ServerEvent event) {
    }

    @Override
    public void onError(V2TtsException error) {
    }
}
