package com.client;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.RUDP.MyClientSocket;

public class LibHTTP {

    private static final int PORT = 80;

    private class URLTuple {
        private boolean validity;
        private String host;
        private String path;
        private String param;

        URLTuple(String user_url) {
            //validate url
            URL url;
            try {
                url = new URL(user_url);
                this.validity = true;
                this.host = url.getHost();
                this.path = url.getPath();
                this.param = url.getQuery();
            } catch (MalformedURLException e) {
                this.validity = false;
                this.host = null;
                this.path = null;
                this.param = null;
            }
        }

        boolean getValidity() {
            return this.validity;
        }

        String getHost() {
            return this.host;
        }

        String getPath() {
            return this.path;
        }

        String getParam() {
            return this.param;
        }

    }


    private StringBuilder sendAndReceive(byte[] message, URLTuple url) throws IOException {
        if (!url.getValidity()) {
            return new StringBuilder("ERROR: Invalid URL");
        }

        StringBuilder reply = new StringBuilder();
        String host = url.getHost();
        MyClientSocket clientSocket=null;
        BufferedReader reader =null;

        try {
            clientSocket=new MyClientSocket(3000,PORT);
            StringBuilder stringBuilder=new StringBuilder();
            stringBuilder.append(new String(message)).append("\r\n\r\n");

            clientSocket.send(stringBuilder.toString());

            reader=new BufferedReader(new StringReader(clientSocket.receive()));
            byte[] buffer=new byte[4096];
            String line=null;
            while((line=reader.readLine())!=null) {
                reply.append(line).append("\n");
            }

            reply.deleteCharAt(reply.length()-1);
            reply.deleteCharAt(reply.length()-1);

        } catch (UnknownHostException e) {
            System.out.println("ERROR: Host not found");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                reader.close();
        }

        return reply;
    }


    public String get(String userUrl, String userHeader) throws IOException {

        URLTuple url = new URLTuple(userUrl);

        //construct request line
        StringBuilder message = new StringBuilder("GET ");
        message.append(url.getPath());
        if (url.getParam()!=null){
            message.append("?");
            message.append(url.getParam());
        }
        message.append(" HTTP/1.0\r\n");

        message.append("Host: ").append(url.getHost());
        if (!userHeader.isEmpty()) {
            message.append("\r\n");
            message.append(userHeader);
        }

        String response = sendAndReceive(message.toString().getBytes(), url).toString();

        response=redirectionHandler(true,response,url.getHost(),userHeader,null);

        return response;

    }

    private String redirectionHandler(boolean isGet, String response, String host, String userHeader, byte[] userData) throws IOException {
        Pattern pRedirect = Pattern.compile("^HTTP[\\S\\s]*3\\d\\d\\sFOUND");
        Matcher mRedirect = pRedirect.matcher(response);
        if (mRedirect.find()) {
            Pattern pRedirectLocation = Pattern.compile("Location: (.*)(?:\\n|\\r\\n)");
            Matcher mRedirectLocation = pRedirectLocation.matcher(response);
            if (mRedirectLocation.find()) {
                System.out.println("Redirecting to " + mRedirectLocation.group(1) + "...");
                String redirectLocation = mRedirectLocation.group(1);
                if (isHTTPURL(redirectLocation)) {
                    if (isGet)
                        return get(redirectLocation, userHeader);
                    else
                        return post(redirectLocation, userHeader, userData);
                } else {
                    if (isGet)
                        return get("http://" + host + redirectLocation, userHeader);
                    else
                        return post("http://" + host + redirectLocation, userHeader, userData);
                }
            } else {
                return "Redirection found, but redirect location not set in response";
            }
        }
        return response;
    }

    private boolean isHTTPURL(String url) {
        Pattern pURL = Pattern.compile("^(http:\\/\\/|https:\\/\\/)[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(?::[0-9]{1,5})?(\\/.*)?$");
        Matcher mURL = pURL.matcher(url);
        return mURL.matches();
    }

    public String get(String userUrl) throws IOException {
        return get(userUrl, "");
    }


    private String post(String userUrl, String userHeader, byte[] userData) throws IOException {

        URLTuple url = new URLTuple(userUrl);
        StringBuilder tmp_message = new StringBuilder();

        tmp_message.append("POST ").append(url.getPath()).append(url.getParam() == null ? "" : ("?" + url.getParam())).append(" HTTP/1.0\r\n");
        tmp_message.append("Host: ").append(url.getHost()).append("\r\n");
        tmp_message.append("Content-Length: ").append(userData.length).append("\r\n"); //add on 1 to size for the ending CRLF
        if (!userHeader.isEmpty())
            tmp_message.append(userHeader).append("\r\n");
        tmp_message.append("\r\n");

        byte[] header = tmp_message.toString().getBytes();

        ByteArrayOutputStream message = new ByteArrayOutputStream();
        message.write(header);
        message.write(userData);

        String response = sendAndReceive(message.toByteArray(), url).toString();

        response=redirectionHandler(false,response,url.getHost(),userHeader,userData);

        return response;


    }

    public String postData(String userUrl, String userHeader, String userData) throws IOException {
        return post(userUrl, userHeader, userData.getBytes());
    }


    public String postFile(String userUrl, String userHeader, String filename) throws IOException {


        final String BOUNDARY = "----Boundary40043451";
        final String MULTIPART_HEADER = "Content-Type: multipart/form-data; boundary=" + BOUNDARY;

        if (!userHeader.isEmpty())
            userHeader += ("\r\n" + MULTIPART_HEADER);
        else
            userHeader = MULTIPART_HEADER;

        ByteArrayOutputStream message = new ByteArrayOutputStream();

        try {
            //open file
            Path path = Paths.get(filename);
            String mimeType = Files.probeContentType(path);

            message.write(("--" + BOUNDARY + "\r\n").getBytes());
            //FIXME name, filename
            message.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + path.getFileName() + "\"\r\n").getBytes());
            message.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes());
            message.write(Files.readAllBytes(path));
            message.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes());

            return post(userUrl, userHeader, message.toByteArray());
        } catch (NoSuchFileException e) {
            return "ERROR: File '" + filename + "' does not exist";
        } catch (IOException e) {
            return "ERROR: Errors happened when trying to open file '" + filename + "'";
        }

    }


    public static void main(String[] args) throws IOException {
        LibHTTP aLibHTTP = new LibHTTP();
//        System.out.println(aLibHTTP.get("http://httpbin.org/get?course=networking&assignment=1","User-Agent: 123"));

//        System.out.println(aLibHTTP.postData("http://httpbin.org/post", "Content-Type: application/json", "Assignment1"));

        System.out.println(aLibHTTP.postFile("http://httpbin.org/post", "User-Agent: 123\r\nAccept: */*", "google.png"));
    }


}
