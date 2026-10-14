package dev.prepstudio.discussion.api;

import dev.prepstudio.discussion.application.DiscussionService;
import dev.prepstudio.discussion.application.ThreadBroadcaster;
import dev.prepstudio.shared.domain.DomainException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/** Receives posts sent to /app/threads/{threadId}/messages. Results are broadcast by {@link ThreadBroadcaster}. */
@Controller
public class DiscussionStompController {

    public record PostMessage(String author, String content) {
    }

    private final DiscussionService discussions;
    private final ThreadBroadcaster broadcaster;

    public DiscussionStompController(DiscussionService discussions, ThreadBroadcaster broadcaster) {
        this.discussions = discussions;
        this.broadcaster = broadcaster;
    }

    @MessageMapping("/threads/{threadId}/messages")
    public void post(@DestinationVariable String threadId, @Payload PostMessage body) {
        try {
            var result = discussions.post(threadId, body.author(), body.content());
            if (result.tutorRateLimited()) {
                broadcaster.aiError(threadId, null,
                        result.message().getAuthor() + " has reached the AI tutor limit. Try again in a minute.");
            }
        } catch (DomainException e) {
            broadcaster.error(threadId, e.getMessage());
        }
    }
}
