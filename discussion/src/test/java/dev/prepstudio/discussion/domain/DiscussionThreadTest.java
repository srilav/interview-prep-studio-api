package dev.prepstudio.discussion.domain;

import dev.prepstudio.discussion.domain.events.AiReplyRequested;
import dev.prepstudio.discussion.domain.events.MessagePosted;
import dev.prepstudio.discussion.domain.events.ThreadOpened;
import dev.prepstudio.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscussionThreadTest {

    @Test
    void mentioningTheTutorRequestsAnAiReply() {
        DiscussionThread thread = DiscussionThread.open("k-1", "Why is HashMap not thread-safe?", "ada");
        thread.post("ada", "@tutor can you explain resize races?", true);

        assertThat(thread.pullEvents())
                .hasExactlyElementsOfTypes(ThreadOpened.class, MessagePosted.class, AiReplyRequested.class);
    }

    @Test
    void noAiRequestWithoutAMentionOrWithoutQuota() {
        DiscussionThread thread = DiscussionThread.open("k-1", "Resize", "ada");
        thread.pullEvents();

        thread.post("ada", "email me at ada@tutor.dev", true);
        thread.post("ada", "@tutor again?", false);
        assertThat(thread.pullEvents()).hasExactlyElementsOfTypes(MessagePosted.class, MessagePosted.class);
    }

    @Test
    void enforcesPostingRules() {
        DiscussionThread thread = DiscussionThread.open("k-1", "Resize", "ada");
        assertThatThrownBy(() -> thread.post("ada", "x".repeat(4001), true)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> thread.post("AI Tutor", "hi", true)).hasMessageContaining("reserved");
        thread.close();
        assertThatThrownBy(() -> thread.post("ada", "hi", true)).hasMessageContaining("closed");
    }
}
