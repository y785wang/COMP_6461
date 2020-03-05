import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import com.httpfs.*;

class HttpFileServer {

    private int port;
    private boolean printDebugMessage;
    private String directoryPath;

    HttpFileServer() {
        port = 8007;
        printDebugMessage = false;
        directoryPath = ".";
    }

    void printDebugMessage() { printDebugMessage = true; }

    void setPortNumber(int port) { this.port = port; }

    void setDirectoryPath(String directoryPath) { this.directoryPath += directoryPath; }

    void run() {
//        try {
            MyServerSocket myServerSocket = new MyServerSocket(port);
            while (true) {
                MyServerSocket newServerSocket = myServerSocket.accept();
                new handleClientRequest(newServerSocket, printDebugMessage, directoryPath);

            }
//        } catch (IOException e) {
//            System.out.println("HttpFileServer.run():  " + e.getMessage());
//        }
    }
}