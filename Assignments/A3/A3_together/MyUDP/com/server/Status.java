package com.server;

public enum Status {

    ACCEPTED(200,"OK"),
    CREATED(201,"CREATED"),
    BAD_REQUEST(400,"BAD REQUEST"),
    FORBIDDEN(403,"FORBIDDEN"),
    NOT_FOUND(404,"NOT FOUND");

    private final int code;
    private final String description;

    Status(int code, String description){
        this.code=code;
        this.description=description;
    }

    public int getCode(){
        return code;
    }

    public String getDescription(){
        return description;
    }

    @Override
    public String toString(){
        return code+" "+description;
    }

}
