import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class DataServer {

    private ServerSocket serverSocket;
    private Socket connectionSocket;
    private int serverPort,udpPort;
    private String text = null;
    private String[] getPort = null;

    public void searchPort() throws FileNotFoundException {
        File server = new File("server.config");
        Scanner readFile = new Scanner(server);

        while (readFile.hasNext()) {
            text = readFile.nextLine();
            getPort = text.split("=");
            if (getPort[0].equals("data_server_port"))
                serverPort = Integer.parseInt(getPort[1]);
            if  (getPort[0].equals("authorize_server_port"))
                udpPort = Integer.parseInt(getPort[1]);
        }
    }

    public void runServer() throws IOException {

        searchPort();

        serverSocket = new ServerSocket(serverPort, 1);

        while(true) {
            connectionSocket = serverSocket.accept();
            new DataConnectionThread(connectionSocket).start();

        }
    }

    public class DataConnectionThread extends Thread{

        private Socket connectionSocket;
        private PrintWriter output;
        private BufferedReader input;
        private String message;

        private DatagramSocket socket;
        private InetAddress address;
        private byte[] bufSend;
        private byte[] bufRec = new byte[256];
        private DatagramPacket packet;

        private String authorizeCheck;
        private String mapping;
        private String[] checkAction;

        private String result;

        public DataConnectionThread(Socket socket){
            this.connectionSocket = socket;
        }

        public void run(){

            try {
                while(true) {
                    output = new PrintWriter(connectionSocket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                    message = input.readLine();
                    checkAction = message.split(":");
                    if (checkAction[1].equals("quit")) {
                        connectionSocket.close();
                        return;
                    }else if(checkAction[1].equals("nametoip") || checkAction[1].equals("iptoname")) {
                        authorizeCheck = checkAction[0] + ":" + checkAction[1];
                        mapping = checkAction[2];
                        sendToAuthorize();
                    }else{
                        connectionSocket.close();
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void findMapping() throws FileNotFoundException {
            File server = new File("data_list.csv");
            Scanner readFile = new Scanner(server);

            result = "not found";
            while (readFile.hasNext()) {
                String data = readFile.nextLine();
                String[] getData = data.split(",");
                if(checkAction[1].equals("nametoip")){
                    if(getData[0].equals(mapping)){
                        result = getData[1];
                    }
                }
                else if(checkAction[1].equals("iptoname")){
                    if(getData[1].equals(mapping)){
                        result = getData[0];
                    }
                }
            }

        }

        public void sendToAuthorize() throws IOException{

            socket = new DatagramSocket();
            address = InetAddress.getByName("127.0.0.1");

            String messageToServer = authorizeCheck;
            bufSend = messageToServer.getBytes();
            packet = new DatagramPacket(bufSend, bufSend.length, address, udpPort);
            socket.send(packet);

            packet = new DatagramPacket(bufRec,bufRec.length);
            socket.receive(packet);
            String message = new String(packet.getData(),0, packet.getLength());

            String[] checkStatus = message.split(":");
            if(checkStatus[1].equals("false")){
                connectionSocket.close();
                return;
            }else if(checkStatus[1].equals("true")){
                findMapping();
                output.println(result);
            }

        }
    }

    public static void main(String[] args) throws IOException {
        DataServer dataServer = new DataServer();
        dataServer.runServer();
    }
}


