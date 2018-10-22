import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

class HttpFileServer {

    private int port;
    private boolean printDebugMessage;
    private String directoryPath;
    private String crlf = "\r\n";
    private ArrayList<String> clientRequests = new ArrayList<>(4);

    HttpFileServer() {
        port = 8080;
        printDebugMessage = false;
        directoryPath = "./";
    }

    void printDebugMessage() { printDebugMessage = true; }

    void setPortNumber(int port) { this.port = port; }

    void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }

    void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket client = serverSocket.accept();
                new handleClientRequest(client);
            }
        } catch (IOException e) {
            System.out.println("HttpFileServer.run():  " + e.getMessage());
        }
    }


    private class handleClientRequest implements Runnable {

        private Socket client;

        handleClientRequest(Socket client) {
            this.client = client;
            new Thread(this).start();
        }

        public void run() {
            try {
                // get client request
                BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String method = "";
                boolean contentLengthFound = false;
                boolean readBody = false;
                String line;
                int contentLength = 0;
                while ((line = br.readLine()) != null) {

                    if (printDebugMessage) System.out.println(line);

                    // TODO: may not need every lines
                    clientRequests.add(line);

                    // deal with header
                    if (method.isEmpty()) {
                        if (line.length() > 2 && line.substring(0, 3).equals("GET")) {
                            method = "GET";
                        } else if (line.length() > 3 && line.substring(0, 4).equals("POST")) {
                            method = "POST";
                        }
                    } else if (!contentLengthFound &&  line.length() > 13 && line.substring(0, 14).equals("Content-Length")) {
                        contentLength = Integer.parseInt(line.substring(16));
                        contentLengthFound = true;
                    }

                    // deal with POST -d or -f
                    if (readBody) {
                        contentLength -= line.length() + 2;
                        // TODO: deal with wrong  contentLength i.e. contentLength < 0
                        if (0 != contentLength) {
                            continue;
                        } else {
                            break;
                        }
                    }

                    // deal with GET, POST without -d or -f
                    if (line.isEmpty()) {
                        if (!contentLengthFound) {
                            break;
                        } else {
                            readBody = true;
                        }
                    }
                }

                // send responds to client
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
                bw.write(generateResponds(clientRequests) + crlf);
                bw.flush();
                br.close();
                bw.close();
            } catch (Exception e) {
                System.out.println("HttpFileServer.run(), catch: " + e.getMessage());
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        client = null;
                        System.out.println("httpFileServer.run(), finally: " + e.getMessage());
                    }
                }
            }
        }
    }

    private String generateResponds(ArrayList<String> clientRequests) {
        for (String request : clientRequests) {
            // TODO: do something here
        }
        return "bullshit :D";
    }
}