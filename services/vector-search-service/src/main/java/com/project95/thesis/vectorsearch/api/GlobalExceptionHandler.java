package com.project95.thesis.vectorsearch.api;

import com.project95.thesis.vectorsearch.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Invalid vector search request: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(new ErrorResponseDto(ex.getMessage()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  public ResponseEntity<ErrorResponseDto> handleValidationException(Exception ex) {
    log.warn(
        "Vector search request validation failed: exceptionType={}", ex.getClass().getSimpleName());
    log.debug("Validation exception details", ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponseDto("Request validation failed"));
  }
}
