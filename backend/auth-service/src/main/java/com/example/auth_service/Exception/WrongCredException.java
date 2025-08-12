package com.example.auth_service.Exception;

public class WrongCredException extends RuntimeException{
    public WrongCredException(String message) {
        super(message);
    }
}
