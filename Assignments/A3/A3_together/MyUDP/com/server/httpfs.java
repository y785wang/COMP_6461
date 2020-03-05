package com.server;

import java.io.File;
import java.io.IOException;

import static java.lang.System.exit;

public class httpfs {

    public static void main(String[] args) {

        Parser parser=new Parser();
        parser.addOption("v",true,true);
        parser.addOption("p", true, false);
        parser.addOption("d", true,false);
        parser.addOption("r", true,true);
        parser.addOption("b", true,true);
        parser.addOption("w", true, false);
        parser.addOption("t", true, false);
        parser.setPrefix("-");

        try{
            parser.parse(args);
        } catch (IllegalArgumentException ex) {
            printErr(ex.getMessage());
        }

        boolean isVerbose=parser.getValue("v")==null ? false : true;
        boolean isOverWrite=parser.getValue("r")==null ? false : true;
        boolean isBrowserCompatible=parser.getValue("b")==null ? false : true;

        int port=8080;
        String setPort=parser.getValue("p");
        if (setPort!=null) {
            try {
                port = Integer.parseInt(parser.getValue("p"));
            } catch (NumberFormatException ex) {
                printErr(ex.getMessage() + " is not an integer");
            }
        }

        int cntThread=5;
        String setCntThread=parser.getValue("w");
        if (setCntThread!=null){
            try{
                cntThread=Integer.parseInt(setCntThread);
            } catch (NumberFormatException ex) {
                printErr(ex.getMessage()+ " is not an integer");
            }
        }

        int timeOut=5000;
        String setTimeOut=parser.getValue("t");
        if (setTimeOut!=null){
            try{
                timeOut=Integer.parseInt(setTimeOut);
            } catch (NumberFormatException ex) {
                printErr(ex.getMessage()+ " is not an integer");
            }
        }

        String pathName=System.getProperty("user.dir");
        String setPathName=parser.getValue("d");
        if(setPathName!=null) {
            try {
                File file = new File(setPathName);
                if (!file.isDirectory()) {
                    printErr("Path \"" + file.getCanonicalPath() + "\" is not a directory");
                }
            } catch (IOException e) {
                printErr(e.getMessage());
            }
            pathName=setPathName;
        }

        LibHttp libHttp=new LibHttp(port,pathName,cntThread,isOverWrite,isVerbose,timeOut,isBrowserCompatible);
        libHttp.run();

    }


    private static void printErr(String error){
        System.out.println(error);
        System.out.println();
        printHelpInfo();
        exit(-1);
    }

    private static void printHelpInfo(){
        System.out.println("server is a simple file server. \n" +
                "usage: server [-v] [-p PORT] [-d PATH-TO-DIR] \n" +
                " \n" +
                "   -v   Prints debugging messages. \n" +
                "   -r   Overwrite existing file. \n" +
                "   -b   Support browser. \n" +
                "   -w   Number of worker thread, default: 5. \n" +
                "   -p   Specifies the port number that the server will listen and serve at. \n" +
                "        Default is 8080. \n" +
                "   -d   Specifies the directory that the server will use to read/write \n" +
                "requested files. Default is the current directory when launching the \n" +
                "application.");
    }



}
