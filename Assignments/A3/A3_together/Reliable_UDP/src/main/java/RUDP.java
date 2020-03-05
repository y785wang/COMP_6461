import java.net.InetAddress;
import java.net.UnknownHostException;

public class RUDP {
    private final int port;
    private final InetAddress address;
    private byte[] payload;

    public RUDP(int port, String address) throws UnknownHostException {
        this.port=port;
        this.address=InetAddress.getByName(address);
    }




}
