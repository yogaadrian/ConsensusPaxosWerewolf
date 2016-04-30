/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientpaxos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import player.Player;

/**
 *
 * @author yoga
 */
public class ClientPaxos {

    static public Socket clientSocket;
    static public BufferedReader objectFromServer;
    static public PrintWriter objectToServer;
    static public Scanner scan = new Scanner(System.in);
    static public int player_id = -1;
    static public boolean is_join = false;
    static public ArrayList<Player> listPlayer = new ArrayList();
    static public int port;
    static public int sequence = 0;
    static public String paxos_role;
    static public Thread udpthread;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        // TODO code application logic here
        //cobain udp
        /*
         Scanner reader = new Scanner(System.in);
         System.out.println("Enter menu: ");
         int menu = reader.nextInt();
         reader.nextLine();
         String menuString;
         switch(menu) {
         case 1: menuString = "Send";
         break;
         case 2: menuString = "Receive";
         break;
         default : menuString = "Invalid";
         }
         System.out.println(menuString);
         if (menu == 1) {
         System.out.println("Enter IP: ");
         String IP = reader.nextLine();
         System.out.println("Enter target port: ");
         int targetPort = reader.nextInt();
         sendMessage(IP, targetPort);
         } else if(menu == 2) {
         System.out.println("Enter listen port: ");
         int listenPort = reader.nextInt();
         receiveMessage(listenPort);
         }*/

        Scanner scan = new Scanner(System.in);

