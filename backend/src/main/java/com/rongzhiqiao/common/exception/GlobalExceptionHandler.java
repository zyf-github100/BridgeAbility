package com.rongzhiqiao.common.exception;

import com.rongzhiqiao.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.failure(exception.getCode(), ErrorMessageLocalizer.localize(exception.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception exception) {
        String message = "请求参数不正确";
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            message = methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                    .map(error -> ErrorMessageLocalizer.localizeFieldError(error.getField(), error.getDefaultMessage()))
                    .collect(Collectors.joining("; "));
        } else if (exception instanceof BindException bindException) {
            message = bindException.getBindingResult().getFieldErrors().stream()
                    .map(error -> ErrorMessageLocalizer.localizeFieldError(error.getField(), error.getDefaultMessage()))
                    .collect(Collectors.joining("; "));
        } else if (exception instanceof ConstraintViolationException constraintViolationException) {
            message = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> ErrorMessageLocalizer.localizeFieldError(
                            violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString(),
                            violation.getMessage()
                    ))
                    .collect(Collectors.joining("; "));
        } else if (exception instanceof HttpMessageNotReadableException) {
            message = "请求体格式不正确，请检查提交内容";
        }
        return ApiResponse.failure(4001, message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        String message = ErrorMessageLocalizer.localize(exception.getMessage());
        return ApiResponse.failure(5000, message);
    }
}
