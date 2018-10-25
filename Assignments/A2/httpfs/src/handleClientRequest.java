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

            boolean readBody = false;
            boolean listAllFiles = false;
            int contentLength = 0;
            int statusCode = 200;
            String method = "";
            String line;
            String filePath = "";
            String contentType = "plain/text";
            StringBuilder respond = new StringBuilder();
            StringBuilder respondBody = new StringBuilder();
            StringBuilder postRequestBody = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // get client request
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
                    filePath = line.substring(pathBeginAt, line.indexOf(" ", pathBeginAt+1));
                    if (filePath.equals("/")) listAllFiles = true;
                } else if (0 == contentLength &&  line.length() > 13 && line.substring(0, 14).equals("Content-Length")) {
                    contentLength = Integer.parseInt(line.substring(16));
                }

                // deal with POST -d or -f
                if (readBody) {
                    postRequestBody.append(line).append("\r\n");
                    contentLength -= line.length() + 2;
                    if (contentLength > 0) {
                        continue;
                    } else if (contentLength < 0) {
                        System.out.println("ERROR: contentLength = " + contentLength);
                        return;
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
                System.out.println("Method:      " + method);
            }
            if (filePath.length() > 3 && filePath.substring(0, 4).equals("/../")) {
                statusCode = 403;
            } else if (method.equals("GET")) {
                if (listAllFiles) {
                    printAllFiles(directoryPath, respondBody);
                } else {
                    // TODO: when pass is ./ i.e. localhost/././file_1
                    File getFile = new File(directoryPath + filePath);
                    if (getFile.exists() && getFile.isFile()) {
                        BufferedReader getFileContents = new BufferedReader(new FileReader(getFile));
                        String getLine;
                        while (null != (getLine = getFileContents.readLine())) {
                            respondBody.append(getLine).append("\r\n");
                        }
                    } else {
                        statusCode = 404;
                    }
                }
            } else if (method.equals("POST")) {
                String contents = postRequestBody.toString();
                BufferedWriter postFileWriter = new BufferedWriter(new FileWriter(new File(directoryPath + filePath)));
                // TODO: remove headers,
                postFileWriter.write(contents);
                postFileWriter.close();
            }

            // Generate responds
            if (printDebugMessage) System.out.println("Status Code: " + statusCode);
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

    private void printAllFiles(String path, StringBuilder respondBody) {
        File curDir = new File(path);
        File[] filesList = curDir.listFiles();
        if (null != filesList) {
            for (File file : filesList) {
                if(file.isFile()) {
                    respondBody.append(".").append(file.getPath().substring(directoryPath.length())).append("\r\n");
                } else if (file.isDirectory()) {
                    printAllFiles(path + "/" + file.getName(), respondBody);
                }
            }
        }
    }
}
