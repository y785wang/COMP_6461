package com.server;

public class InvalidRequest extends Exception {
    public InvalidRequest(String message){
        super(message);
    }
    public InvalidRequest(){
        super();
    }
}
