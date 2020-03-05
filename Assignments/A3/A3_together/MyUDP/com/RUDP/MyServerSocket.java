package com.RUDP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

public class MyServerSocket {
    private long totalSequenceNumber;
    private DatagramChannel channel;
    private int serverPort;
    private long initialSeqNum;
    private SocketAddress router;
    private InetAddress clientAddress;
    private int clientPort;
    private SocketAddress routerAddress;
    private long sendSeqNum;
    private boolean debugMessage;

    public MyServerSocket(int serverRootPort) {
        totalSequenceNumber = 4294967295L;
//        totalSequenceNumber = 8;
        serverPort = serverRootPort;
        routerAddress = new InetSocketAddress("localhost", 3000);
        debugMessage = false;

        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(serverRootPort));
        } catch (IOException exception) {
            System.out.println("com.RUDP.MyServerSocket.ctor(): " + exception.getMessage());
        }
    }

    public MyServerSocket accept() {
        if (debugMessage) System.out.println("\nAccepting...");
        try {
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);
            for (; ; ) {
                buf.clear();
                SocketAddress router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();

                if (1 == packet.getType()) {
                    if (debugMessage) System.out.println(serverPort + " received: " + packet);
                    long initialSeqNum = packet.getSequenceNumber();
                    String payLoad = "SYN-ACK " + (serverPort+1);
                    Packet resp = packet.toBuilder()
                            .setType(2)
                            .setPayload(payLoad.getBytes())
                            .create();
                    channel.send(resp.toBuffer(), router);
                    if (debugMessage) System.out.println(serverPort + " sent    : " + resp);
                    MyServerSocket newServerSocket =  new MyServerSocket(++serverPort);
                    newServerSocket.setInitialSeqNum(initialSeqNum, router, packet.getPeerAddress(), packet.getPeerPort());
                    return newServerSocket;
                }
            }
        } catch (IOException exception) {
            System.out.println("com.RUDP.MyServerSocket.accept(): " + exception.getMessage());
            return null;
        }
    }

    private void setInitialSeqNum(long initialSeqNum, SocketAddress router, InetAddress clientAddress, int clientPort) {
        this.initialSeqNum = initialSeqNum;
        this.router = router;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    public String receive() {
        if (debugMessage) System.out.println("\nReceiving at port " + serverPort);
        SelectiveRepeatReceiver selectiveRepeatReceiver = new SelectiveRepeatReceiver(channel, clientAddress, clientPort, router);
        sendSeqNum = selectiveRepeatReceiver.receive(initialSeqNum, totalSequenceNumber, serverPort);
//        System.out.println("receive: " + selectiveRepeatReceiver.getData());
        return selectiveRepeatReceiver.getData();
    }

    public void send(String data) {
        SelectiveRepeatSender selectiveRepeatSender = new SelectiveRepeatSender(channel, new InetSocketAddress("localhost", clientPort), routerAddress);
        if (debugMessage) System.out.println("begin " + sendSeqNum);
        selectiveRepeatSender.send(data, sendSeqNum, totalSequenceNumber);
    }

    public int getServerPort() { return serverPort; }

    public void close() {
        try {
            channel.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
