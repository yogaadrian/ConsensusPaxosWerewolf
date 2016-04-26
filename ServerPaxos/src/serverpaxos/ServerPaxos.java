/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverpaxos;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import player.Player;

/**
 *
 * @author yoga
 */
public class ServerPaxos {

    static public ServerSocket server;
    static public int totalPlayer = 0;
    static public int totalReady = 0;
    static public ArrayList<Player> listPlayer = new ArrayList();
    static public boolean play = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String ip;
        String hostname;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Server IP address : " + ip);
            System.out.println("Port : 2000");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        server = new ServerSocket(2000);
        while (true) {
            Socket socket = server.accept();
            System.out.println("Connected");
            ClientController clientcontroller = new ClientController(socket);

            Thread t = new Thread(clientcontroller);
            t.start();
        }

        // TODO code application logic here
    }

    public static class ClientController
            extends Thread {

        public Socket socket;

        public ClientController(Socket clientSocket) {
            this.socket = clientSocket;
        }

        void SendToClient(String msg) throws Exception {
            //create output stream attached to socket
            PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            //send msg to server
            outToClient.print(msg + '\n');
            outToClient.flush();
        }

        void Parse(String msg) throws ParseException, Exception {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(msg);
            String method = (String) json.get("method");
            System.out.println(method);
            if (method.equals("join")) {
                String username = (String) json.get("username");
                if (checkuser(username) && !play) {
                    listPlayer.add(new Player(totalPlayer++, 1, (String) json.get("udp_address"), Integer.parseInt(json.get("udp_port").toString()), username));
                    String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "ok");
                    jsonObject.put("player_id", totalPlayer - 1);

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println("kirim : " +response);
                    SendToClient(response);
                } else if (!checkuser(username)) {
                    String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "fail");
                    jsonObject.put("description", "user exists");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println(json);
                    SendToClient(response);

                } else if(play){
                     String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "fail");
                    jsonObject.put("description", "please wait, game is currently running");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println(json);
                    SendToClient(response);
                } 
            }
        }

        boolean checkuser(String username) {
            for (int i = 0; i < listPlayer.size(); i++) {
                if (listPlayer.get(i).getUsername().equals(username)) {
                    return false;
                }
            }
            return true;
        }

        public void run() {
            try {
                System.out.println(socket.getInetAddress().toString() + ':' + socket.getPort());
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String request;
                while ((request = inFromClient.readLine()) != null) {
                    System.out.println(request);
                    Parse(request);
                }
            } catch (SocketException ex) {
                System.out.println("disconnect 1 client");
            } catch (IOException ex) {
                Logger.getLogger(ServerPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(ServerPaxos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
