package com.javacafe.realtime_sujeong.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// @Validated (단일 파라미터 유효성)에서 발생하는 예외 처리
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex) {
		Map<String, String> errors = new HashMap<>();

		ex.getConstraintViolations().forEach(violation ->
			errors.put(
				violation.getPropertyPath().toString(),
				violation.getMessage()
			)
		);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(errors);
	}

	// 그 외 예상치 못한 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleException(Exception ex) {
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("error", ex.getMessage()));
	}
}
