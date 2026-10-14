package dev.prepstudio.discussion.domain;

import dev.prepstudio.discussion.domain.events.AiReplyRequested;
import dev.prepstudio.discussion.domain.events.MessagePosted;
import dev.prepstudio.discussion.domain.events.ThreadOpened;
import dev.prepstudio.shared.domain.AggregateRoot;
import dev.prepstudio.shared.domain.DomainException;
import dev.prepstudio.shared.domain.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * A discussion about one key. Messages are stored separately (they grow without bound) but are only
 * ever created through the thread, which enforces the posting rules.
 */
@Entity
@Table(name = "discussion_thread")
public class DiscussionThread extends AggregateRoot {

    public static final String AI_TUTOR = "AI Tutor";
    public static final int MAX_MESSAGE = 4000;
    private static final Pattern TUTOR_MENTION = Pattern.compile("(?i)(^|\\W)@tutor\\b");

    @Id
    private String id;
    private String keyId;
    private String title;
    private String openedBy;
    private boolean closed;
    private Instant openedAt;

    @Version
    @Column(name = "lock_version")
    private long lockVersion;

    protected DiscussionThread() {
    }

    public static DiscussionThread open(String keyId, String title, String openedBy) {
        if (title == null || title.isBlank()) throw new DomainException("Give the thread a title");
        if (title.strip().length() > 200) throw new DomainException("A thread title can have at most 200 characters");
        var t = new DiscussionThread();
        t.id = Ids.newId();
        t.keyId = keyId;
        t.title = title.strip();
        t.openedBy = author(openedBy);
        t.openedAt = Instant.now();
        t.registerEvent(new ThreadOpened(t.id, keyId, t.title, t.openedBy, t.openedAt));
        return t;
    }

    /**
     * Posts a learner's message. Mentioning @tutor also asks the AI tutor to reply,
     * unless the learner has used up their tutor quota.
     */
    public Message post(String author, String content, boolean tutorQuotaAvailable) {
        Message message = newMessage(Ids.newId(), author(author), false, content);
        if (mentionsTutor(content) && tutorQuotaAvailable) {
            registerEvent(new AiReplyRequested(id, keyId, message.getId(), message.getAuthor(), Instant.now()));
        }
        return message;
    }

    public Message postAiReply(String messageId, String content) {
        return newMessage(messageId, AI_TUTOR, true, content);
    }

    public void close() {
        closed = true;
    }

    public static boolean mentionsTutor(String content) {
        return content != null && TUTOR_MENTION.matcher(content).find();
    }

    private Message newMessage(String messageId, String author, boolean fromAi, String content) {
        if (closed) throw new DomainException("This thread is closed");
        if (content == null || content.isBlank()) throw new DomainException("Write a message first");
        if (content.length() > MAX_MESSAGE) {
            throw new DomainException("Messages can have at most " + MAX_MESSAGE + " characters");
        }
        var message = new Message(messageId, id, author, fromAi, content.strip(), Instant.now());
        registerEvent(new MessagePosted(id, keyId, message.getId(), author, fromAi, message.getContent(), message.getPostedAt()));
        return message;
    }

    private static String author(String name) {
        if (name == null || name.isBlank()) throw new DomainException("Enter a display name");
        String trimmed = name.strip();
        if (trimmed.equalsIgnoreCase(AI_TUTOR)) throw new DomainException("That name is reserved");
        return trimmed.length() > 60 ? trimmed.substring(0, 60) : trimmed;
    }

    public String getId() { return id; }
    public String getKeyId() { return keyId; }
    public String getTitle() { return title; }
    public String getOpenedBy() { return openedBy; }
    public boolean isClosed() { return closed; }
    public Instant getOpenedAt() { return openedAt; }
}
