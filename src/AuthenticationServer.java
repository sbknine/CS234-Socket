import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class AuthenticationServer {

    private int serverPort;
    private String text = null;
    private String secretKey;

    public void searchPort() throws FileNotFoundException {
        File server = new File("server.config");
        Scanner readFile = new Scanner(server);

        while (readFile.hasNext()) {
            text = readFile.nextLine();
            String[] getPort = text.split("=");
            if (getPort[0].equals("authentication_server_port"))
                serverPort = Integer.parseInt(getPort[1]);
            if(getPort[0].equals("secret_key"))
                secretKey = getPort[1];
        }
    }


    public void runServer() throws IOException {

        searchPort();
        ServerSocket serverSocket = new ServerSocket(serverPort, 1);

        while(true) {
            Socket connectionSocket = serverSocket.accept();
            new AuthenticConnectThread(connectionSocket,secretKey).start();
        }

    }
    public class AuthenticConnectThread extends Thread{

        private Socket connectionSocket;
        private String info;
        private int success = 0;
        private String secretKey;
        private PrintWriter output;
        private BufferedReader input;

        public AuthenticConnectThread(Socket socket,String secretKey){
            this.connectionSocket = socket;
            this.secretKey = secretKey;
        }

        public void authenticUser(String message) throws FileNotFoundException {
            File authentication = new File("user_pass_action.csv");
            Scanner readUser = new Scanner(authentication);

            String[] check = message.split(":");
            while(readUser.hasNext()) {
                text = readUser.nextLine();
                String[] checkUser = text.split(",");
                if (check[0].equals(checkUser[0]) && check[1].equals(checkUser[1])) {
                    info = check[0] + "." + check[1] + "." + secretKey;
                    encode();
                    success = 1;
                    break;
                }
            }
        }

        public void encode(){
            String encodedString = Base64.getEncoder().encodeToString(info.getBytes());
            output.println(encodedString);
        }

        public void run(){
            try {
                output = new PrintWriter(connectionSocket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

                for (int i = 0; i <= 2;i++) {
                    String message;
                    String user = input.readLine();
                    String pass = input.readLine();
                    String[] used = user.split(":");
                    String[] passed = pass.split(":");
                    message = used[1] + ":" + passed[1];
                    authenticUser(message);

                    if(success == 1){
                        break;
                    }
                }
                if(success == 0){
                    connectionSocket.close();
                    return;
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        AuthenticationServer auServer = new AuthenticationServer();
        auServer.runServer();
    }
}