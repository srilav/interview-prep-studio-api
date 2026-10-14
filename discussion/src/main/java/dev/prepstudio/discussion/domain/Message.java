package dev.prepstudio.discussion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Immutable once posted. Created only by {@link DiscussionThread}. */
@Entity
@Table(name = "discussion_message")
public class Message {

    @Id
    private String id;
    private String threadId;
    private String author;
    private boolean fromAiTutor;
    @Column(columnDefinition = "text")
    private String content;
    private Instant postedAt;

    protected Message() {
    }

    Message(String id, String threadId, String author, boolean fromAiTutor, String content, Instant postedAt) {
        this.id = id;
        this.threadId = threadId;
        this.author = author;
        this.fromAiTutor = fromAiTutor;
        this.content = content;
        this.postedAt = postedAt;
    }

    public String getId() { return id; }
    public String getThreadId() { return threadId; }
    public String getAuthor() { return author; }
    public boolean isFromAiTutor() { return fromAiTutor; }
    public String getContent() { return content; }
    public Instant getPostedAt() { return postedAt; }
}
