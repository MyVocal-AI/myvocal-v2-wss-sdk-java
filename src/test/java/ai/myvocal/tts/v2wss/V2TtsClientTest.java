package ai.myvocal.tts.v2wss;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.ByteString;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class V2TtsClientTest {
    @Test
    public void completesLifecycleDecodesAudioAndAcceptsClose1000()
            throws Exception {
        MockWebServer server = new MockWebServer();
        List<String> clientMessages = new ArrayList<String>();
        server.enqueue(new MockResponse().withWebSocketUpgrade(
                new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket socket, Response response) {
                        socket.send("{\"event\":\"connected_success\"}");
                    }

                    @Override
                    public void onMessage(WebSocket socket, String text) {
                        clientMessages.add(text);
                        String event = new JSONObject(text).getString("event");
                        if ("task_start".equals(event)) {
                            socket.send("{\"event\":\"task_started\","
                                    + "\"session_id\":\"session-001\"}");
                        } else if ("task_cancel".equals(event)) {
                            socket.send("{\"event\":\"task_canceled\"}");
                        } else if ("task_continue".equals(event)) {
                            socket.send("{\"event\":\"task_continued\","
                                    + "\"data\":{\"audio\":\"010203\"},"
                                    + "\"is_final\":true}");
                        } else if ("task_flush".equals(event)) {
                            socket.send("{\"event\":\"task_flushed\"}");
                        } else if ("task_finish".equals(event)) {
                            socket.send("{\"event\":\"task_finished\"}");
                            socket.close(1000, "complete");
                        }
                    }
                }));
        server.start();

        RecordingListener listener = new RecordingListener();
        V2TtsClient client = new V2TtsClient(
                server.url("/v2/ai/speech/websocket").toString()
                        .replace("http://", "ws://"),
                "test-key",
                listener);
        try {
            client.connect(5, TimeUnit.SECONDS);
            client.start(StartRequest.builder("voice-a")
                    .sessionId("session-001").build(),
                    5, TimeUnit.SECONDS);
            client.cancel(5, TimeUnit.SECONDS);
            client.sendText("Hello.");
            client.flush(5, TimeUnit.SECONDS);
            CompletionResult result = client.finish(5, TimeUnit.SECONDS);

            assertArrayEquals(new byte[]{1, 2, 3}, result.getAudio());
            assertEquals(1000, result.getCloseCode());
            assertEquals("session-001", result.getSessionId());
            assertEquals(ClientState.CLOSED, client.getState());
            assertTrue(listener.errors.isEmpty());
            assertEquals(5, clientMessages.size());
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTextOver10000Characters() {
        char[] chars = new char[10001];
        java.util.Arrays.fill(chars, 'a');
        V2TtsClient.validateText(new String(chars));
    }

    @Test
    public void acceptsBinaryJsonFrames() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().withWebSocketUpgrade(
                new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket socket, Response response) {
                        socket.send(ByteString.encodeUtf8(
                                "{\"event\":\"connected_success\"}"));
                    }

                    @Override
                    public void onMessage(WebSocket socket, String text) {
                        String event = new JSONObject(text)
                                .getString("event");
                        if ("task_start".equals(event)) {
                            socket.send(ByteString.encodeUtf8(
                                    "{\"event\":\"task_started\"}"));
                        } else if ("task_finish".equals(event)) {
                            socket.send(ByteString.encodeUtf8(
                                    "{\"event\":\"task_finished\"}"));
                            socket.close(1000, "complete");
                        }
                    }
                }));
        server.start();
        V2TtsClient client = new V2TtsClient(
                server.url("/").toString().replace("http://", "ws://"),
                "test-key", new RecordingListener());
        try {
            client.connect(5, TimeUnit.SECONDS);
            client.start(StartRequest.builder("voice-a").build(),
                    5, TimeUnit.SECONDS);
            assertEquals(1000,
                    client.finish(5, TimeUnit.SECONDS).getCloseCode());
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test
    public void closeBeforeTaskFinishedFailsWithoutEventTimeout()
            throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().withWebSocketUpgrade(
                new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket socket, Response response) {
                        socket.send("{\"event\":\"connected_success\"}");
                    }

                    @Override
                    public void onMessage(WebSocket socket, String text) {
                        String event = new JSONObject(text)
                                .getString("event");
                        if ("task_start".equals(event)) {
                            socket.send("{\"event\":\"task_started\"}");
                        } else if ("task_flush".equals(event)) {
                            socket.close(1000, "premature");
                        }
                    }
                }));
        server.start();
        V2TtsClient client = new V2TtsClient(
                server.url("/").toString().replace("http://", "ws://"),
                "test-key", new RecordingListener());
        try {
            client.connect(5, TimeUnit.SECONDS);
            client.start(StartRequest.builder("voice-a").build(),
                    5, TimeUnit.SECONDS);
            try {
                client.flush(5, TimeUnit.SECONDS);
                fail("expected V2TtsException");
            } catch (V2TtsException expected) {
                assertTrue(expected.getMessage().contains(
                        "before task_finished"));
            }
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test
    public void taskFailedRemainsTheTerminalErrorAfterClose1000()
            throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().withWebSocketUpgrade(
                new WebSocketListener() {
                    @Override
                    public void onMessage(WebSocket socket, String text) {
                        socket.send("{\"event\":\"task_failed\","
                                + "\"error_code\":\"2204\","
                                + "\"error\":\"rejected continue\","
                                + "\"data\":{\"audio\":\"010203\"},"
                                + "\"base_resp\":{\"status_code\":2204,"
                                + "\"status_msg\":\"rejected\"}}");
                        socket.close(1000, "failed");
                    }
                }));
        server.start();
        V2TtsClient client = new V2TtsClient(
                server.url("/").toString().replace("http://", "ws://"),
                "test-key", new RecordingListener());
        try {
            client.connect(5, TimeUnit.SECONDS);
            try {
                client.start(StartRequest.builder("voice-a").build(),
                        5, TimeUnit.SECONDS);
                fail("expected protocol error");
            } catch (V2TtsException error) {
                assertEquals("2204", error.getErrorCode());
                assertEquals(Integer.valueOf(2204), error.getStatusCode());
            }
            Field audioField = V2TtsClient.class.getDeclaredField("audio");
            audioField.setAccessible(true);
            ByteArrayOutputStream audio =
                    (ByteArrayOutputStream) audioField.get(client);
            assertEquals(0, audio.size());
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test
    public void listenerFailureDoesNotPoisonSuccessfulCompletion()
            throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().withWebSocketUpgrade(
                new WebSocketListener() {
                    @Override
                    public void onMessage(WebSocket socket, String text) {
                        String event = new JSONObject(text).getString("event");
                        if ("task_start".equals(event)) {
                            socket.send("{\"event\":\"task_started\"}");
                        } else if ("task_finish".equals(event)) {
                            socket.send("{\"event\":\"task_finished\"}");
                            socket.close(1000, "complete");
                        }
                    }
                }));
        server.start();
        RecordingListener listener = new RecordingListener() {
            @Override
            public void onEvent(ServerEvent event) {
                if ("task_finished".equals(event.getEvent())) {
                    throw new RuntimeException("callback boom");
                }
            }
        };
        V2TtsClient client = new V2TtsClient(
                server.url("/").toString().replace("http://", "ws://"),
                "test-key", listener);
        try {
            client.connect(5, TimeUnit.SECONDS);
            client.start(StartRequest.builder("voice-a").build(),
                    5, TimeUnit.SECONDS);
            assertEquals(1000,
                    client.finish(5, TimeUnit.SECONDS).getCloseCode());
            assertEquals(1, listener.errors.size());
            assertTrue(listener.errors.get(0).getMessage()
                    .contains("listener callback failed"));
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test
    public void audioEventsAreNotRetainedInControlQueues()
            throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().withWebSocketUpgrade(
                new WebSocketListener() {
                    @Override
                    public void onMessage(WebSocket socket, String text) {
                        String event = new JSONObject(text).getString("event");
                        if ("task_start".equals(event)) {
                            socket.send("{\"event\":\"task_started\"}");
                        } else if ("task_continue".equals(event)) {
                            for (int i = 0; i < 40; i++) {
                                socket.send("{\"event\":\"task_continued\","
                                        + "\"data\":{\"audio\":\"01\"}}");
                            }
                        } else if ("task_finish".equals(event)) {
                            socket.send("{\"event\":\"task_finished\"}");
                            socket.close(1000, "complete");
                        }
                    }
                }));
        server.start();
        V2TtsClient client = new V2TtsClient(
                server.url("/").toString().replace("http://", "ws://"),
                "test-key", new RecordingListener());
        try {
            client.connect(5, TimeUnit.SECONDS);
            client.start(StartRequest.builder("voice-a").build(),
                    5, TimeUnit.SECONDS);
            client.sendText("queue check");
            assertEquals(1000,
                    client.finish(5, TimeUnit.SECONDS).getCloseCode());

            Field queuesField = V2TtsClient.class.getDeclaredField("queues");
            queuesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, BlockingQueue<ServerEvent>> queues =
                    (Map<String, BlockingQueue<ServerEvent>>)
                            queuesField.get(client);
            assertFalse(queues.containsKey("task_continued"));
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test
    public void missingCloseAfterTaskFinishedAbortsTransport()
            throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().withWebSocketUpgrade(
                new WebSocketListener() {
                    @Override
                    public void onMessage(WebSocket socket, String text) {
                        String event = new JSONObject(text).getString("event");
                        if ("task_start".equals(event)) {
                            socket.send("{\"event\":\"task_started\"}");
                        } else if ("task_finish".equals(event)) {
                            socket.send("{\"event\":\"task_finished\"}");
                        }
                    }
                }));
        server.start();
        V2TtsClient client = new V2TtsClient(
                server.url("/").toString().replace("http://", "ws://"),
                "test-key", new RecordingListener());
        try {
            client.connect(5, TimeUnit.SECONDS);
            client.start(StartRequest.builder("voice-a").build(),
                    5, TimeUnit.SECONDS);
            try {
                client.finish(100, TimeUnit.MILLISECONDS);
                fail("expected close timeout");
            } catch (V2TtsException expected) {
                assertTrue(expected.getMessage().contains("close frame"));
            }
            assertEquals(ClientState.CLOSED, client.getState());
            Field socketField = V2TtsClient.class
                    .getDeclaredField("webSocket");
            socketField.setAccessible(true);
            assertNull(socketField.get(client));
        } finally {
            client.close();
            server.shutdown();
        }
    }

    @Test
    public void handshakeTimeoutCancelsSocketAndClosesClient()
            throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE));
        server.start();
        V2TtsClient client = new V2TtsClient(
                server.url("/").toString().replace("http://", "ws://"),
                "test-key", new RecordingListener());
        try {
            try {
                client.connect(100, TimeUnit.MILLISECONDS);
                fail("expected handshake timeout");
            } catch (V2TtsException expected) {
                assertTrue(expected.getMessage().contains("handshake"));
            }
            assertEquals(ClientState.CLOSED, client.getState());
            Field socketField = V2TtsClient.class
                    .getDeclaredField("webSocket");
            socketField.setAccessible(true);
            assertNull(socketField.get(client));
        } finally {
            client.close();
            server.shutdown();
        }
    }

    private static class RecordingListener
            implements V2TtsListener {
        private final List<V2TtsException> errors =
                new ArrayList<V2TtsException>();

        @Override
        public void onEvent(ServerEvent event) {
        }

        @Override
        public void onAudio(byte[] audio, ServerEvent event) {
        }

        @Override
        public void onError(V2TtsException error) {
            errors.add(error);
        }
    }
}
