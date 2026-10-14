package dev.prepstudio.progress.api;

import dev.prepstudio.api.progress.v1.GetSummaryRequest;
import dev.prepstudio.api.progress.v1.ProgressServiceGrpc;
import dev.prepstudio.api.progress.v1.ProgressSummary;
import dev.prepstudio.api.progress.v1.RecentActivity;
import dev.prepstudio.progress.domain.ActivityType;
import dev.prepstudio.progress.domain.StudyActivityRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ProgressGrpcService extends ProgressServiceGrpc.ProgressServiceImplBase {

    private final StudyActivityRepository activities;

    public ProgressGrpcService(StudyActivityRepository activities) {
        this.activities = activities;
    }

    @Override
    public void getSummary(GetSummaryRequest request, StreamObserver<ProgressSummary> out) {
        String learner = request.getLearner().strip();
        var summary = ProgressSummary.newBuilder()
                .setLearner(learner)
                .setExplanationsViewed((int) activities.countByLearnerIgnoreCaseAndType(learner, ActivityType.EXPLANATION_VIEWED))
                .setMessagesPosted((int) activities.countByLearnerIgnoreCaseAndType(learner, ActivityType.MESSAGE_POSTED));
        activities.findTop10ByLearnerIgnoreCaseOrderByOccurredAtDesc(learner).forEach(a -> summary.addRecent(
                RecentActivity.newBuilder()
                        .setKeyId(a.getKeyId())
                        .setType(a.getType().name())
                        .setOccurredAt(a.getOccurredAt().toString())));
        out.onNext(summary.build());
        out.onCompleted();
    }
}
