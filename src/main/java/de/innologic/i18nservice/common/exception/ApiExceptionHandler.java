package de.innologic.i18nservice.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Conflict");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    // DTO @Valid Fehler (z.B. Pattern, NotBlank)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Error");

        String msg = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Validation failed");

        pd.setDetail(msg);
        return pd;
    }

    // Falls @Version über JPA zuschlägt (zusätzlich zu deinem expectedVersion Check)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail optimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Version Conflict");
        pd.setDetail("Optimistic locking conflict (concurrent update).");
        return pd;
    }

    // Unique-Constraints etc. (z.B. language already exists)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail dataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Data Integrity Violation");
        pd.setDetail("Database constraint violated (duplicate or invalid reference).");
        return pd;
    }
}

