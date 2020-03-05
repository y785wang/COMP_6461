package com.RUDP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

class SelectiveRepeatReceiver {
    private DatagramChannel channel;
    private boolean inputData;
    private InetAddress clientAddress;
    private int clientPort;
    private SocketAddress routerAddress;
    private StringBuilder data;
    private boolean debugMessage;

    SelectiveRepeatReceiver(DatagramChannel channel, InetAddress clientAddress, int clientPort, SocketAddress routerAddress) {
        this.channel = channel;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.routerAddress = routerAddress;

        inputData = false;
        debugMessage = false;
    }

    long receive(long windowBeginSeqNum, long totalSequenceNumber, int serverPort) {
        long windowSize = totalSequenceNumber / 2;
//        long windowSize = 4;
        data = new StringBuilder();

        HashMap<Long, Packet> currentWindowPackets = new HashMap<>();

        ByteBuffer buf = ByteBuffer
                .allocate(Packet.MAX_LEN)
                .order(ByteOrder.BIG_ENDIAN);
        try {
            channel.configureBlocking(false);
            for (; ; ) {
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);
                selector.select(500);
                Set<SelectionKey> keys = selector.selectedKeys();

                if (keys.isEmpty()) {
                    if (inputData) {
                        if (debugMessage) System.out.println("Time out");
                        Packet resp = new Packet.Builder()
                                .setType(3)
                                .setSequenceNumber(windowBeginSeqNum)
                                .setPeerAddress(clientAddress)
                                .setPortNumber(clientPort)
                                .setPayload("ACK".getBytes())
                                .create();
                        channel.send(resp.toBuffer(), routerAddress);
                        if (debugMessage) System.out.println("    " + serverPort + " sent    : " + resp);
                    }
                } else {
                    inputData = true;
                    buf.clear();
                    channel.receive(buf);
                    buf.flip();
                    Packet packet = Packet.fromBuffer(buf);
                    buf.flip();
                    long seqNum = packet.getSequenceNumber();
                    if (4 == packet.getType()) {
                        Packet resp = packet.toBuilder()
                                .setType(5)
                                .setSequenceNumber(windowBeginSeqNum)
                                .setPayload("FIN_ACK".getBytes())
                                .create();
                        channel.send(resp.toBuffer(), routerAddress);
                        if (debugMessage) System.out.println("    " + serverPort + " sent    : " + resp);
                        return ++windowBeginSeqNum;
                    }
                    if (0 != packet.getType()) continue;
                    if (debugMessage) System.out.print(serverPort + " received: " + packet);
                    boolean outOfOrderButWithinRange = false;
                    if (windowBeginSeqNum == seqNum) {
                        // in order
                        if (debugMessage) System.out.print(", in order, deliver #" + seqNum);
                        data.append(new String(packet.getPayload(), UTF_8));
                        windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                        // check buffer
                        for (long i = 0; i < windowSize-1; ++i) {
                            long bufferSeqNum = windowBeginSeqNum;
                            if (currentWindowPackets.containsKey(bufferSeqNum)) {
                                if (debugMessage) System.out.print(", #" + bufferSeqNum);
                                data.append(new String(currentWindowPackets.get(bufferSeqNum).getPayload(), UTF_8));
                                windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                                currentWindowPackets.remove(bufferSeqNum);
                            } else {
                                break;
                            }
                        }
                        if (debugMessage) System.out.println();
                    } else if (windowBeginSeqNum + windowSize <= totalSequenceNumber) {
                        // out of order
                        if (windowBeginSeqNum < seqNum && seqNum < windowBeginSeqNum + windowSize) {
                            // within window range
                            if (debugMessage) System.out.print(", out of order, within range");
                            outOfOrderButWithinRange = true;
                        } else {
                            if (debugMessage) System.out.println(", out of order, out of range, discard it");
                        }
                    } else {
                        // out of order
                        if (debugMessage) System.out.print(", out of order, within range");
                        if (windowBeginSeqNum < seqNum && seqNum < totalSequenceNumber ||
                                0 <= seqNum && seqNum < (windowSize - (totalSequenceNumber - windowBeginSeqNum))) {
                            // within window range
                            outOfOrderButWithinRange = true;
                        } else {
                            if (debugMessage) System.out.println(", out of order, out of range, discard it");
                        }
                    }

                    if (outOfOrderButWithinRange) {
                        // check duplicate
                        if (currentWindowPackets.containsKey(seqNum)) {
                            if (debugMessage) System.out.println(", duplicate, discard it");
                            continue;
                        }
                        if (debugMessage) System.out.println(", not duplicate, buffer it");
                        // buffer it
                        currentWindowPackets.put(seqNum, packet);
                        // send ACK
                        Packet resp = packet.toBuilder()
                                .setType(3)
                                .setSequenceNumber(windowBeginSeqNum)
                                .setPayload("ACK".getBytes())
                                .create();
                        channel.send(resp.toBuffer(), routerAddress);
                        if (debugMessage) System.out.println("    " + serverPort + " sent    : " + resp);
                    }
                }

                keys.clear();
                selector.close();
            }
        } catch (IOException exception) {
            System.out.println("com.RUDP.MyServerSocket.receive(): " + exception.getMessage());
            return -1;
        }
    }

    String getData() { return data.toString(); }
}
