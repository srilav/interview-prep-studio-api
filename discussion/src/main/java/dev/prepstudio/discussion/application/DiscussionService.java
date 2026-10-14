package dev.prepstudio.discussion.application;

import dev.prepstudio.catalog.application.CatalogQueries;
import dev.prepstudio.discussion.domain.DiscussionThread;
import dev.prepstudio.discussion.domain.DiscussionThreadRepository;
import dev.prepstudio.discussion.domain.Message;
import dev.prepstudio.discussion.domain.MessageRepository;
import dev.prepstudio.shared.domain.NotFoundException;
import dev.prepstudio.shared.events.DomainEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DiscussionService {

    private final DiscussionThreadRepository threads;
    private final MessageRepository messages;
    private final CatalogQueries catalog;
    private final DomainEventPublisher events;
    private final TutorRateLimiter rateLimiter;

    public DiscussionService(DiscussionThreadRepository threads, MessageRepository messages, CatalogQueries catalog,
                             DomainEventPublisher events, TutorRateLimiter rateLimiter) {
        this.threads = threads;
        this.messages = messages;
        this.catalog = catalog;
        this.events = events;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public DiscussionThread open(String keyId, String title, String openedBy) {
        catalog.keySnapshot(keyId).orElseThrow(() -> new NotFoundException("Key", keyId));
        DiscussionThread thread = DiscussionThread.open(keyId, title, openedBy);
        threads.save(thread);
        events.publishFrom(thread);
        return thread;
    }

    @Transactional(readOnly = true)
    public List<DiscussionThread> threadsFor(String keyId) {
        return threads.findByKeyIdOrderByOpenedAtDesc(keyId);
    }

    /** Oldest first, at most {@code limit} of the most recent messages. */
    @Transactional(readOnly = true)
    public List<Message> history(String threadId, int limit) {
        thread(threadId);
        int size = limit <= 0 ? 100 : Math.min(limit, 500);
        List<Message> newestFirst = new ArrayList<>(messages.findByThreadIdOrderByPostedAtDesc(threadId, PageRequest.of(0, size)));
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    public record PostResult(Message message, boolean tutorRateLimited) {
    }

    @Transactional
    public PostResult post(String threadId, String author, String content) {
        DiscussionThread thread = thread(threadId);
        boolean wantsTutor = DiscussionThread.mentionsTutor(content);
        boolean limited = wantsTutor && !rateLimiter.tryAcquire(author == null ? "" : author.strip());
        Message message = thread.post(author, content, !limited);
        messages.save(message);
        events.publishFrom(thread);
        return new PostResult(message, limited);
    }

    @Transactional
    public Message postAiReply(String threadId, String messageId, String content) {
        DiscussionThread thread = thread(threadId);
        Message message = thread.postAiReply(messageId, content);
        messages.save(message);
        events.publishFrom(thread);
        return message;
    }

    private DiscussionThread thread(String id) {
        return threads.findById(id).orElseThrow(() -> new NotFoundException("Thread", id));
    }
}
