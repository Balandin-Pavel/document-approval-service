package ru.balandin.docflow.api;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.balandin.docflow.document.WorkflowConflictException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(WorkflowConflictException.class)
    ProblemDetail conflict(WorkflowConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate() {
        return problem(HttpStatus.CONFLICT, "Document was updated concurrently");
    }

    @ExceptionHandler(SecurityException.class)
    ProblemDetail forbidden(SecurityException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setTitle(status.getReasonPhrase());
        return result;
    }
}
