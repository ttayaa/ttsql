package com.ttsql.Exception;

public class NotCollectionOrArrayException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotCollectionOrArrayException(String msg) {
        super(msg);
    }
}
