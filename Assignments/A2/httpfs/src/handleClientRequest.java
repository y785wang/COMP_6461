import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

class handleClientRequest implements Runnable {

    private Socket client;
    private boolean printDebugMessage;
    private String directoryPath;

    handleClientRequest(Socket client, boolean printDebugMessage, String directoryPath) {
        this.client = client;
        this.printDebugMessage = printDebugMessage;
        this.directoryPath = directoryPath;
        new Thread(this).start();
    }

    public void run() {
        try {
            // get client request
            BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String method = "";
            boolean readBody = false;
            String line;
            StringBuilder respond = new StringBuilder();
            String contentType = "plain/text";
            int contentLength = 0;
            String filename = "";
            int statusCode = 200;
            StringBuilder respondBody = new StringBuilder();

            if (printDebugMessage) System.out.println("\n-------------------- Receiving request... --------------------\n");
            while ((line = br.readLine()) != null) {

                if (printDebugMessage) System.out.println(line);

                // deal with header
                if (method.isEmpty()) {
                    if (line.substring(0, 3).equals("GET")) {
                        method = "GET";
                    } else if (line.substring(0, 4).equals("POST")) {
                        method = "POST";
                    }
                    int pathBeginAt = line.indexOf("/");
                    String path = line.substring(pathBeginAt+1, line.indexOf(" ", pathBeginAt+1));
                    if (!path.isEmpty()) {
                        int directoryEnd = path.indexOf("/");
                        filename = -1 != directoryEnd ? path.substring(0, path.indexOf("/")) : path;
                    }
                    // TODO: if receive: src/xxx.java, parse it to src, then check if src is directory later for security
                } else if (0 == contentLength &&  line.length() > 13 && line.substring(0, 14).equals("Content-Length")) {
                    contentLength = Integer.parseInt(line.substring(16));
                }

                // deal with POST -d or -f
                if (readBody) {
                    contentLength -= line.length() + 2;
                    // TODO: deal with wrong contentLength i.e. contentLength < 0
                    if (0 != contentLength) {
                        continue;
                    } else {
                        break;
                    }
                }

                // deal with GET, POST without -d or -f
                if (line.isEmpty()) {
                    if (0 == contentLength) {
                        break;
                    } else {
                        readBody = true;
                    }
                }
            }

            // Dealing with request
            if (printDebugMessage) {
                System.out.println("\n-------------------- Dealing with request... -----------------\n");
                System.out.println("Method:   " + method);
                System.out.println("Filename: \"" + filename + "\"");
            }
            if (method.equals("GET")) {
                boolean getFileLists = filename.isEmpty();
                boolean foundFile = false;
                File curDir = new File(directoryPath);
                File[] filesList = curDir.listFiles();
                if (null != filesList) {
                    for (File file : filesList) {
                        if(file.isFile()) {
                            if (getFileLists) {
                                // TODO: deal with content-type
                                respondBody.append(file.getName()).append("\n");
                            } else if (filename.equals(file.getName())) {
                                foundFile = true;
                                break;
                            }
                        } else if (file.isDirectory() && filename.equals(file.getName())) {
                            statusCode = 403;
                            break;
                        }
                    }
                }
                if (foundFile) {
                    // TODO: deal with content-type
                    BufferedReader getFileBR = new BufferedReader(new FileReader(new File(filename)));
                    String getFileLine;
                    while (null != (getFileLine = getFileBR.readLine())) {
                        respondBody.append(getFileLine).append("\r\n");
                    }
                } else if (!getFileLists) {
                    if (200 == statusCode) statusCode = 404;
                }
            }

            // Generate responds
            if (404 == statusCode) {
                respond.append("HTTP/1.1 404 NOT FOUND\r\n");
                // TODO: based on the content type
                respondBody.append("404 Not Found\r\n");
                respondBody.append("The requested URL was not found on the server.\r\n");
                respondBody.append("If you entered the URL manually, please check you spelling and try again.\r\n");
            } else if (403 == statusCode) {
                respond.append("HTTP/1.1 403 Forbidden\r\n");
                // TODO: based on the content type
                respondBody.append("403 Forbidden\r\n");
                respondBody.append("You tried to leave the working directory, which is not allowed for security reason.\r\n");
            } else {
                respond.append("HTTP/1.1 200 OK\r\n");
            }
            respond.append("Connection: close\r\n");
            respond.append("Server: httpfs\n");
            respond.append("Date: ").append(Calendar.getInstance().getTime().toString()).append("\r\n");
            respond.append("Content-Type: ").append(contentType).append("\r\n");
            respond.append("Content-Length: ").append(respondBody.length()).append("\r\n");
            respond.append("\r\n");
            respond.append(respondBody.toString());

            // send responds to client
            if (printDebugMessage) System.out.println("\n-------------------- Finish... -------------------------------\n");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
            bw.write(respond.toString());
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
