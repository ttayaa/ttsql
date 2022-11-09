package com.ttsql.Exception;

public class NotFindPrimaryKeyException extends RuntimeException{
    private static final long serialVersionUID = 1L;
    public NotFindPrimaryKeyException(String msg) {
        super(msg);
    }

}