        System.out.print("Input server IP hostname : ");
        String host = scan.nextLine();
        System.out.print("Input server Port : ");
        int port = scan.nextInt();
        scan.nextLine();
        clientSocket = new Socket(host, port);
        System.out.println("Connected");
        Thread t = new Thread(new StringGetter());
        t.start();
        while (true) {
            sleep(100);
            System.out.print("COMMAND : ");
            //send msg to server
            String msg = scan.nextLine();
            ParseCommand(msg);
        }
    }

    //parser command ke server
    public static void ParseCommand(String msg) throws Exception {
        if (msg.equals("join")) {
            String json;
            JSONObject jsonObject = new JSONObject();
            System.out.print("Masukkan nama : ");
            String username = scan.nextLine();
            jsonObject.put("method", "join");
            jsonObject.put("username", username);
            jsonObject.put("udp_address", InetAddress.getLocalHost().getHostAddress());
            System.out.print("Masukkan port : ");
            port = scan.nextInt();
            scan.nextLine();
            jsonObject.put("udp_port", port);
            json = jsonObject.toString();
            System.out.println(json);
            sendToServer(json);
        } else if (msg.equals("leave")) {
            String json;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "leave");
            json = jsonObject.toString();
            System.out.println(json);
            sendToServer(json);
        } else if (msg.equals("ready")) {
            String json;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "ready");
            json = jsonObject.toString();
            System.out.println(json);
            sendToServer(json);
        } else if (msg.equals("client address")) {
            String json;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "client_address");
            json = jsonObject.toString();
            System.out.println(json);
            sendToServer(json);
        } else if (msg.equals("prepare")) {
            UDPThread.send();
        }
    }

    public static class StringGetter
            extends Thread {

        public void run() {
            try {
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String response;
                while (true) {
                    response = inFromServer.readLine();
                    System.out.println(response);
                    Parse(response);
                }
            } catch (IOException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void Parse(String str) throws ParseException, Exception {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(str);
            String status = (String) json.get("status");
            if (status.equals("fail")) {
                System.out.println("FAIL");
                String description = (String) json.get("description");
                System.out.println("description " + description);
            } else if (status.equals("error")) {
                System.out.println("ERROR");
                String description = (String) json.get("description");
                System.out.println("description " + description);
            } else if (status.equals("ok")) {
                System.out.println("OK");
                //JIKA ADA JSON DENGAN KUNCI TERSEBUT
                if (json.get("player_id") != null) {
                    player_id = Integer.parseInt(json.get("player_id").toString());
                    System.out.println("ID player :" + player_id);
                    udpthread = new Thread(new UDPThread());
                    udpthread.start();
                }
                if (json.get("description") != null) {
                    System.out.println("description : " + json.get("description").toString());
                }
                if (json.get("clients") != null) {
                    listPlayer.clear();
                    System.out.println("clients");
                    JSONArray jsonarray = (JSONArray) json.get("clients");
                    for(int i=0; i<jsonarray.size();i++){
                        JSONObject temp= (JSONObject)jsonarray.get(i);
                        System.out.println(temp.toJSONString());
                        listPlayer.add(new Player(Integer.parseInt(temp.get("player_id").toString()), Integer.parseInt(temp.get("is_alive").toString()), (String) temp.get("address"), Integer.parseInt(temp.get("port").toString()), temp.get("username").toString()));
                        if (listPlayer.get(i).is_alive==0){
                            listPlayer.get(i).setRole(temp.get("role").toString());
                        }
                    }
                    if ((player_id != listPlayer.size()-1) && (player_id != listPlayer.size()-2)) {
                        paxos_role = "acceptor";
                    } else {
                        paxos_role = "proposer";
                    }
                }

            }
        }
    }

    public static void sendToServer(String msg) throws Exception {
        //create output stream attached to socket
        PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        //send msg to server
        outToServer.print(msg + '\n');
        outToServer.flush();
    }
    
    public static class UDPThread
            extends Thread {
         
         String sentence;
         long maxid[] = new long[2];
         int acc_kpu_id = -1;
         InetAddress IPAddress;
         int senderport;
         static UnreliableSender unreliableSender;
         static DatagramSocket clientSocket;
         
         UDPThread() throws SocketException, Exception {
             clientSocket = new DatagramSocket(port);
             unreliableSender = new UnreliableSender(clientSocket);
             //ParseCommand("client address");
         }
         
        public void run() {
            try {
                //send();
                receiveMessage();
            } catch (IOException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public static void send() throws Exception {
            if (paxos_role.equals("proposer")) {
                String json;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", "prepare_proposal");
                JSONArray ja = new JSONArray();
                ja.add(++sequence);
                ja.add(player_id);
                jsonObject.put("proposal_id", ja);
                json = jsonObject.toString();
                System.out.println(json);
                for (int i = 0; i < listPlayer.size(); i++) {
                    if ((listPlayer.get(i).getPlayerId() != listPlayer.size()-1) && (listPlayer.get(i).getPlayerId() != listPlayer.size()-2)) {
                        System.out.println(listPlayer.get(i).getAddress());
                        System.out.println(listPlayer.get(i).getPort());
                        sendMessage(listPlayer.get(i).getAddress(), listPlayer.get(i).getPort(), json);
                    }
                }
            }
        }
        
         public static void sendMessage(String IP, int targetPort, String sentence) throws Exception {
             
            InetAddress IPAddress = InetAddress.getByName(IP);

            byte[] sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
            unreliableSender.send(sendPacket);
        }
        
        public void receiveMessage() throws Exception {

            byte[] receiveData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                IPAddress = receivePacket.getAddress();
                senderport = receivePacket.getPort();

                sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("RECEIVED: " + sentence);
                Parse(sentence);
            }
        }

        public void Parse(String str) throws ParseException, IOException {
            if (paxos_role.equals("acceptor")) {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(str);
                String method = (String) json.get("method");
                if (method.equals("prepare_proposal")) {
                    try {
                        JSONArray jsonarray = (JSONArray) json.get("proposal_id");
                        long curid[] = new long[2];
                        curid[0] = (long)jsonarray.get(0);
                        curid[1] = (long)jsonarray.get(1);
                        boolean valid = false;
                        if (curid[0] > maxid[0]) {
                            valid = true;
                        } else if (curid[0] == maxid[0]) {
                            if (curid[1] > maxid[1]) {
                                valid = true;
                            }
                        }
                        JSONObject jsonObject = new JSONObject();
                        if (valid) {
                            jsonObject.put("status", "ok");
                            jsonObject.put("description", "accepted");
                            if (acc_kpu_id != -1) {
                                jsonObject.put("previous_accepted", acc_kpu_id);
                            }
                        } else {
                            jsonObject.put("status", "fail");
                            jsonObject.put("description", "rejected");
                        }
                        // convert JSONObject to JSON to String
                        String response = jsonObject.toString();
                        System.out.println("kirim : " + response);
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                        unreliableSender.send(sendPacket);

                        if (valid) {
                            maxid[0] = curid[0];
                            maxid[1] = curid[1];
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                   String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "error");
                    jsonObject.put("description", "wrong request");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println("kirim : " + response);
                    byte[] sendData = response.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                    unreliableSender.send(sendPacket);
                }
            }             
        }
    }

}
