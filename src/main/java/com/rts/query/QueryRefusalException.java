package com.rts.query;

import com.rts.model.CoreModels.RefusalReason;

public class QueryRefusalException extends RuntimeException {
    private final RefusalReason reason;

    public QueryRefusalException(RefusalReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public RefusalReason reason() {
        return reason;
    }
}
