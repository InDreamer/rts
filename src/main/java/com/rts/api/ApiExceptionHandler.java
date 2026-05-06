package com.rts.api;

import com.rts.model.CoreModels.AnswerType;
import com.rts.model.CoreModels.Refusal;
import com.rts.model.CoreModels.RefusalReason;
import com.rts.model.CoreModels.ServiceAnswer;
import com.rts.query.QueryRefusalException;
import com.rts.store.ProjectionValidationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(QueryRefusalException.class)
    public ResponseEntity<ServiceAnswer> queryRefusal(QueryRefusalException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(refusal(ex.reason(), ex.getMessage()));
    }

    @ExceptionHandler(ProjectionValidationException.class)
    public ResponseEntity<ServiceAnswer> projectionRefusal(ProjectionValidationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(refusal(RefusalReason.manifest_invalid, ex.getMessage()));
    }

    private ServiceAnswer refusal(RefusalReason reason, String message) {
        return new ServiceAnswer(
                AnswerType.refusal,
                null,
                null,
                List.of(),
                List.of(),
                message == null ? List.of() : List.of(message),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                new Refusal(reason, message, List.of(), false),
                List.of(),
                null);
    }
}
