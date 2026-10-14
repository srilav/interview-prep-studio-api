package dev.prepstudio.discussion.api;

import dev.prepstudio.api.discussion.v1.DiscussionServiceGrpc;
import dev.prepstudio.api.discussion.v1.ListMessagesRequest;
import dev.prepstudio.api.discussion.v1.ListMessagesResponse;
import dev.prepstudio.api.discussion.v1.ListThreadsRequest;
import dev.prepstudio.api.discussion.v1.ListThreadsResponse;
import dev.prepstudio.api.discussion.v1.OpenThreadRequest;
import dev.prepstudio.api.discussion.v1.Thread;
import dev.prepstudio.discussion.application.DiscussionService;
import dev.prepstudio.discussion.domain.DiscussionThread;
import dev.prepstudio.discussion.domain.Message;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class DiscussionGrpcService extends DiscussionServiceGrpc.DiscussionServiceImplBase {

    private final DiscussionService discussions;

    public DiscussionGrpcService(DiscussionService discussions) {
        this.discussions = discussions;
    }

    @Override
    public void openThread(OpenThreadRequest request, StreamObserver<Thread> out) {
        out.onNext(thread(discussions.open(request.getKeyId(), request.getTitle(), request.getOpenedBy())));
        out.onCompleted();
    }

    @Override
    public void listThreads(ListThreadsRequest request, StreamObserver<ListThreadsResponse> out) {
        var response = ListThreadsResponse.newBuilder();
        discussions.threadsFor(request.getKeyId()).forEach(t -> response.addThreads(thread(t)));
        out.onNext(response.build());
        out.onCompleted();
    }

    @Override
    public void listMessages(ListMessagesRequest request, StreamObserver<ListMessagesResponse> out) {
        var response = ListMessagesResponse.newBuilder();
        discussions.history(request.getThreadId(), request.getLimit()).forEach(m -> response.addMessages(message(m)));
        out.onNext(response.build());
        out.onCompleted();
    }

    private static Thread thread(DiscussionThread t) {
        return Thread.newBuilder()
                .setId(t.getId()).setKeyId(t.getKeyId()).setTitle(t.getTitle())
                .setOpenedBy(t.getOpenedBy()).setClosed(t.isClosed()).setOpenedAt(t.getOpenedAt().toString())
                .build();
    }

    private static dev.prepstudio.api.discussion.v1.Message message(Message m) {
        return dev.prepstudio.api.discussion.v1.Message.newBuilder()
                .setId(m.getId()).setThreadId(m.getThreadId()).setAuthor(m.getAuthor())
                .setFromAiTutor(m.isFromAiTutor()).setContent(m.getContent()).setPostedAt(m.getPostedAt().toString())
                .build();
    }
}
