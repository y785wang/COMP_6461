import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

class MyClientSocket {

    private SocketAddress routerAddress;
    private InetSocketAddress serverAddress;
    private DatagramChannel channel;
    private final long totalSequenceNumber = 4294967295L;
//    private final long totalSequenceNumber = 8;
    private long randomSendSeqNum;
    private long receiveSeqNum;
    private int serverPort;
    private boolean debugMessage;

    MyClientSocket(int routerPort, int serverPort) {
        this.serverPort = serverPort;
        this.routerAddress = new InetSocketAddress("localhost", routerPort);
        this.serverAddress = new InetSocketAddress("localhost", serverPort);

        debugMessage = true;
    }

    void send(String data) {
        try {
            channel = DatagramChannel.open();
        } catch (IOException exception) {
            if (debugMessage) System.out.println("MyClientSocket.send(): " + exception.getMessage());
        }
        handShake();
        selectiveRepeat(data);
    }

    private void handShake() {
        if (debugMessage) System.out.println("\nHand Shaking... Server port " + serverPort);
        int handShakeStep = 1;
        boolean connected = false;
        while (!connected) {
            try {
                switch (handShakeStep) {
                    case 1:
                        randomSendSeqNum = (long) (Math.random() * totalSequenceNumber);
                        Packet p1 = new Packet.Builder()
                                .setType(1)
                                .setSequenceNumber(randomSendSeqNum)
                                .setPortNumber(serverPort)
                                .setPeerAddress(serverAddress.getAddress())
                                .setPayload("SYN".getBytes())
                                .create();
                        channel.send(p1.toBuffer(), routerAddress);
                        if (debugMessage) System.out.println("Sent to " + serverPort + ": " + p1);

                        // Try to receive a packet within timeout.
                        channel.configureBlocking(false);
                        Selector selector = Selector.open();
                        channel.register(selector, OP_READ);
                        selector.select(2000);

                        Set<SelectionKey> keys = selector.selectedKeys();
                        if (keys.isEmpty()) {
                            if (debugMessage) System.out.println("Time out");
                        } else {
                            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                            channel.receive(buf);
                            buf.flip();
                            Packet resp = Packet.fromBuffer(buf);
                            if (debugMessage) System.out.println("    Received: " + resp.getPeerPort() + ": " + resp);
                            if (2 == resp.getType() && randomSendSeqNum == resp.getSequenceNumber()) {
                                String payLoad = new String(resp.getPayload(), UTF_8);
                                serverPort = new Integer(payLoad.substring(8));
                                this.serverAddress = new InetSocketAddress("localhost", serverPort);
                                handShakeStep = 3;
                            }

                            keys.clear();
                            selector.close();
                        }
                        break;
                    case 3:
                        Packet p2 = new Packet.Builder()
                                .setType(3)
                                .setSequenceNumber(randomSendSeqNum)
                                .setPortNumber(serverPort)
                                .setPeerAddress(serverAddress.getAddress())
                                .setPayload("ACK".getBytes())
                                .create();
                        channel.send(p2.toBuffer(), routerAddress);
                        if (debugMessage) System.out.println("Sent to " + serverPort + ": " + p2);
                        connected = true;
                        break;
                }
            } catch (IOException exception) {
                System.out.println("MyClientSocket.handShake(), step " + handShakeStep + " " + exception.getMessage());
            }
        }
    }

    private void selectiveRepeat(String data) {
        if (debugMessage) System.out.println("\nSelective Repeating... New server port " + serverPort);
        SelectiveRepeatSender selectiveRepeatSender = new SelectiveRepeatSender(channel, serverAddress, routerAddress);
        receiveSeqNum = selectiveRepeatSender.send(data, randomSendSeqNum, totalSequenceNumber);
    }

    String receive() {
        SelectiveRepeatReceiver selectiveRepeatReceiver = new SelectiveRepeatReceiver(channel, serverAddress.getAddress(), serverPort, routerAddress);
        selectiveRepeatReceiver.receive(receiveSeqNum, totalSequenceNumber, serverPort);
        return selectiveRepeatReceiver.getData();
    }
}
