public class TestUDPClient {

    private static void runClient(String data) {
        MyClientSocket myClientSocket = new MyClientSocket(3000, 8007);
        myClientSocket.send(data);
        String respond = myClientSocket.receive();
        System.out.println("Respond = " + respond);
    }

    public static void main(String[] args) {

//        String msg = "00000000001111111111222222222233333333334444444444";
        String msg = "0000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999";
        msg +=       "aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffffgggggggggghhhhhhhhhhiiiiiiiiiijjjjjjjjjj";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1; ++i) {
            sb.append(msg);
        }
        msg = sb.toString();
        msg += " The End";

        runClient(msg);
    }
}

