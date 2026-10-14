package dev.prepstudio.app;

import dev.prepstudio.api.catalog.v1.CatalogServiceGrpc.CatalogServiceBlockingStub;
import dev.prepstudio.api.catalog.v1.CreateCategoryRequest;
import dev.prepstudio.api.catalog.v1.CreatePlatformRequest;
import dev.prepstudio.api.catalog.v1.CreateSubcategoryRequest;
import dev.prepstudio.api.catalog.v1.Key;
import dev.prepstudio.api.catalog.v1.KeyStatus;
import dev.prepstudio.api.catalog.v1.ListTreeRequest;
import dev.prepstudio.api.catalog.v1.PlatformKind;
import dev.prepstudio.api.catalog.v1.SearchKeysRequest;
import dev.prepstudio.api.catalog.v1.TaxonomyAdminServiceGrpc.TaxonomyAdminServiceBlockingStub;
import dev.prepstudio.api.catalog.v1.UpsertKeyRequest;
import dev.prepstudio.api.discussion.v1.DiscussionServiceGrpc.DiscussionServiceBlockingStub;
import dev.prepstudio.api.discussion.v1.ListMessagesRequest;
import dev.prepstudio.api.discussion.v1.OpenThreadRequest;
import dev.prepstudio.api.learning.v1.ExplanationChunk;
import dev.prepstudio.api.learning.v1.LearningServiceGrpc.LearningServiceBlockingStub;
import dev.prepstudio.api.learning.v1.StreamExplanationRequest;
import dev.prepstudio.api.progress.v1.GetSummaryRequest;
import dev.prepstudio.api.progress.v1.ProgressServiceGrpc.ProgressServiceBlockingStub;
import dev.prepstudio.discussion.application.DiscussionService;
import dev.prepstudio.shared.events.OutboxEventRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Boots the whole application with in-process gRPC, H2 and the fake LLM, and drives it through the API. */
@SpringBootTest
@ActiveProfiles("test")
class PrepStudioApiTest {

    @GrpcClient("inProcess") TaxonomyAdminServiceBlockingStub admin;
    @GrpcClient("inProcess") CatalogServiceBlockingStub catalog;
    @GrpcClient("inProcess") LearningServiceBlockingStub learning;
    @GrpcClient("inProcess") DiscussionServiceBlockingStub discussion;
    @GrpcClient("inProcess") ProgressServiceBlockingStub progress;

    @Autowired DiscussionService discussionService;
    @Autowired OutboxEventRepository outbox;

    @Test
    void sampleTaxonomyIsLoaded() {
        var tree = admin.listTree(ListTreeRequest.getDefaultInstance());
        assertThat(tree.getPlatformsList()).extracting(p -> p.getName()).contains("Java", "Spring Boot", "Apache Kafka");
        var map = tree.getPlatformsList().stream().filter(p -> p.getName().equals("Java")).findFirst().orElseThrow()
                .getCategories(0).getSubcategories(0);
        assertThat(map.getName()).isEqualTo("Map");
        assertThat(map.getKeyCount()).isEqualTo(3);
    }

    @Test
    void editorBuildsTaxonomyAndPublishesAKeyThatLearnersCanFind() {
        var python = admin.createPlatform(CreatePlatformRequest.newBuilder().setName("Python").setKind(PlatformKind.LANGUAGE).build());
        var category = admin.createCategory(CreateCategoryRequest.newBuilder().setPlatformId(python.getId()).setName("Data model").build());
        var sub = admin.createSubcategory(CreateSubcategoryRequest.newBuilder()
                .setPlatformId(python.getId()).setCategoryId(category.getId()).setName("Dunder methods").build());

        Key saved = admin.upsertKey(UpsertKeyRequest.newBuilder().setKey(Key.newBuilder()
                .setSubcategoryId(sub.getId()).setTerm("__eq__").addAliases("dunder eq")
                .setShortDefinition("Defines equality for instances; pair it with __hash__.")
                .setStatus(KeyStatus.PUBLISHED)).build());
        assertThat(saved.getPath().getSubcategoryName()).isEqualTo("Dunder methods");

        var found = catalog.searchKeys(SearchKeysRequest.newBuilder().setQuery("DUNDER").setPlatformId(python.getId()).build());
        assertThat(found.getKeysList()).extracting(Key::getTerm).containsExactly("__eq__");

        assertThat(outbox.findAll()).extracting(e -> e.getEventType())
                .contains("PlatformAdded", "CategoryAdded", "SubcategoryAdded", "KeyCreated", "KeyPublished");
    }

    @Test
    void businessRuleViolationsBecomeGrpcStatusCodes() {
        assertThatThrownBy(() -> admin.createPlatform(CreatePlatformRequest.newBuilder().setName("java").setKind(PlatformKind.LANGUAGE).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS));

        assertThatThrownBy(() -> admin.upsertKey(UpsertKeyRequest.newBuilder().setKey(Key.newBuilder()
                .setSubcategoryId("s-java-map").setTerm("LinkedHashMap").setShortDefinition("x".repeat(300))).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));

        assertThatThrownBy(() -> catalog.getKey(dev.prepstudio.api.catalog.v1.GetKeyRequest.newBuilder().setKeyId("nope").build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void explanationIsStreamedOnceThenServedFromStoreAndCountsTowardProgress() {
        var request = StreamExplanationRequest.newBuilder().setKeyId("k-volatile").setRequestedBy("ada").build();

        List<ExplanationChunk> first = new ArrayList<>();
        learning.streamExplanation(request).forEachRemaining(first::add);
        assertThat(first.size()).isGreaterThan(2);
        assertThat(first.get(0).hasMarkdownDelta()).isTrue();
        var completed = first.get(first.size() - 1).getCompleted();
        assertThat(completed.getMarkdown()).contains("```java");
        assertThat(completed.getVersion()).isEqualTo(1);

        List<ExplanationChunk> second = new ArrayList<>();
        learning.streamExplanation(request).forEachRemaining(second::add);
        assertThat(second).hasSize(1);
        assertThat(second.get(0).getCompleted().getId()).isEqualTo(completed.getId());

        var summary = progress.getSummary(GetSummaryRequest.newBuilder().setLearner("ADA").build());
        assertThat(summary.getExplanationsViewed()).isEqualTo(2);
    }

    @Test
    void tutorRepliesInTheThreadWhenMentioned() throws InterruptedException {
        var thread = discussion.openThread(OpenThreadRequest.newBuilder()
                .setKeyId("k-hashmap").setTitle("Resize races").setOpenedBy("grace").build());
        discussionService.post(thread.getId(), "grace", "@tutor why can HashMap lose entries under concurrency?");

        var listRequest = ListMessagesRequest.newBuilder().setThreadId(thread.getId()).build();
        for (int i = 0; i < 50 && discussion.listMessages(listRequest).getMessagesCount() < 2; i++) Thread.sleep(100);

        var messages = discussion.listMessages(listRequest).getMessagesList();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getAuthor()).isEqualTo("grace");
        assertThat(messages.get(1).getFromAiTutor()).isTrue();
        assertThat(messages.get(1).getContent()).contains("Offline answer");
    }
}
