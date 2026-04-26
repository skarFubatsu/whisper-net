package com.whispernetwork.api.interfaces.http.error;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

final class ProblemDetails {
    private ProblemDetails() {}

    static ProblemDetail validationProblem(String instance, String requestId, List<Map<String, String>> errors) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("urn:whispernet:error:validation"));
        pd.setTitle("Bad Request");
        pd.setDetail("Validation failed");
        pd.setInstance(URI.create(instance));
        pd.setProperty("requestId", requestId);
        pd.setProperty("errors", errors);
        return pd;
    }

    static ProblemDetail statusProblem(HttpStatus status, String detail, String instance, String requestId) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create("urn:whispernet:error:" + status.value()));
        pd.setTitle(status.getReasonPhrase());
        pd.setDetail(detail);
        pd.setInstance(URI.create(instance));
        pd.setProperty("requestId", requestId);
        return pd;
    }

    static ProblemDetail badRequest(String detail, String instance, String requestId) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("urn:whispernet:error:bad-request"));
        pd.setTitle("Bad Request");
        pd.setDetail(detail);
        pd.setInstance(URI.create(instance));
        pd.setProperty("requestId", requestId);
        return pd;
    }

    static ProblemDetail internalError(String instance, String requestId) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create("urn:whispernet:error:internal"));
        pd.setTitle("Internal Server Error");
        pd.setDetail("Unexpected error");
        pd.setInstance(URI.create(instance));
        pd.setProperty("requestId", requestId);
        return pd;
    }
}
