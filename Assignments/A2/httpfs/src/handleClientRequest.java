import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;

class handleClientRequest implements Runnable {

    private Socket client;
    private boolean printDebugMessage;
    private String directoryPath;
    private String crlf = "\r\n";

    // get client request
    private boolean readBody = false;
    private int contentLength = 0;
    private BufferedReader br;

    // deal with request
    private String filePath = "";
    private boolean listAllFiles = false;
    private String method = "";

    // generate respond
//    private String contentType = "plain/text";
    private StringBuilder respond = new StringBuilder();
    private StringBuilder respondBody = new StringBuilder();
    private int statusCode = 200;
    private ArrayList<String> postRequestBody = new ArrayList<>();

    handleClientRequest(Socket client, boolean printDebugMessage, String directoryPath) {
        this.client = client;
        this.printDebugMessage = printDebugMessage;
        this.directoryPath = directoryPath;
        new Thread(this).start();
    }

    public void run() {
        try {

            // get client request
            if (printDebugMessage) System.out.println("\n-------------------- Receiving request... --------------------\n");
            getClientRequest();

            // Dealing with request
            if (printDebugMessage) {
                System.out.println("\n-------------------- Dealing with request... -----------------\n");
                System.out.println("Method:           " + method);
            }
            dealWithRequest();

            // Generate responds
            if (printDebugMessage) System.out.println("Status Code:      " + statusCode);
            generateRespond();

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

    private void getClientRequest() throws Exception {
        String line;
        br = new BufferedReader(new InputStreamReader(client.getInputStream()));
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
                postRequestBody.add(line + crlf);
                contentLength -= line.length() + 2;
                if (contentLength > 0) {
                    continue;
                } else if (contentLength < 0) {
                    // TODO: set status code 400
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
    }

    private void dealWithRequest() throws Exception {
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
                        respondBody.append(getLine).append(crlf);
                    }
                } else {
                    statusCode = 404;
                }
            }
        } else if (method.equals("POST")) {
            File postFile = new File(directoryPath + filePath);
            String postFileName = "";
            String postFilePath = directoryPath + filePath;
            boolean currentDirectory = false;
            boolean fileExist = false;

            if (postFile.isDirectory()) {
                statusCode = 400;
                respondBody.append("There is an existing directory with the same name : ");
                respondBody.append(directoryPath).append(filePath).append(crlf);
            }
            if (postFile.exists()) fileExist = true;
            if (-1 == postFilePath.indexOf("/", 2)) currentDirectory = true;
            if (!currentDirectory) {
                int indexOfSlash = 2; //postFilePath.indexOf("/", 2); // 2 for skipping "./"
                while(true) {
                    indexOfSlash = postFilePath.indexOf("/", indexOfSlash);
                    if (-1 == indexOfSlash) {
                        break;
                    } else {
                        postFileName = postFilePath.substring(indexOfSlash); // begins with "/", i.e. "/dir/file"
                        ++indexOfSlash;
                    }
                }
            }
            if (!postFileName.isEmpty()) {
                String newDirectoryPath = postFilePath.substring(0, postFilePath.length()-postFileName.length());
                File newDirectory = new File(newDirectoryPath);
                if (newDirectory.mkdirs()) {
                    if (printDebugMessage) {
                        System.out.println("Create directory: " + newDirectoryPath);
                        System.out.println("Create new file:  " + postFilePath);
                    }
                }
                postFile = new File(postFilePath);
            } else {
                if (printDebugMessage && fileExist) {
                    System.out.println("Overwrite file:   " + postFilePath.substring(1));
                } else {
                    System.out.println("Create new file:  " + postFilePath.substring(1));
                }
            }
            if (printDebugMessage && !currentDirectory && fileExist) System.out.println("Overwrite file:   " + postFilePath);
            BufferedWriter postFileWriter = new BufferedWriter(new FileWriter(postFile));
            postFileWriter.write(extractPostBodyFileContent());
            postFileWriter.close();
        }
    }

    private void printAllFiles(String path, StringBuilder respondBody) {
        File curDir = new File(path);
        File[] filesList = curDir.listFiles();
        if (null != filesList) {
            for (File file : filesList) {
                if(file.isFile()) {
                    respondBody.append(".").append(file.getPath().substring(directoryPath.length())).append(crlf);
                } else if (file.isDirectory()) {
                    printAllFiles(path + "/" + file.getName(), respondBody);
                }
            }
        }
    }

    private void generateRespond() {
        if (404 == statusCode) {
            respond.append("HTTP/1.1 404 NOT FOUND\r\n");
            respondBody.append("404 Not Found\r\n");
            respondBody.append("The requested URL was not found on the server.\r\n");
            respondBody.append("If you entered the URL manually, please check you spelling and try again.\r\n");
        } else if (403 == statusCode) {
            respond.append("HTTP/1.1 403 Forbidden\r\n");
            respondBody.append("403 Forbidden\r\n");
            respondBody.append("You tried to leave the working directory, which is not allowed for security reason.\r\n");
        } else if (400 == statusCode) {
            respond.append("HTTP/1.1 40 Bad Request\r\n");
        } else {
            respond.append("HTTP/1.1 200 OK\r\n");
            // TODO: show create/overwrite info
            respondBody.append("Post file successfully.");
        }
        respond.append("Connection: close\r\n");
        respond.append("Server: httpfs\n");
        respond.append("Date: ").append(Calendar.getInstance().getTime().toString()).append(crlf);
        respond.append("Content-Type: ").append("plain/text").append(crlf);
        respond.append("Content-Length: ").append(respondBody.length()).append(crlf);
        respond.append(crlf);
        respond.append(respondBody.toString());
    }

    // TODO: client -d does not work
    private String extractPostBodyFileContent() {
        StringBuilder fileContent = new StringBuilder();
        boolean readBody = false;
        for (String line : postRequestBody) {
            if (line.equals(crlf)) {
                if (!readBody) {
                    readBody = true;
                } else {
                    break;
                }
            } else if (readBody) {
                fileContent.append(line);
            }
        }
        return fileContent.toString();
    }
}
