package com.rahim.rest;

import com.rahim.ingest.coinex.CoinExException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = CandleController.class)
public class CandleErrorHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail badRequest(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ex instanceof MethodArgumentTypeMismatchException ? "Invalid query parameter type" : ex.getMessage());
    }

    @ExceptionHandler(CoinExException.class)
    public ProblemDetail upstream(CoinExException ex) {
        return ProblemDetail.forStatusAndDetail(
                ex.isTimeout() ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY, ex.getMessage());
    }
}
