package com.server;

import com.RUDP.MyServerSocket;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker implements Runnable {
    private final static String CRLF="\r\n";
    private final static String DUALCRLF ="\r\n\r\n";
    private final static byte CR="\r".getBytes()[0];
    private final static byte LF="\n".getBytes()[0];
    private MyServerSocket serverSocket=null;
    private boolean isOverWritten;
    private int timeout;
    private boolean browserCompatible;
    private boolean isVerbose;
    private long tId;
    private LibHttp libHTTP;
    private InputStream inputStream =null;
    private DataInputStream dataInputStream=null;
    private BufferedReader bufferedReader=null;
    private ByteArrayOutputStream headerStream=null;
    private enum METHOD{GET, POST};
    private Status status;
    private METHOD method;
    private File file;
    private String workingPath;


    @Override
    public void run() {
        try {

            tId=Thread.currentThread().getId();
            System.out.println("Thread "+tId+" is running");

            this.inputStream = new ByteArrayInputStream(serverSocket.receive().getBytes());
            this.dataInputStream=new DataInputStream(new BufferedInputStream(inputStream));
            this.headerStream=new ByteArrayOutputStream();

            splitHeader(dataInputStream,headerStream);
            this.bufferedReader=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(headerStream.toByteArray())));

            parseHeadLine();

            if (method == METHOD.GET) {
                if (file.isDirectory()) {
                    getDirectory();
                } else {
                    getFile();
                }
            } else {
                postHandler();
            }

        } catch (InvalidRequest | SocketTimeoutException ex){
            System.out.println(ex.getMessage());
            if (status==null) status= Status.BAD_REQUEST;
            errHandler(ex.getMessage());
        } catch (IOException ex){
            System.out.println("I/O error: "+ex.getMessage());
        } finally {
            try{
                if (bufferedReader!=null) bufferedReader.close();
                if (headerStream!=null) headerStream.close();
                if (dataInputStream!=null) dataInputStream.close();
                if (inputStream!=null) inputStream.close();
                if (serverSocket!=null) serverSocket.close();

                System.out.println("Thread "+tId+" terminated");
            } catch (IOException ex) {
                System.out.println("I/O error: " + ex.getMessage());
            }
        }
    }

    public Worker(LibHttp libHTTP, MyServerSocket serverSocket, String workingPath, int timeout, boolean isOverWritten, boolean browserCompatible, boolean isVerbose){
        this.libHTTP=libHTTP;
        this.serverSocket=serverSocket;
        this.workingPath=workingPath;
        this.isOverWritten = isOverWritten;
        this.timeout=timeout;
        this.browserCompatible=browserCompatible;
        this.isVerbose=isVerbose;
    }

    private void splitHeader(DataInputStream dataInputStream, ByteArrayOutputStream headerStream) throws InvalidRequest {
        byte[] b=new byte[1];
        int readLen;
        try{
            while ((readLen=dataInputStream.read(b,0,1))!=-1) {
                if (b[0]==CR){
                    dataInputStream.mark(3);
                    byte peekByte1=dataInputStream.readByte();
                    byte peekByte2=dataInputStream.readByte();
                    byte peekByte3=dataInputStream.readByte();

                    if (peekByte1==LF && peekByte2==CR && peekByte3==LF)
                        break;
                    else
                        dataInputStream.reset();
                }
                headerStream.write(b);
            }

        } catch (Exception e){
            System.out.println(e.getMessage());
            throw new InvalidRequest();
        }
    }

    private void parseHeadLine() throws IOException, InvalidRequest {

        String line;

        //parse headline
        Pattern pattern=Pattern.compile("^(GET|POST)\\s(?:https?://)*[^/\\r\\n]*([^\\r\\n]*(?=\\s))\\s[^\\r\\n]+");
        line=bufferedReader.readLine();
        Matcher matcher=pattern.matcher(line);

        if (!matcher.matches()){
            throw new InvalidRequest("Invalid Head Line");
        }

        method= METHOD.valueOf(matcher.group(1));
        file=new File(workingPath+matcher.group(2));
        if (isVerbose) System.out.println("Thread "+tId+" - File(directory) in request: "+file.getCanonicalFile());

        if (!isWithinWorkingDir(file)){
            status=Status.FORBIDDEN;
            throw new InvalidRequest("Forbidden Access");
        }

    }

    private boolean isWithinWorkingDir(File child) throws IOException {
        File workingDir=new File(workingPath);
        return child.getCanonicalPath().startsWith(workingDir.getCanonicalPath());
    }

    private void getDirectory() throws IOException {

        if (isVerbose) System.out.println("Thread "+tId+" - Client is getting directory: "+file.getAbsolutePath());

        //getFile() already handled non-existing directory

        status=Status.ACCEPTED;

        //accept types
        HashSet<String> acceptTypes=null;
        boolean hasAccept=false;

        String line;
        while ((line=bufferedReader.readLine())!=null /*&& !line.isEmpty()*/){
            if (line.startsWith("Accept:")){
                Pattern p=Pattern.compile("[:\\s,]([^,;\\s]+)");
                Matcher m=p.matcher(line);
                acceptTypes=new HashSet<>();
                while (m.find()){
                    acceptTypes.add(m.group(1));
                }
                hasAccept=true;
                if (isVerbose) System.out.println("Client accepts type: "+ acceptTypes.toString());
                break;
            }
        }

        StringBuilder body=new StringBuilder();

        if (hasAccept && acceptTypes.contains("*/*")){
                hasAccept=false;
        }

        for (File f:file.listFiles()){
            String type=Files.probeContentType(f.toPath());

            if (hasAccept && !acceptTypes.contains(type) && !f.isDirectory())
                continue;

            body.append(f.getName()).append(CRLF);
        }

        StringBuilder header=new StringBuilder();
        header.append("HTTP/1.0 ").append(status.toString()).append(CRLF);
        header.append("Content-Length: ").append(body.length()).append(CRLF);
        header.append(CRLF);

        header.append(body);
        header.append(DUALCRLF);

        serverSocket.send(header.toString());

    }

    private void getFile() throws InvalidRequest, IOException {

        if (isVerbose) System.out.println("Thread "+tId+" - Client is getting file: "+file.getAbsolutePath());

        if (!file.exists()){
            status= Status.NOT_FOUND;
            throw new InvalidRequest("File not found");
        }

        status=Status.ACCEPTED;

        //build header
        String mimeType=Files.probeContentType(file.toPath());
        if (mimeType==null) mimeType="";

        StringBuilder header=new StringBuilder();
        header.append("HTTP/1.0 ").append(status.toString()).append(CRLF);
        header.append("Content-Length: ").append(file.length()).append(CRLF);
        header.append("Content-Type: ").append(mimeType).append(CRLF);
        header.append("Content-Disposition: ").append(getContentDisposition(mimeType,file)).append(CRLF);
        header.append(CRLF);

        libHTTP.getLock(file,tId).readLock().lock();
        try {
            System.out.println("Thread "+ tId+" got lock");
            if (isVerbose) System.out.println("Thread "+tId+" - Sending file: "+file.getAbsolutePath());
            header.append(readFile());
            header.append(DUALCRLF);
            serverSocket.send(header.toString());
            if (isVerbose) System.out.println("Thread "+tId+" - File sending finished");
        } finally {
            libHTTP.removeLock(file,tId,false);
        }

    }

    private String readFile() {
        try {
            BufferedReader bufferedReader=new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder=new StringBuilder();
            String line=null;
            while ((line=bufferedReader.readLine())!=null) {
                stringBuilder.append(line).append('\n');
            }
            return stringBuilder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return "Error";
    }

    private String getContentDisposition(String mimeType, File file){
        if (mimeType.startsWith("html") || mimeType.startsWith("text") || mimeType.startsWith("image"))
            return "inline";
        else
            return "attachment; filename=\""+file.getName()+"\"";
    }

    private void errHandler(String message) {

        //build header
        StringBuilder error=new StringBuilder();
        error.append("HTTP/1.0 ").append(status.toString()).append(CRLF);
        error.append("Content-Length: ").append(status.toString().length()+message.length()+2).append(CRLF);
        error.append(CRLF);

        //error message
        error.append(status.toString()).append(CRLF);
        error.append(message);
        error.append(DUALCRLF);

        serverSocket.send(error.toString());

    }

    private void postHandler() throws InvalidRequest, IOException {

        if (isVerbose) System.out.println("Thread "+tId+" - Client is posting file: "+file.getAbsolutePath());

        String line;
        long bodyLen=-1;
        String contentType="";
        String boundary="";
        boolean isPostData=false;
        int endBoundaryLen=0;

        while ((line=bufferedReader.readLine())!=null) {
            String[] keyVal=line.split(":\\s?");

            switch (keyVal[0]){
                case "Content-Type":
                    contentType=keyVal[1];
                    break;
                case "Content-Length":
                    bodyLen=Long.parseLong(keyVal[1]);
            }
        }

        if (bodyLen==-1 /*|| contentType.isEmpty()*/){
            throw new InvalidRequest("Incorrect header of post message: no content length or content type");
        }

        try {
             boundary=contentType.split("=")[1];
        } catch (Exception ex) {
//            throw new InvalidRequest("Incorrect header of post message: no boundary");
            isPostData=true;
        }

        if (!isPostData) endBoundaryLen=boundary.length()+8;

        BufferedOutputStream postFile=null;

        try(
            ByteArrayOutputStream contentHeader=new ByteArrayOutputStream();) {
            splitHeader(dataInputStream, contentHeader);

            if (browserCompatible) {
                String fileName="";
                Pattern p=Pattern.compile("filename=\"([^\\/:*?\"<>|]+)\"");
                Matcher m=p.matcher(contentHeader.toString());
                if (m.find()){
                    fileName=m.group(1);
                }
                if (!fileName.isEmpty()) {
                    file=new File(workingPath,fileName);
                }
            }

            if (file.isDirectory()) {
                status = Status.FORBIDDEN;
                throw new InvalidRequest("Post file name cannot be a directory");
            }

            if (!isOverWritten && file.exists()){
                status = Status.FORBIDDEN;
                System.out.println(file.getCanonicalPath());
                throw new InvalidRequest("Cannot overwrite an existing file");
            }

            if (!isWithinWorkingDir(file)){
                status = Status.FORBIDDEN;
                throw new InvalidRequest("Cannot write a file outside of working path");
            }

            if (!isPostData) bodyLen=bodyLen-contentHeader.size()-endBoundaryLen-4;
            long writeLen=0;
            int readLen=0;

            libHTTP.getLock(file,tId).writeLock().lock();

            System.out.println("Thread "+ tId+" got lock");
            if (isVerbose) System.out.println("Thread "+tId+" - Writing to "+file.getAbsolutePath());

            postFile= new BufferedOutputStream(new FileOutputStream(file));

            if(!isPostData) {

                byte[] buffer = new byte[4096];
                while (writeLen < bodyLen && (readLen = dataInputStream.read(buffer, 0, 4096)) != -1) {
                    writeLen += readLen;
                    if (writeLen >= bodyLen) readLen = readLen - endBoundaryLen - 4;
                    postFile.write(buffer, 0, readLen);
                }
            } else {
                contentHeader.writeTo(postFile);
            }

            if (isVerbose) System.out.println("Thread "+tId+" - Finished writing");
        } finally {
            if (postFile!=null) postFile.close();
            libHTTP.removeLock(file,tId,true);

        }

        status=Status.CREATED;

        //build header
        StringBuilder header=new StringBuilder();
        header.append("HTTP/1.0 ").append(status.toString()).append(CRLF);
        header.append("Content-Length: ").append(status.toString().length()).append(CRLF);
        header.append(CRLF);
        header.append(status.toString());
        header.append(DUALCRLF);

        //trailing two CRLF
        serverSocket.send(header.toString());

    }

}
