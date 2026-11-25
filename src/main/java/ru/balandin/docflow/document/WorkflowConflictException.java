package ru.balandin.docflow.document;

public class WorkflowConflictException extends RuntimeException {
    public WorkflowConflictException(String message) {
        super(message);
    }
}
