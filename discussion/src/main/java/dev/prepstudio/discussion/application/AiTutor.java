package dev.prepstudio.discussion.application;

import dev.prepstudio.ai.LlmGateway;
import dev.prepstudio.ai.LlmRequest;
import dev.prepstudio.ai.LlmTurn;
import dev.prepstudio.catalog.application.CatalogQueries;
import dev.prepstudio.catalog.application.CatalogQueries.KeySnapshot;
import dev.prepstudio.discussion.domain.Message;
import dev.prepstudio.discussion.domain.events.AiReplyRequested;
import dev.prepstudio.shared.domain.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Reacts to {@link AiReplyRequested}: streams a reply from the LLM to the thread, then posts it. */
@Component
public class AiTutor {

    private static final Logger log = LoggerFactory.getLogger(AiTutor.class);
    private static final int CONTEXT_MESSAGES = 20;

    static final String SYSTEM_PROMPT = """
            You are the AI tutor in a study group of software engineers preparing for technical interviews.
            The group is discussing the key "%s" (%s). Glossary definition: %s
            Messages from learners are prefixed with their name. Answer the latest question that mentions @tutor.
            Be accurate and concise: short paragraphs, and a small Java 21 snippet in a ```java block when code helps.
            If a learner's claim is wrong, say so politely and explain why. Stay on interview-relevant topics.
            """;

    private final DiscussionService discussions;
    private final CatalogQueries catalog;
    private final LlmGateway llm;
    private final ThreadBroadcaster broadcaster;

    public AiTutor(DiscussionService discussions, CatalogQueries catalog, LlmGateway llm, ThreadBroadcaster broadcaster) {
        this.discussions = discussions;
        this.catalog = catalog;
        this.llm = llm;
        this.broadcaster = broadcaster;
    }

    @Async
    @TransactionalEventListener
    public void on(AiReplyRequested event) {
        String replyId = Ids.newId(); // also used as the final message id, so clients can swap the streaming bubble
        try {
            KeySnapshot key = catalog.keySnapshot(event.keyId()).orElseThrow();
            List<Message> history = discussions.history(event.threadId(), CONTEXT_MESSAGES);
            var request = new LlmRequest(
                    SYSTEM_PROMPT.formatted(key.term(), key.path(), key.shortDefinition()), turns(history), 1500);

            var reply = new StringBuilder();
            try (Stream<String> parts = llm.stream(request)) {
                parts.forEach(part -> {
                    reply.append(part);
                    broadcaster.aiChunk(event.threadId(), replyId, part);
                });
            }
            discussions.postAiReply(event.threadId(), replyId, reply.toString());
        } catch (RuntimeException e) {
            log.warn("AI tutor reply failed for thread {}: {}", event.threadId(), e.getMessage());
            broadcaster.aiError(event.threadId(), replyId, "The AI tutor could not reply: " + e.getMessage());
        }
    }

    /** Learner messages become user turns (prefixed with the author), tutor messages assistant turns. */
    static List<LlmTurn> turns(List<Message> history) {
        var turns = new ArrayList<LlmTurn>();
        for (Message m : history) {
            if (m.isFromAiTutor()) {
                if (!turns.isEmpty()) turns.add(LlmTurn.assistant(m.getContent()));
            } else {
                turns.add(LlmTurn.user(m.getAuthor() + ": " + m.getContent()));
            }
        }
        if (turns.isEmpty()) turns.add(LlmTurn.user("(no messages)"));
        return turns;
    }
}
