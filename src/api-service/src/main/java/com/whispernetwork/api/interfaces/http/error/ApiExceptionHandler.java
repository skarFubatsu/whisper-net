package com.whispernetwork.api.interfaces.http.error;

import com.whispernetwork.api.application.error.BadRequestException;
import com.whispernetwork.api.application.error.ConflictException;
import com.whispernetwork.api.application.error.ForbiddenException;
import com.whispernetwork.api.application.error.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        HttpServletResponse servletResponse = ((ServletWebRequest) request).getResponse();
        String requestId = ensureRequestId(servletRequest, servletResponse);

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ProblemDetail pd = ProblemDetails.validationProblem(servletRequest.getRequestURI(), requestId, errors);

        log.warn(
                "requestId={} status=400 path={} validation_error_count={} message={}",
                requestId,
                servletRequest.getRequestURI(),
                errors.size(),
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request, HttpServletResponse response) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String detail = ex.getReason() == null ? "Request failed" : ex.getReason();
        return toProblem(status, detail, request, response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NotFoundException ex, HttpServletRequest request, HttpServletResponse response) {
        return toProblem(HttpStatus.NOT_FOUND, ex.getMessage(), request, response);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(
            ForbiddenException ex, HttpServletRequest request, HttpServletResponse response) {
        return toProblem(HttpStatus.FORBIDDEN, ex.getMessage(), request, response);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictException ex, HttpServletRequest request, HttpServletResponse response) {
        return toProblem(HttpStatus.CONFLICT, ex.getMessage(), request, response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(
            BadRequestException ex, HttpServletRequest request, HttpServletResponse response) {
        return toProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), request, response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request, HttpServletResponse response) {
        return toProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), request, response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResponseEntity<ProblemDetail> responseEntity =
                toProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, response);
        log.error(
                "requestId={} status=500 path={} message={}",
                getRequestId(request),
                request.getRequestURI(),
                ex.getMessage(),
                ex);
        return responseEntity;
    }

    private Map<String, String> toFieldError(FieldError e) {
        return Map.of(
                "field",
                e.getField(),
                "message",
                e.getDefaultMessage() == null ? "invalid value" : e.getDefaultMessage());
    }

    private String ensureRequestId(HttpServletRequest request, HttpServletResponse response) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        String requestId = attr instanceof String ? (String) attr : null;

        if (requestId == null || requestId.isBlank()) {
            String headerValue = request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
            requestId = (headerValue == null || headerValue.isBlank())
                    ? UUID.randomUUID().toString()
                    : headerValue;
            request.setAttribute(RequestIdFilter.REQUEST_ID_ATTR, requestId);
        }

        if (response != null) {
            response.setHeader(RequestIdFilter.REQUEST_ID_HEADER, requestId);
        }
        return requestId;
    }

    private ResponseEntity<ProblemDetail> toProblem(
            HttpStatus status, String detail, HttpServletRequest request, HttpServletResponse response) {
        String requestId = ensureRequestId(request, response);
        ProblemDetail pd = status == HttpStatus.INTERNAL_SERVER_ERROR
                ? ProblemDetails.internalError(request.getRequestURI(), requestId)
                : (status == HttpStatus.BAD_REQUEST
                        ? ProblemDetails.badRequest(detail, request.getRequestURI(), requestId)
                        : ProblemDetails.statusProblem(status, detail, request.getRequestURI(), requestId));

        if (status != HttpStatus.INTERNAL_SERVER_ERROR) {
            log.warn(
                    "requestId={} status={} path={} message={}",
                    requestId,
                    status.value(),
                    request.getRequestURI(),
                    detail);
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private String getRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return attr instanceof String ? (String) attr : "unknown";
    }
}
