package com.client;

public class Request {
    protected boolean isValid=true;
    protected boolean isPost=false;
    protected String url="###N/A###";
    protected String userHeader="";
    protected boolean verbose=false;
    protected String userData="";
    protected String inputFileName="";
    protected boolean isOutput=false;
    protected String outputFileName="";
    protected String errMsg="";
    protected Mode postMode=Mode.UNSET;
    protected enum Mode {DATA, FILE, UNSET};
}
