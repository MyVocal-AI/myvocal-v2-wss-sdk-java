package ai.myvocal.tts.v2wss;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class V2TtsClient implements AutoCloseable {
    public static final String DEFAULT_ENDPOINT =
            "wss://openapi.myvocal.ai/v2/ai/speech/websocket";

    private final String endpoint;
    private final String apiKey;
    private final V2TtsListener listener;
    private final OkHttpClient httpClient;
    private final Map<String, BlockingQueue<ServerEvent>> queues =
            new ConcurrentHashMap<String, BlockingQueue<ServerEvent>>();
    private final ByteArrayOutputStream audio = new ByteArrayOutputStream();
    private final Object monitor = new Object();
    private final CountDownLatch openSignal = new CountDownLatch(1);

    private volatile ClientState state = ClientState.NEW;
    private volatile WebSocket webSocket;
    private volatile V2TtsException terminalError;
    private volatile int closeCode = -1;
    private volatile boolean taskFinished;
    private volatile String sessionId;

    public V2TtsClient(String endpoint, String apiKey,
                       V2TtsListener listener) {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("endpoint is required");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.listener = listener;
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public V2TtsClient(String apiKey, V2TtsListener listener) {
        this(DEFAULT_ENDPOINT, apiKey, listener);
    }

    public ClientState getState() {
        return state;
    }

    public ServerEvent connect(long timeout, TimeUnit unit)
            throws V2TtsException, InterruptedException {
        requireState(ClientState.NEW);
        Request request = new Request.Builder()
                .url(endpoint)
                .header("API-Key", apiKey)
                .build();
        webSocket = httpClient.newWebSocket(request, new SocketListener());
        if (!openSignal.await(timeout, unit)) {
            abortTransport(true);
            throw new V2TtsException("timed out waiting for WebSocket handshake");
        }
        if (terminalError != null) throw terminalError;
        state = ClientState.CONNECTED;
        return queue("connected_success").poll();
    }

    public ServerEvent start(StartRequest request, long timeout, TimeUnit unit)
            throws V2TtsException, InterruptedException {
        requireState(ClientState.CONNECTED);
        sessionId = request.getSessionId();
        send(request.toJson());
        ServerEvent event = awaitEvent("task_started", timeout, unit);
        if (event.getSessionId() != null) sessionId = event.getSessionId();
        state = ClientState.STARTED;
        return event;
    }

    public void sendText(String text) throws V2TtsException {
        requireState(ClientState.STARTED);
        validateText(text);
        send(new JSONObject()
                .put("event", "task_continue")
                .put("text", text));
    }

    public ServerEvent cancel(long timeout, TimeUnit unit)
            throws V2TtsException, InterruptedException {
        requireState(ClientState.STARTED);
        send(new JSONObject().put("event", "task_cancel"));
        return awaitEvent("task_canceled", timeout, unit);
    }

    public ServerEvent flush(long timeout, TimeUnit unit)
            throws V2TtsException, InterruptedException {
        requireState(ClientState.STARTED);
        send(new JSONObject().put("event", "task_flush"));
        return awaitEvent("task_flushed", timeout, unit);
    }

    public CompletionResult finish(long timeout, TimeUnit unit)
            throws V2TtsException, InterruptedException {
        requireState(ClientState.STARTED);
        state = ClientState.FINISHING;
        send(new JSONObject().put("event", "task_finish"));
        ServerEvent event = awaitEvent("task_finished", timeout, unit);
        taskFinished = true;
        state = ClientState.FINISHED;
        if (event.getSessionId() != null) sessionId = event.getSessionId();
        long remainingNanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + remainingNanos;
        synchronized (monitor) {
            while (closeCode < 0 && remainingNanos > 0) {
                TimeUnit.NANOSECONDS.timedWait(monitor, remainingNanos);
                remainingNanos = deadline - System.nanoTime();
            }
        }
        if (closeCode < 0) {
            abortTransport(true);
            throw new V2TtsException(
                    "task_finished received but close frame was not received");
        }
        if (closeCode != 1000) {
            throw new V2TtsException(
                    "unexpected close code after task_finished: " + closeCode);
        }
        state = ClientState.CLOSED;
        return new CompletionResult(
                audio.toByteArray(), closeCode, sessionId);
    }

    public static void validateText(String text) {
        int length = text == null
                ? 0 : text.codePointCount(0, text.length());
        if (length < 1 || length > 10000) {
            throw new IllegalArgumentException(
                    "text must contain 1 to 10000 characters");
        }
    }

    @Override
    public void close() {
        WebSocket socket = webSocket;
        if (socket != null && closeCode < 0) {
            socket.close(1000, "client close");
        }
        state = ClientState.CLOSED;
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    private void send(JSONObject message) throws V2TtsException {
        if (webSocket == null || !webSocket.send(message.toString())) {
            throw new V2TtsException("WebSocket is not connected");
        }
    }

    private ServerEvent awaitEvent(String name, long timeout, TimeUnit unit)
            throws V2TtsException, InterruptedException {
        BlockingQueue<ServerEvent> queue = queue(name);
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true) {
            if (terminalError != null) throw terminalError;
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new V2TtsException(
                        "timed out waiting for " + name);
            }
            ServerEvent event = queue.poll(
                    Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(50)),
                    TimeUnit.NANOSECONDS);
            if (event != null) return event;
        }
    }

    private BlockingQueue<ServerEvent> queue(String name) {
        BlockingQueue<ServerEvent> existing = queues.get(name);
        if (existing != null) return existing;
        BlockingQueue<ServerEvent> created =
                new ArrayBlockingQueue<ServerEvent>(16);
        BlockingQueue<ServerEvent> raced = queues.putIfAbsent(name, created);
        return raced == null ? created : raced;
    }

    private void requireState(ClientState expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "operation requires " + expected + ", current=" + state);
        }
    }

    private final class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket socket, Response response) {
            openSignal.countDown();
        }

        @Override
        public void onMessage(WebSocket socket, String text) {
            try {
                ServerEvent event = ServerEvent.fromJson(text);
                if (event.getSessionId() != null) {
                    sessionId = event.getSessionId();
                }
                byte[] chunk = event.getAudio();
                if (chunk.length > 0
                        && !"task_failed".equals(event.getEvent())) {
                    synchronized (audio) {
                        audio.write(chunk, 0, chunk.length);
                    }
                }
                if ("task_failed".equals(event.getEvent())) {
                    fail(event.asException());
                } else {
                    String queueName = "task_cancelled".equals(
                            event.getEvent())
                            ? "task_canceled" : event.getEvent();
                    if (isControlEvent(queueName)) {
                        queue(queueName).offer(event);
                    }
                }
                if ("task_finished".equals(event.getEvent())) {
                    taskFinished = true;
                }
                notifyEvent(event);
                if (chunk.length > 0) notifyAudio(chunk, event);
            } catch (RuntimeException error) {
                fail(new V2TtsException(
                        "invalid server event", error));
            }
        }

        @Override
        public void onMessage(WebSocket socket, ByteString bytes) {
            onMessage(socket, bytes.utf8());
        }

        @Override
        public void onClosing(WebSocket socket, int code, String reason) {
            closeCode = code;
            if (!taskFinished) {
                fail(new V2TtsException(
                        "WebSocket closed before task_finished: " + code));
            }
            socket.close(code, reason);
            signalClosed();
        }

        @Override
        public void onClosed(WebSocket socket, int code, String reason) {
            closeCode = code;
            state = ClientState.CLOSED;
            signalClosed();
        }

        @Override
        public void onFailure(WebSocket socket, Throwable error,
                              Response response) {
            if (taskFinished && closeCode == 1000) {
                signalClosed();
                return;
            }
            fail(new V2TtsException(
                    "WebSocket transport failed", error));
            openSignal.countDown();
            signalClosed();
        }

        private void fail(V2TtsException error) {
            V2TtsClient.this.fail(error);
        }

        private void signalClosed() {
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    private static boolean isControlEvent(String eventName) {
        return "connected_success".equals(eventName)
                || "task_started".equals(eventName)
                || "task_canceled".equals(eventName)
                || "task_flushed".equals(eventName)
                || "task_finished".equals(eventName);
    }

    private synchronized void fail(V2TtsException error) {
        if (terminalError != null) return;
        terminalError = error;
        notifyError(error);
    }

    private void notifyEvent(ServerEvent event) {
        if (listener == null) return;
        try {
            listener.onEvent(event);
        } catch (RuntimeException error) {
            notifyError(new V2TtsException(
                    "listener callback failed: onEvent", error));
        }
    }

    private void notifyAudio(byte[] chunk, ServerEvent event) {
        if (listener == null) return;
        try {
            listener.onAudio(chunk, event);
        } catch (RuntimeException error) {
            notifyError(new V2TtsException(
                    "listener callback failed: onAudio", error));
        }
    }

    private void notifyError(V2TtsException error) {
        if (listener == null) return;
        try {
            listener.onError(error);
        } catch (RuntimeException ignored) {
            // Listener failures never change the protocol result.
        }
    }

    private void abortTransport(boolean markClosed) {
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) socket.cancel();
        if (markClosed) state = ClientState.CLOSED;
        signalMonitor();
    }

    private void signalMonitor() {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }
}
