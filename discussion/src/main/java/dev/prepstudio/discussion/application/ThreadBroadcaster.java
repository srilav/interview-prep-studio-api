package dev.prepstudio.discussion.application;

import dev.prepstudio.discussion.domain.events.MessagePosted;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Pushes thread activity to WebSocket subscribers of /topic/threads/{threadId}.
 * Frames: {"type":"message",...}, {"type":"ai_chunk",...}, {"type":"ai_error",...}, {"type":"error",...}.
 */
@Component
public class ThreadBroadcaster {

    private final SimpMessagingTemplate ws;

    public ThreadBroadcaster(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    public record MessageView(String id, String threadId, String author, boolean fromAiTutor, String content, String postedAt) {
    }

    @TransactionalEventListener
    public void on(MessagePosted e) {
        send(e.threadId(), Map.of("type", "message", "message", new MessageView(
                e.messageId(), e.threadId(), e.author(), e.fromAiTutor(), e.content(), e.occurredAt().toString())));
    }

    public void aiChunk(String threadId, String replyId, String delta) {
        send(threadId, Map.of("type", "ai_chunk", "threadId", threadId, "replyId", replyId, "delta", delta));
    }

    public void aiError(String threadId, String replyId, String error) {
        send(threadId, Map.of("type", "ai_error", "threadId", threadId, "replyId", replyId, "error", error));
    }

    public void error(String threadId, String error) {
        send(threadId, Map.of("type", "error", "threadId", threadId, "error", error));
    }

    private void send(String threadId, Object frame) {
        ws.convertAndSend("/topic/threads/" + threadId, frame);
    }
}
