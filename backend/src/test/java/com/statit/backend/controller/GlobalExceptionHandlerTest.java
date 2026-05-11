package com.statit.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest
{
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentReturns400WithMessage()
    {
        ResponseEntity<String> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody());
    }

    @Test
    void generalExceptionReturns500WithMessage()
    {
        ResponseEntity<Map<String, String>> response =
                handler.handleGeneral(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("boom", response.getBody().get("error"));
    }

    @Test
    void generalExceptionWithNullMessageFallsBackToDefault()
    {
        ResponseEntity<Map<String, String>> response = handler.handleGeneral(new RuntimeException());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().get("error"));
    }
}
