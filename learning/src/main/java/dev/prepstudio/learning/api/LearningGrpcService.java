package dev.prepstudio.learning.api;

import dev.prepstudio.api.learning.v1.Explanation;
import dev.prepstudio.api.learning.v1.ExplanationChunk;
import dev.prepstudio.api.learning.v1.LearningServiceGrpc;
import dev.prepstudio.api.learning.v1.StreamExplanationRequest;
import dev.prepstudio.catalog.api.CatalogProtoMapper;
import dev.prepstudio.learning.application.ExplanationService;
import dev.prepstudio.learning.application.ExplanationService.GenerationFailedException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.concurrent.CancellationException;

@GrpcService
public class LearningGrpcService extends LearningServiceGrpc.LearningServiceImplBase {

    private final ExplanationService explanations;

    public LearningGrpcService(ExplanationService explanations) {
        this.explanations = explanations;
    }

    @Override
    public void streamExplanation(StreamExplanationRequest request, StreamObserver<ExplanationChunk> out) {
        var call = (ServerCallStreamObserver<ExplanationChunk>) out;
        try {
            explanations.stream(request.getKeyId(), CatalogProtoMapper.difficulty(request.getLevel()),
                    request.getRegenerate(), blankToNull(request.getRequestedBy()), new ExplanationService.Sink() {
                        @Override
                        public void delta(String markdown) {
                            if (call.isCancelled()) throw new CancellationException("Client cancelled");
                            out.onNext(ExplanationChunk.newBuilder().setMarkdownDelta(markdown).build());
                        }

                        @Override
                        public void completed(dev.prepstudio.learning.domain.Explanation e) {
                            out.onNext(ExplanationChunk.newBuilder().setCompleted(Explanation.newBuilder()
                                    .setId(e.getId())
                                    .setKeyId(e.getKeyId())
                                    .setLevel(CatalogProtoMapper.difficulty(e.getLevel()))
                                    .setMarkdown(e.getMarkdown())
                                    .setVersion(e.getRevision())).build());
                        }
                    });
        } catch (GenerationFailedException e) {
            if (!call.isCancelled()) out.onNext(ExplanationChunk.newBuilder().setError(e.getMessage()).build());
        }
        if (!call.isCancelled()) out.onCompleted();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
