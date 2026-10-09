package com.wonder.electricals.inventory.exception;

import com.wonder.electricals.inventory.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Single place where exceptions become either a JSON body (REST under {@code /api})
 * or a redirect carrying a flash message (Thymeleaf pages). Controllers stay free
 * of try/catch blocks and no stack trace ever reaches the user.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Message key used by {@code fragments/alerts.html} to render the flash banner. */
    public static final String FLASH_ERROR = "errorMessage";

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        log.warn("Resource not found: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
        }
        return redirectWithError(request, redirectAttributes, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Object handleDuplicate(DuplicateResourceException ex, HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        log.warn("Duplicate record rejected: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
        }
        return redirectWithError(request, redirectAttributes, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public Object handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        log.warn("Stock OUT rejected: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
        }
        return redirectWithError(request, redirectAttributes, ex.getMessage());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public Object handleOperationNotAllowed(OperationNotAllowedException ex, HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {
        log.warn("Operation refused: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
        }
        return redirectWithError(request, redirectAttributes, ex.getMessage());
    }

    /** Bean Validation failures on {@code @Valid @RequestBody} payloads. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String[] fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .toArray(String[]::new);
        String message = fieldErrors.length == 0 ? "Validation failed" : fieldErrors[0];

        if (isApiRequest(request)) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.of(400, "Bad Request", message, request.getRequestURI(), fieldErrors));
        }
        return redirectWithError(request, redirectAttributes, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String[] messages = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .toArray(String[]::new);
        String message = messages.length == 0 ? "Validation failed" : messages[0];

        if (isApiRequest(request)) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.of(400, "Bad Request", message, request.getRequestURI(), messages));
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Malformed input that never reaches a service: a non-numeric id in a path variable,
     * an unparsable query parameter, or an unreadable JSON body. These are client mistakes,
     * so they must answer 400 instead of being reported as an internal error.
     */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public Object handleMalformedRequest(Exception ex, HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        String message = describeMalformedRequest(ex);
        log.info("Rejected malformed request {} {}: {}", request.getMethod(), request.getRequestURI(), message);

        if (isApiRequest(request)) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.of(400, "Bad Request", message, request.getRequestURI()));
        }
        return redirectWithError(request, redirectAttributes, message);
    }

    /** Bad credentials from the REST login endpoint; the page form uses a redirect instead. */
    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.info("Authentication failed for {}: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.of(401, "Unauthorized", "Invalid email or password",
                            request.getRequestURI()));
        }
        return "redirect:/login?error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for {} {}", request.getMethod(), request.getRequestURI());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiErrorResponse.of(403, "Forbidden",
                            "You do not have permission to perform this action", request.getRequestURI()));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * A URL that exists but not for this HTTP method. Answers 405 with the methods
     * that are allowed, instead of the catch-all turning it into a 500.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                            HttpServletRequest request) {
        log.info("Method {} not allowed on {}", request.getMethod(), request.getRequestURI());

        if (isApiRequest(request)) {
            Set<HttpMethod> supported = ex.getSupportedHttpMethods();
            String allowed = supported == null ? ""
                    : supported.stream().map(HttpMethod::name).sorted()
                            .collect(Collectors.joining(", "));
            ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
            if (supported != null) {
                response.allow(supported.toArray(new HttpMethod[0]));
            }
            return response.body(ApiErrorResponse.of(405, "Method Not Allowed",
                    "Method " + request.getMethod() + " is not supported for "
                            + request.getRequestURI()
                            + (allowed.isEmpty() ? "" : ". Allowed: " + allowed),
                    request.getRequestURI()));
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    /** A body sent with a content type the endpoint cannot read. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                               HttpServletRequest request) {
        log.info("Unsupported media type on {} {}", request.getMethod(), request.getRequestURI());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(ApiErrorResponse.of(415, "Unsupported Media Type",
                            "Content type " + ex.getContentType() + " is not supported",
                            request.getRequestURI()));
        }
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

    /**
     * A URL that matches no controller. Spring hands these to the static resource
     * handler, which reports "no static resource"; to the caller it is simply a 404.
     */
@ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResource(NoResourceFoundException ex, HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        log.info("No handler for {} {}", request.getMethod(), request.getRequestURI());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.of(404, "Not Found", "No endpoint " + request.getRequestURI(),
                            request.getRequestURI()));
        }
        // Hand the request to the container error page, telling it this is a 404 so the
        // styled templates/error.html is rendered with the correct status.
        request.setAttribute("jakarta.servlet.error.status_code", HttpStatus.NOT_FOUND.value());
        request.setAttribute("jakarta.servlet.error.request_uri", request.getRequestURI());
        return "forward:/error";
    }

    /**
     * Last-resort handler. The user gets a generic message; the full stack trace is
     * written to the server log only.
     */
    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        String message = "Something went wrong while processing your request. Please try again.";
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.of(500, "Internal Server Error", message, request.getRequestURI()));
        }
        return redirectWithError(request, redirectAttributes, message);
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    /** Turns the low-level binding failures into one message a client can act on. */
    private static String describeMalformedRequest(Exception ex) {
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            String required = mismatch.getRequiredType() != null
                    ? mismatch.getRequiredType().getSimpleName() : "value";
            return "'" + mismatch.getValue() + "' is not a valid " + required
                    + " for parameter '" + mismatch.getName() + "'";
        }
        if (ex instanceof MissingServletRequestParameterException missing) {
            return "Required parameter '" + missing.getParameterName() + "' is missing";
        }
        return "The request body could not be read";
    }

    private static boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/");
    }

    /**
     * Sends the browser back to the page that submitted the form. When no referrer
     * is available the user lands on the dashboard instead of a blank page.
     */
    private static String redirectWithError(HttpServletRequest request,
                                            RedirectAttributes redirectAttributes, String message) {
        String referer = request.getHeader("Referer");
        String target = referer != null && !referer.isBlank() ? referer : request.getContextPath() + "/dashboard";
        redirectAttributes.addFlashAttribute(FLASH_ERROR, message);
        return "redirect:" + target;
    }
}
