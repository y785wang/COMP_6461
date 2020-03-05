public class TestUDPServer {

    private void listenAndServe() {
        MyServerSocket myServerSocket = new MyServerSocket(8007);
        while (true) {
            MyServerSocket newServerSocket = myServerSocket.accept();
            new TestHandleRequest(newServerSocket);
        }
    }

    public static void main(String[] args) {
        TestUDPServer server = new TestUDPServer();
        server.listenAndServe();
    }
}