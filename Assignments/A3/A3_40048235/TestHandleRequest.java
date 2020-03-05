public class TestHandleRequest implements Runnable {

    private MyServerSocket myServerSocket;

    TestHandleRequest(MyServerSocket myServerSocket) {
        this.myServerSocket = myServerSocket;
        if (null == myServerSocket) {
            System.out.println("TestHandleRequest.ctor(): new ServerSocket is null");
            return;
        }
        new Thread(this).start();
    }

    public void run() {
        String data = myServerSocket.receive();
        System.out.println(myServerSocket.getServerPort() + " received request: " + data);
        myServerSocket.send(data);
    }


}
