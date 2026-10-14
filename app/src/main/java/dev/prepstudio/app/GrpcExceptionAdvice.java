package dev.prepstudio.app;

import dev.prepstudio.ai.LlmUnavailableException;
import dev.prepstudio.shared.domain.ConflictException;
import dev.prepstudio.shared.domain.DomainException;
import dev.prepstudio.shared.domain.NotFoundException;
import io.grpc.Status;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Maps domain exceptions to gRPC status codes. The message is safe to show to users. */
@GrpcAdvice
public class GrpcExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionAdvice.class);

    @GrpcExceptionHandler(NotFoundException.class)
    public Status notFound(NotFoundException e) {
        return Status.NOT_FOUND.withDescription(e.getMessage());
    }

    @GrpcExceptionHandler(ConflictException.class)
    public Status conflict(ConflictException e) {
        return Status.ALREADY_EXISTS.withDescription(e.getMessage());
    }

    @GrpcExceptionHandler(DomainException.class)
    public Status invalid(DomainException e) {
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
    }

    @GrpcExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public Status staleWrite(ObjectOptimisticLockingFailureException e) {
        return Status.ABORTED.withDescription("Someone else changed this at the same time. Reload and try again.");
    }

    @GrpcExceptionHandler(LlmUnavailableException.class)
    public Status llmDown(LlmUnavailableException e) {
        log.warn("LLM unavailable: {}", e.getMessage());
        return Status.UNAVAILABLE.withDescription("The AI model is unavailable right now. Try again shortly.");
    }
}
