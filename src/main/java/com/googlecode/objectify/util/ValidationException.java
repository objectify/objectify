package com.googlecode.objectify.util;

/**
 *
 * @author Hendrik Pilz <hepisec@gmail.com>
 */
public class ValidationException extends Exception {
    public ValidationException() {
        super();
    }
    
    public ValidationException(String msg) {
        super(msg);
    }
    
    public ValidationException(Throwable cause) {
        super(cause);
    }
    
    public ValidationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
