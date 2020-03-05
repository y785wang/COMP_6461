import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class HttpFileServer {

    private int port;
    private boolean printDebugMessage;
    private String directoryPath;

    HttpFileServer() {
        port = 8080;
        printDebugMessage = false;
        directoryPath = ".";
    }

    void printDebugMessage() { printDebugMessage = true; }

    void setPortNumber(int port) { this.port = port; }

    void setDirectoryPath(String directoryPath) { this.directoryPath += directoryPath; }

    void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket client = serverSocket.accept();
                new handleClientRequest(client, printDebugMessage, directoryPath);
            }
        } catch (IOException e) {
            System.out.println("HttpFileServer.run():  " + e.getMessage());
        }
    }
}