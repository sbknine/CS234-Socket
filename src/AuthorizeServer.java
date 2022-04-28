import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.Base64;

public class AuthorizeServer {
    private DatagramSocket socketServer;
    private byte[] bufSend;
    private byte[] bufRec = new byte[256];
    private int serverPort;
    private InetAddress address;

    private String text = null;
    private String[] getPort = null;
    private String[] checkUser;
    private String[] info;
    private String action;



    public void searchPort() throws FileNotFoundException {
        File server = new File("server.config");
        Scanner readFile = new Scanner(server);

        while (readFile.hasNext()) {
            text = readFile.nextLine();
            getPort = text.split("=");
            if (getPort[0].equals("authorize_server_port"))
                serverPort = Integer.parseInt(getPort[1]);

        }
    }

    public void decode(String encodedString){
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        String decodedString = new String(decodedBytes);
        try {
            userCheckAction(decodedString);
        } catch (FileNotFoundException exception) {
            exception.printStackTrace();
        }
    }

    public void userCheckAction(String message) throws FileNotFoundException {
        File authentication = new File("user_pass_action.csv");
        Scanner readUser = new Scanner(authentication);

        String[] check = message.split("\\.");
        while(readUser.hasNext()) {
            text = readUser.nextLine();
            checkUser = text.split(",");
            if (check[0].equals(checkUser[0]) && check[1].equals(checkUser[1])) {
                if(checkUser[2].contains(":")){
                    String[] checkAction = checkUser[2].split(":");
                    for (int i = 0; i < checkAction.length; i++) {
                        if(info[1].equals(checkAction[i])){
                            action = "true";
                        }
                    }
                }else if(info[1].equals(checkUser[2])){
                    action = "true";
                }else{
                    action = "false";
                }
            }
        }
    }

    public void runServer() throws IOException {
        searchPort();
        socketServer = new DatagramSocket(serverPort);
        while (true){
            DatagramPacket packet = new DatagramPacket(bufRec,bufRec.length);
            socketServer.receive(packet);
            String message = new String(packet.getData(),0, packet.getLength());
            info = message.split(":");

            decode(info[0]);
            String messageToClient = info[1]+":"+action;

            address = packet.getAddress();
            int port = packet.getPort();
            bufSend = messageToClient.getBytes();
            packet = new DatagramPacket(bufSend, bufSend.length, address, port);
            socketServer.send(packet);

        }

    }

    public static void main(String[] args) throws IOException {
        AuthorizeServer authorServer = new AuthorizeServer();
        authorServer.runServer();
    }
}
