package de.innologic.i18nservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.i18nservice.common.context.RequestContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unauthorized(HttpServletResponse response, String detail) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
    }

    public void forbidden(HttpServletResponse response, String detail) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Forbidden", detail);
    }

    private void write(HttpServletResponse response, HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);

        String requestId = RequestContext.requestId();
        if (requestId != null) {
            pd.setProperty("requestId", requestId);
        }
        String actor = RequestContext.actor();
        if (actor != null) {
            pd.setProperty("actor", actor);
        }

        response.setStatus(status.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(pd));
    }
}
