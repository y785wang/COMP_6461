import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

class SelectiveRepeatSender {
    private int maxDataLength;
    private int currentPacket;
    private int endCounter;
    private HashMap<Long, Packet> currentWindowPackets;
    private boolean done;

    private DatagramChannel channel;
    private InetSocketAddress receiverAddress;
    private SocketAddress routerAddress;
    private int serverPort;

    private boolean debugMessage;

    SelectiveRepeatSender(DatagramChannel channel, InetSocketAddress receiverAddress, SocketAddress routerAddress) {
        this.channel = channel;
        this.receiverAddress = receiverAddress;
        this.routerAddress = routerAddress;

        serverPort = receiverAddress.getPort();
        maxDataLength= 1013;
//        maxDataLength = 10;
        currentPacket = 0;
        endCounter = 3;
        currentWindowPackets = new HashMap<>();
        done = false;
        debugMessage = true;
    }

    long send(String data, long windowBeginSeqNum, long totalSequenceNumber) {
        byte[] byteData = data.getBytes();
        long windowSize = totalSequenceNumber / 2;
//        long windowSize = 4;
        long totalPacket = byteData.length / maxDataLength;
        if (0 != byteData.length % maxDataLength) ++totalPacket;

        while(true) {
            try {
                // fill up / create window size Packets
                if (!done) {
                    for (int i = 0; i < windowSize; ++i) {
                        long currentSeqNum = (windowBeginSeqNum + i) % totalSequenceNumber;
                        if (!currentWindowPackets.containsKey(currentSeqNum)) {
                            byte[] packetData;
                            if (currentPacket < totalPacket - 1) {
                                packetData = Arrays.copyOfRange(byteData,
                                        (currentPacket) * maxDataLength,
                                        (currentPacket + 1) * maxDataLength);

                            } else if (currentPacket == totalPacket - 1){
                                packetData = Arrays.copyOfRange(byteData,
                                        (currentPacket) * maxDataLength,
                                        byteData.length);
                            } else {
                                // no more Packets to create
                                break;
                            }
                            ++currentPacket;
                            Packet p = new Packet.Builder()
                                    .setType(0)
                                    .setSequenceNumber(currentSeqNum)
                                    .setPortNumber(receiverAddress.getPort())
                                    .setPeerAddress(receiverAddress.getAddress())
                                    .setPayload(packetData)
                                    .create();
                            currentWindowPackets.put(currentSeqNum, p);
                            channel.send(currentWindowPackets.get(currentSeqNum).toBuffer(), routerAddress);
                            if (debugMessage) System.out.println("Sent to " + serverPort + ": " + currentWindowPackets.get(currentSeqNum));
                        }
                    }
                }

                // Try to receive a packet within timeout.
                channel.configureBlocking(false);
                Selector selector = Selector.open();
                channel.register(selector, OP_READ);
                selector.select(2000);

                Set<SelectionKey> keys = selector.selectedKeys();
                if (keys.isEmpty()) {
                    if (debugMessage) System.out.println("Time out");
                    if (done) {
                        if (--endCounter < 0) {
                            if (debugMessage) System.out.println("Finish sending data");
                            return ++windowBeginSeqNum;
                        }
                        channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                        if (debugMessage) System.out.println("Sent to " + serverPort + ": " + currentWindowPackets.get(windowBeginSeqNum));
                    } else {
                        for (long i = 0; i < windowSize; ++i) {
                            long seqNum = windowBeginSeqNum + i;
                            if (currentWindowPackets.containsKey(seqNum)) { // not all window if filled up at the end
                                channel.send(currentWindowPackets.get(seqNum).toBuffer(), routerAddress);
                                if (debugMessage) System.out.println("Sent to " + serverPort + ": " + currentWindowPackets.get(seqNum));
                            }
                        }
                    }
                } else {
                    ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                    channel.receive(buf);
                    buf.flip();
                    Packet resp = Packet.fromBuffer(buf);
                    if (done) {
                        if (3 == resp.getType()) {
                            channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                            if (debugMessage) System.out.println("Sent to " + serverPort + ": " + currentWindowPackets.get(windowBeginSeqNum));
                        } else if (5 == resp.getType()) {
                            if (debugMessage) System.out.println("Finish sending request");
                            return ++windowBeginSeqNum;
                        }
                    } else if (3 == resp.getType()) {
                        if (debugMessage) System.out.println("    Received: " + resp);
                        long missedSeqNum = resp.getSequenceNumber();
                        if (currentWindowPackets.containsKey(missedSeqNum)) {
                            // send missed Packet
                            channel.send(currentWindowPackets.get(missedSeqNum).toBuffer(), routerAddress);
                            if (debugMessage) System.out.println("Sent to " + serverPort + ": " + currentWindowPackets.get(missedSeqNum));

                            long numACKed = missedSeqNum - windowBeginSeqNum;
                            for (int i = 0; i < numACKed; ++i) {
                                // remove ACKed Packets
                                currentWindowPackets.remove(windowBeginSeqNum);
                                // shift window
                                windowBeginSeqNum = (windowBeginSeqNum + 1) % totalSequenceNumber;
                            }
                        } else if (missedSeqNum == (windowBeginSeqNum + currentWindowPackets.size()) % totalSequenceNumber) {
                            // all received
                            currentWindowPackets.clear();
                            windowBeginSeqNum = missedSeqNum;
                            if (currentPacket == totalPacket) {
                                // finish sending all Packets
                                Packet p = new Packet.Builder()
                                        .setType(4)
                                        .setSequenceNumber(windowBeginSeqNum)
                                        .setPortNumber(receiverAddress.getPort())
                                        .setPeerAddress(receiverAddress.getAddress())
                                        .setPayload("FIN".getBytes())
                                        .create();
                                currentWindowPackets.put(windowBeginSeqNum, p);
                                channel.send(currentWindowPackets.get(windowBeginSeqNum).toBuffer(), routerAddress);
                                if (debugMessage) System.out.println("Sent to " + serverPort + ": " + currentWindowPackets.get(windowBeginSeqNum));
                                done = true;
                            }
                        }
                    }
                }
                keys.clear();
                selector.close();
            } catch (IOException exception) {
                System.out.println("MyClientSocket.selectiveRepeat(): " + exception.getMessage());
            }
        }
    }
}
