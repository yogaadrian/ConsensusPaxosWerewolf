/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientpaxos;

import java.io.BufferedReader;
import java.io.FileReader;
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
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import player.Player;
import vote.Vote;

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
    static public ArrayList<Player> listAlivePlayer = new ArrayList();
    static public ArrayList<Player> listDeadPlayer = new ArrayList();
    static public ArrayList<Vote> listVote = new ArrayList();
    static public int port;
    static public int sequence = 0;

    static public boolean play = false;
    static public boolean day = true;
    static public long days;
    static public String role;
    static public ArrayList<String> friend = new ArrayList();
    static public int isAlive;

    static public String paxos_role = "";
    static public long timeout = 6000;
    static public String phase;
    static public boolean ismajority = false;
    static public boolean sendKPUID = false;
    static public boolean voteInput = false;
    static public Scanner scanner;
    static public String votePhase;
    static public int acc_kpu_id = -1;
    static public boolean kpuselected = false;
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        // TODO code application logic here

        String host = "";
        int port = 0;
        
        try(BufferedReader br = new BufferedReader(new FileReader("ipserver.txt"))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            if (line != null) {
                host = line;
                line = br.readLine();
                if (line != null) {
                    port = Integer.parseInt(line);
                }
            }
        }
        
        scanner = new Scanner(System.in);

        //System.out.print("Input server IP hostname : ");
        //host = scan.nextLine();
        //System.out.print("Input server Port : ");
        //port = scan.nextInt();
        //scan.nextLine();
        clientSocket = new Socket(host, port);
        System.out.println("Connected");
        Thread t = new Thread(new StringGetter());
        t.start();
        while (true) {
            sleep(100);
            System.out.print("COMMAND : ");
            //send msg to server
            String msg = scanner.next();
            ParseCommand(msg);
        }
    }

    //parser command ke server
    public static void ParseCommand(String msg) throws Exception {
        if (!voteInput) {
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
                System.out.println("Send to server : " + json);
                sendToServer(json);
            } else if (msg.equals("leave")) {
                String json;
                player_id = -1;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", "leave");
                json = jsonObject.toString();
                System.out.println("Send to server : " + json);
                sendToServer(json);
            } else if (msg.equals("ready")) {
                String json;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", "ready");
                json = jsonObject.toString();
                System.out.println("Send to server : " + json);
                sendToServer(json);
            } else if (msg.equals("client_address")) {
                getClientAddress();
            } else if (msg.equals("prepare")) {
                UDPThread.propose();
            } else {
                String json;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", msg);
                json = jsonObject.toString();
                System.out.println("Send to server : " + json);
                sendToServer(json);
            }
        } else {
            Vote(msg);
        }
    }
    
    public static void Vote(String votedName) throws Exception {
        if (votePhase.equals("day")) {
            // Kondisi siang
            String jsonVote;
            JSONObject jsonObject = new JSONObject();
            int player_id = -1;
            for (int i = 0; i < listPlayer.size(); i++) {
                if (listPlayer.get(i).getUsername().equals(votedName)) {
                    player_id = listPlayer.get(i).getPlayerId();
                }
            }
            if (player_id != -1) {
                System.out.println("player_id : " + player_id);
                //method yang akan dikirimkan
                jsonObject.put("method", "vote_civilian");
                jsonObject.put("player_id", player_id);
                String jsonOut = jsonObject.toString();
                System.out.println("kirim ke kpu : " + jsonOut);
                voteInput = false;

                //Kirim ke KPUjoin
                for (int i = 0; i < listPlayer.size(); i++) {
                    if (listPlayer.get(i).getPlayerId() == acc_kpu_id) {
                        UDPThread.sendReliableMessage(listPlayer.get(i).getAddress(), listPlayer.get(i).getPort(), jsonOut);
                    }
                }
            }
        } else if (votePhase.equals("night")) {
            // Kondisi malam
            String jsonVote;
            JSONObject jsonObject = new JSONObject();
            System.out.println("nama player yg akan dibunuh : " + votedName);
            int player_id = -1;
            for (int i = 0; i < listAlivePlayer.size(); i++) {
                if (listAlivePlayer.get(i).getUsername().equals(votedName) && listAlivePlayer.get(i).getPlayerId() != player_id && !friend.get(0).equals(listAlivePlayer.get(i).getUsername())) {
                    player_id = listAlivePlayer.get(i).getPlayerId();
                }
            }
            if (player_id != -1) {
                //method yang akan dikirimkan
                jsonObject.put("method", "vote_werewolf");
                jsonObject.put("player_id", player_id);
                String jsonOut = jsonObject.toString();
                System.out.println("kirim ke kpu : " + jsonOut);
                voteInput = false;

                //Kirim ke KPU
                for (int i = 0; i < listPlayer.size(); i++) {
                    if (listPlayer.get(i).getPlayerId() == acc_kpu_id) {
                        UDPThread.sendReliableMessage(listPlayer.get(i).getAddress(), listPlayer.get(i).getPort(), jsonOut);
                    }
                }
            } else {
                System.out.println("Error! Player yang divote tidak valid");
                StringBuilder aliveCivilians = new StringBuilder("(");
                for (int i = 0; i < listAlivePlayer.size(); i++) {
                    if (listAlivePlayer.get(i).getPlayerId() != player_id && !friend.get(0).equals(listAlivePlayer.get(i).getUsername())) {
                        if (!aliveCivilians.toString().equals("(")) {
                            aliveCivilians.append(", ");
                        }
                        aliveCivilians.append(listAlivePlayer.get(i).getUsername());
                    } 
                }
                aliveCivilians.append(")");
                System.out.print("vote player yang akan dibunuh " + aliveCivilians.toString() + ": ");
            }
        }
    }
    
    public static void getClientAddress() throws Exception {
        String json;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "client_address");
        json = jsonObject.toString();
        System.out.println("Send to server : " + json);
        sendToServer(json);
    }

    public static class StringGetter
            extends Thread {

        public void run() {
            try {
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String response;
                while (true) {
                    response = inFromServer.readLine();
                    if (!voteInput) {
                        System.out.println("Receive from server : " + response);
                        Parse(response);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);

                //fail respons yang aneh aneh itu di exception ato gimana sih?
            }
        }

        public void Parse(String str) throws ParseException, Exception {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(str);

            if (json.get("status") != null) {
                String status = (String) json.get("status");
                if (status.equals("fail")) {
                    System.out.println("FAIL");
//                    String description = (String) json.get("description");
//                    System.out.println("description " + description);
                    if (sendKPUID) {
                        UDPThread.propose();
                        sendKPUID = false;
                    }
                } else if (status.equals("error")) {
                    System.out.println("ERROR");
//                    String description = (String) json.get("description");
//                    System.out.println("description " + description);
                } else if (status.equals("ok")) {
                    System.out.println("OK");
                    //JIKA ADA JSON DENGAN KUNCI TERSEBUT
                    if (json.get("player_id") != null) {
                        player_id = Integer.parseInt(json.get("player_id").toString());
                        System.out.println("ID player :" + player_id);
                        Thread t = new Thread(new UDPThread());
                        t.start();
                    }
                    if (json.get("description") != null) {
                        System.out.println("description : " + json.get("description").toString());
                    }
                    if (json.get("clients") != null) {
                        listPlayer.clear();
                        listAlivePlayer.clear();
                        System.out.println("clients");
                        JSONArray jsonarray = (JSONArray) json.get("clients");
                        for (int i = 0; i < jsonarray.size(); i++) {
                            JSONObject temp = (JSONObject) jsonarray.get(i);
                            System.out.println(temp.toJSONString());
                            listPlayer.add(new Player(Integer.parseInt(temp.get("player_id").toString()), Integer.parseInt(temp.get("is_alive").toString()), (String) temp.get("address"), Integer.parseInt(temp.get("port").toString()), temp.get("username").toString()));
                            if (listPlayer.get(i).is_alive == 0) {
                                listPlayer.get(i).setRole(temp.get("role").toString());
                                boolean found = false;
                                for (int j = 0; j < listDeadPlayer.size(); j++) {
                                    if (listPlayer.get(i).getPlayerId() == listDeadPlayer.get(j).getPlayerId()) {
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    System.out.println("Player yang terbunuh adalah " + listPlayer.get(i).getUsername() + " (" + listPlayer.get(i).getRole() + ")");
                                    listDeadPlayer.add(listPlayer.get(i));
                                }
                            } else {
                                listAlivePlayer.add(listPlayer.get(i));
                            }
                            if (listPlayer.get(i).getPlayerId() == player_id) {
                                isAlive = listPlayer.get(i).getIsAlive();
                            }
                        }
                        if ((player_id != listPlayer.size() - 1) && (player_id != listPlayer.size() - 2)) {
                            paxos_role = "acceptor";
                        } else {
                            paxos_role = "proposer";
                        }
                        if (!kpuselected) {
                            phase = "prepare";
                            if (paxos_role.equals("proposer") && phase.equals("prepare")) {
                                UDPThread.propose();
                            }
                        }
                    }
                }
            } else if (json.get("method") != null) { //kalau yang diterima adalah protocol 12 ke bawah
                String method = (String) json.get("method");
                if (method.equals("start")) {
                    play = true;
                    days = 0;
                    if (json.get("time").toString().equals("day")) {
                        day = true;
                    } else {
                        day = false;
                    }
                    if (json.get("role").toString().equals("civilian")) {
                        role = "civilian";
                    } else {
                        role = "werewolf";
                        JSONArray jsonarraytemp = (JSONArray) json.get("friend");
                        for (int i = 0; i < jsonarraytemp.size(); i++) {
                            String tmp = (String) jsonarraytemp.get(i);
                            friend.add(tmp);
                        }
                    }
                    String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "ok");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println("kirim ke server : " + response);
                    sendToServer(response);
                    getClientAddress();
                } else if (method.equals("change_phase")) {
                    if (json.get("time").toString().equals("day")) {
                        if (!day) {
                            kpuselected = false;
                            getClientAddress();
                        }
                        day = true;
                    } else {
                        if (role.equals("werewolf")) {
                            StringBuilder friendList = new StringBuilder("");
                            for (int i = 0; i < friend.size(); i++) {
                                friendList.append(friend.get(i));
                            }
                            System.out.println("Teman sesama werewolf : " + friendList.toString());
                        }
                        day = false;
                    }
                    days = (long) json.get("days");
                    System.out.println("description : " + json.get("description").toString());
                    String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "ok");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println("kirim ke server : " + response);
                    sendToServer(response);
                    getClientAddress();
                } else if (method.equals("game_over")) {
                    play = false;
                    if (json.get("winner").toString().equals(role)) {
                        System.out.println("you win");
                    } else {
                        System.out.println("you lose");
                    }
                    String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "ok");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println("kirim ke server : " + response);
                    sendToServer(response);
                } else if (method.equals("vote_now")) {
                    if (day) {
                        if (isAlive == 1) {
                            voteInput = true;
                            long currentTime = System.currentTimeMillis();
                            long dur = System.currentTimeMillis() - currentTime;
                            while (dur < timeout * 2) {
                                dur = System.currentTimeMillis() - currentTime;
                            }
                            StringBuilder alivePlayers = new StringBuilder("(");
                            for (int i = 0; i < listAlivePlayer.size(); i++) {
                                if (listAlivePlayer.get(i).getPlayerId() != player_id) {
                                    if (!alivePlayers.toString().equals("(")) {
                                        alivePlayers.append(", ");
                                    }
                                    alivePlayers.append(listAlivePlayer.get(i).getUsername());
                                } 
                            }
                            alivePlayers.append(")");
                            System.out.print("vote player yang akan dibunuh " + alivePlayers.toString() + ": ");
                            votePhase = (String) json.get("phase");
                        }
                    } else {
                        if (role.equals("werewolf")) {
                            voteInput = true;
                            long currentTime = System.currentTimeMillis();
                            long dur = System.currentTimeMillis() - currentTime;
                            while (dur < timeout * 2) {
                                dur = System.currentTimeMillis() - currentTime;
                            }
                            StringBuilder aliveCivilians = new StringBuilder("(");
                            for (int i = 0; i < listAlivePlayer.size(); i++) {
                                if (listAlivePlayer.get(i).getPlayerId() != player_id && !friend.get(0).equals(listAlivePlayer.get(i).getUsername())) {
                                    if (!aliveCivilians.toString().equals("(")) {
                                        aliveCivilians.append(", ");
                                    }
                                    aliveCivilians.append(listAlivePlayer.get(i).getUsername());
                                } 
                            }
                            aliveCivilians.append(")");
                            System.out.print("vote player yang akan dibunuh " + aliveCivilians.toString() + ": ");
                            votePhase = (String) json.get("phase");
                        }
                    }
                } else if (method.equals("kpu_selected")) {
                    System.out.println("KPU SELECTED");
                    int kpu_id = Integer.parseInt(json.get("kpu_id").toString());
                    System.out.println("kpu_id : " + kpu_id);
                    acc_kpu_id = kpu_id;
                    sendKPUID = false;
                    kpuselected = true;
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
        long max_proposed_id[] = new long[2];
        InetAddress IPAddress;
        int senderport;
        static UnreliableSender unreliableSender;
        static DatagramSocket clientSocket;
        static int ok_count;
        static int totalVote;
        static int nwerewolf = 0;

        UDPThread() throws SocketException, Exception {
            clientSocket = new DatagramSocket(port);
            unreliableSender = new UnreliableSender(clientSocket);
            phase = "prepare";
        }

        public void run() {
            try {
                receiveMessage();
            } catch (IOException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public static void propose() throws Exception {
            if (paxos_role.equals("proposer") && !kpuselected) {
                String json;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", "prepare_proposal");
                JSONArray ja = new JSONArray();
                ja.add(++sequence);
                ja.add(player_id);
                jsonObject.put("proposal_id", ja);
                json = jsonObject.toString();
                System.out.println("kirim ke acceptor : " + json);
                ok_count = 0;
                if (!kpuselected) {
                    Thread t = new Thread(new MajorityChecker());
                    t.start();
                    for (int i = 0; i < listPlayer.size(); i++) {
                        if ((listPlayer.get(i).getPlayerId() != listPlayer.size() - 1) && (listPlayer.get(i).getPlayerId() != listPlayer.size() - 2)) {
                            sendMessage(listPlayer.get(i).getAddress(), listPlayer.get(i).getPort(), json);
                        }
                    }
                }
                
            }
        }

        public static void accept() throws Exception {
            if (paxos_role.equals("proposer") && !kpuselected) {
                String json;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", "accept_proposal");
                JSONArray ja = new JSONArray();
                ja.add(sequence);
                ja.add(player_id);
                jsonObject.put("proposal_id", ja);
                int kpu_id;
                if (acc_kpu_id == -1) {
                    kpu_id = player_id;
                } else {
                    kpu_id = acc_kpu_id;
                }
                jsonObject.put("kpu_id", kpu_id);
                json = jsonObject.toString();
                System.out.println("kirim ke acceptor : " + json);
                ok_count = 0;
                if (!kpuselected) {
                    Thread t = new Thread(new MajorityChecker());
                    t.start();
                    for (int i = 0; i < listPlayer.size(); i++) {
                        if ((listPlayer.get(i).getPlayerId() != listPlayer.size() - 1) && (listPlayer.get(i).getPlayerId() != listPlayer.size() - 2)) {
                            sendMessage(listPlayer.get(i).getAddress(), listPlayer.get(i).getPort(), json);
                        }
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
        
        public static void sendReliableMessage(String IP, int targetPort, String sentence) throws Exception {

            InetAddress IPAddress = InetAddress.getByName(IP);

            byte[] sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
            clientSocket.send(sendPacket);
        }

        public void receiveMessage() throws Exception {

            byte[] receiveData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                IPAddress = receivePacket.getAddress();
                senderport = receivePacket.getPort();

                sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("received from udp client : " + sentence);
                Parse(sentence);
            }
        }

        public void Parse(String str) throws ParseException, IOException, Exception {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(str);

            if (json.get("method") != null) {
                String method = (String) json.get("method");
                if (method.equals("prepare_proposal")) {
                    if (!kpuselected) {
                        try {
                            JSONArray jsonarray = (JSONArray) json.get("proposal_id");
                            long curid[] = new long[2];
                            curid[0] = (long) jsonarray.get(0);
                            curid[1] = (long) jsonarray.get(1);
                            boolean valid = false;
                            if (curid[0] > max_proposed_id[0]) {
                                valid = true;
                            } else if (curid[0] == max_proposed_id[0]) {
                                if (curid[1] > max_proposed_id[1]) {
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
                            if (!kpuselected) {
                                // convert JSONObject to JSON to String
                                String response = jsonObject.toString();
                                System.out.println("kirim ke udp client : " + response);
                                byte[] sendData = response.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                                unreliableSender.send(sendPacket);
                            }
                            
                            if (valid) {
                                max_proposed_id[0] = curid[0];
                                max_proposed_id[1] = curid[1];
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else if (method.equals("accept_proposal")) {
                    if (!kpuselected) {
                        try {
                            JSONArray jsonarray = (JSONArray) json.get("proposal_id");
                            long curid[] = new long[2];
                            curid[0] = (long) jsonarray.get(0);
                            curid[1] = (long) jsonarray.get(1);
                            boolean valid = false;
                            int kpu_id = -1;
                            if (curid[0] == max_proposed_id[0]) {
                                if (curid[1] == max_proposed_id[1]) {
                                    kpu_id = Integer.parseInt(json.get("kpu_id").toString());
                                    if ((acc_kpu_id == -1) || (kpu_id == acc_kpu_id)) {
                                        valid = true;
                                    }
                                }
                            }
                            JSONObject jsonObject = new JSONObject();
                            if (valid) {
                                jsonObject.put("status", "ok");
                                jsonObject.put("description", "accepted");
                            } else {
                                jsonObject.put("status", "fail");
                                jsonObject.put("description", "rejected");
                            }
                            
                            if (!kpuselected) {
                                // convert JSONObject to JSON to String
                                String response = jsonObject.toString();
                                System.out.println("kirim ke udp client : " + response);
                                byte[] sendData = response.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                                unreliableSender.send(sendPacket);
                            }

                            if (valid) {
                                //kirim kpu_id ke learner
                                String jsonOut;
                                JSONObject jsonObjectOut = new JSONObject();
                                jsonObjectOut.put("method", "accepted_proposal");
                                jsonObjectOut.put("kpu_id", kpu_id);
                                jsonObjectOut.put("Description", "Kpu is selected");
                                jsonOut = jsonObjectOut.toString();
                                System.out.println(jsonOut);
                                sendToServer(jsonOut);
                                sendKPUID = true;
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else if (method.equals("vote_werewolf")) {
                    int vote_id = 0;
                    int civilian_id = Integer.parseInt(json.get("player_id").toString());
                    boolean found = false;
                    for (int i = 0; i < listAlivePlayer.size(); i++) {
                        if (listAlivePlayer.get(i).getPlayerId() == civilian_id) {
                            found = true;
                        }
                    }
                    if (found) {
                        totalVote++;
                        vote_id = totalVote;
                        if (vote_id == 1) {
                            listVote.clear();
                            for (int i = 0; i < listAlivePlayer.size(); i++) {
                                listVote.add(new Vote(listAlivePlayer.get(i).getPlayerId(), 0));                             
                            }
                            nwerewolf = 2;
                            for (int i = 0; i < listDeadPlayer.size(); i++) {
                                if (listDeadPlayer.get(i).getRole().equals("werewolf")) {
                                    nwerewolf--;
                                }
                            }
                        }
                        for (int i = 0; i < listVote.size(); i++) {
                            if (listVote.get(i).getPlayerId() == civilian_id) {
                                int currentVote = listVote.get(i).getCountVote();
                                listVote.get(i).setCountVote(currentVote + 1);
                            }
                        }
                        String response;
                        //build jsonObject
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("status", "ok");
                        jsonObject.put("description", "vote werewolf accepted");
                        //convert JSONObject to JSON to String
                        response = jsonObject.toString();
                        System.out.println("kirim ke udp client : " + response);
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                        clientSocket.send(sendPacket);
                        if (vote_id == nwerewolf) {
                            countVotes();
                        }
                    } else {
                        String response;
                        //build jsonObject
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("status", "fail");
                        jsonObject.put("description", "player_id not valid");
                        //convert JSONObject to JSON to String
                        response = jsonObject.toString();
                        System.out.println("kirim ke udp client : " + response);
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                        clientSocket.send(sendPacket);
                    }
                } else if (method.equals("vote_civilian")) {
                    int vote_id = 0;
                    int player_id = Integer.parseInt(json.get("player_id").toString());
                    boolean found = false;
                    for (int i = 0; i < listAlivePlayer.size(); i++) {
                        if (listAlivePlayer.get(i).getPlayerId() == player_id) {
                            found = true;
                        }
                    }
                    if (found) {
                        totalVote++;
                        vote_id = totalVote;
                        if (vote_id == 1) {
                            listVote.clear();
                            for (int i = 0; i < listAlivePlayer.size(); i++) {
                                listVote.add(new Vote(listAlivePlayer.get(i).getPlayerId(), 0));
                            }
                        }
                        for (int i = 0; i < listVote.size(); i++) {
                            if (listVote.get(i).getPlayerId() == player_id) {
                                int currentVote = listVote.get(i).getCountVote();
                                listVote.get(i).setCountVote(currentVote + 1);
                            }
                        }
                        String response;
                        //build jsonObject
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("status", "ok");
                        jsonObject.put("description", "vote civilian accepted");
                        //convert JSONObject to JSON to String
                        response = jsonObject.toString();
                        System.out.println("kirim ke udp client : " + response);
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                        clientSocket.send(sendPacket);
                        if (vote_id == listAlivePlayer.size()) {
                            countVotes();
                        }
                    } else {
                        String response;
                        //build jsonObject
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("status", "fail");
                        jsonObject.put("description", "player_id not valid");
                        //convert JSONObject to JSON to String
                        response = jsonObject.toString();
                        System.out.println("kirim ke udp client : " + response);
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                        clientSocket.send(sendPacket);
                    }
                } else {
                    String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "error");
                    jsonObject.put("description", "wrong request");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println("kirim ke udp client: " + response);
                    byte[] sendData = response.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, senderport);
                    clientSocket.send(sendPacket);
                }
            } else if (json.get("status") != null && !voteInput) {
                String status = (String) json.get("status");
                if (status.equals("ok")) {
                    if (paxos_role.equals("proposer")) {
                        if (phase.equals("prepare")) {
                            ok_count++;
                            if (json.get("previous_accepted") != null) {
                                acc_kpu_id = Integer.parseInt(json.get("previous_accepted").toString());
                            }
                        } else if (phase.equals("accept")) {
                            ok_count++;
                        }
                    }
                }
                if (status.equals("fail")) { 
                    if (paxos_role.equals("proposer")) {
                        if (phase.equals("prepare")) {
                            long currentTime = System.currentTimeMillis();
                            long dur = System.currentTimeMillis() - currentTime;
                            while(dur < timeout) {
                                dur = System.currentTimeMillis() - currentTime;
                            }
                            propose();
                        } else if (phase.equals("accept")) {
                            long currentTime = System.currentTimeMillis();
                            long dur = System.currentTimeMillis() - currentTime;
                            while(dur < timeout) {
                                dur = System.currentTimeMillis() - currentTime;
                            }
                            propose();
                        }
                    }
                    if (kpuselected &&!voteInput) {
                        if (day) {
                            if (isAlive == 1) {
                                voteInput = true;
                                long currentTime = System.currentTimeMillis();
                                long dur = System.currentTimeMillis() - currentTime;
                                while (dur < timeout * 2) {
                                    dur = System.currentTimeMillis() - currentTime;
                                }
                                StringBuilder alivePlayers = new StringBuilder("(");
                                for (int i = 0; i < listAlivePlayer.size(); i++) {
                                    if (listAlivePlayer.get(i).getPlayerId() != player_id) {
                                        if (!alivePlayers.toString().equals("(")) {
                                            alivePlayers.append(", ");
                                        }
                                        alivePlayers.append(listAlivePlayer.get(i).getUsername());
                                    } 
                                }
                                alivePlayers.append(")");
                                System.out.print("vote player yang akan dibunuh " + alivePlayers.toString() + ": ");
                            }
                        } else {
                            if (role.equals("werewolf")) {
                                voteInput = true;
                                long currentTime = System.currentTimeMillis();
                                long dur = System.currentTimeMillis() - currentTime;
                                while (dur < timeout * 2) {
                                    dur = System.currentTimeMillis() - currentTime;
                                }
                                StringBuilder aliveCivilians = new StringBuilder("(");
                                for (int i = 0; i < listAlivePlayer.size(); i++) {
                                    if (listAlivePlayer.get(i).getPlayerId() != player_id && !friend.get(0).equals(listAlivePlayer.get(i).getUsername())) {
                                        if (!aliveCivilians.toString().equals("(")) {
                                            aliveCivilians.append(", ");
                                        }
                                        aliveCivilians.append(listAlivePlayer.get(i).getUsername());
                                    } 
                                }
                                aliveCivilians.append(")");
                                System.out.print("vote player yang akan dibunuh " + aliveCivilians.toString() + ": ");
                            }
                        }
                    }
                } else {

                }
            }

        }

        public static class MajorityChecker extends Thread {

            static long sendTimeMillis;
            static int playerSize;

            MajorityChecker() {
                sendTimeMillis = System.currentTimeMillis();
                playerSize = listPlayer.size();
            }

            public void run() {
                while (true) {
                    long duration = System.currentTimeMillis() - sendTimeMillis;
                    int majority = (playerSize - 2) / 2;
                    //System.out.println("start : " + duration);
                    while ((ok_count <= majority) && (duration < timeout)) {
                        duration = System.currentTimeMillis() - sendTimeMillis;
                    }
                    //System.out.println("end : " + duration);
                    if (ok_count > majority) {
                        if (phase.equals("prepare")) {
                            try {
                                phase = "accept";
                                accept();
                                break;
                            } catch (Exception ex) {
                                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else if (phase.equals("accept")) {
                            try {
                                break;
                            } catch (Exception ex) {
                                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    } else {
                        try {
                            if (phase.equals("prepare")) {
                                propose();
                                break;
                            } else if (phase.equals("accept")) {
                                accept();
                                break;
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);

                        }
                    }
                }
            }
        }
        
        public static void countVotes() {
            // mencari vote terbanyak
            String jsonVote;
            JSONObject jsonObject = new JSONObject();
            int max = -1;
            boolean found = false;
            int player_killed = -1;
            for (int i = 0; i < listVote.size(); i++) {
                System.out.println("(" + listVote.get(i).getPlayerId() + ", " + listVote.get(i).getCountVote() + ")");
                if (listVote.get(i).getCountVote() > max) {
                    max = listVote.get(i).getCountVote();
                    player_killed = listVote.get(i).getPlayerId();
                    found = true;
                } else if (listVote.get(i).getCountVote() == max) {
                    found = false;
                }
            }
            //temp array
            JSONArray ja = new JSONArray();
            for (int i = 0; i < listVote.size(); i++) {
                JSONArray temparray = new JSONArray();
                temparray.add(listVote.get(i).getPlayerId());
                temparray.add(listVote.get(i).getCountVote());
                ja.add(temparray);
            }
            if (found) {
                if (votePhase.equals("day")) {
                    jsonObject.put("method", "vote_result_civilian");
                } else if (votePhase.equals("night")) {
                    jsonObject.put("method", "vote_result_werewolf");
                }
                jsonObject.put("vote_status", 1);
                jsonObject.put("player_killed", player_killed);
                jsonObject.put("vote_result", ja);
                jsonVote = jsonObject.toString();
                System.out.println("kirim ke server : " + jsonVote);
                try {
                    sendToServer(jsonVote);
                } catch (Exception ex) {
                    Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                if (votePhase.equals("day")) {
                    jsonObject.put("method", "vote_result_werewolf");
                } else if (votePhase.equals("night")) {
                    jsonObject.put("method", "vote_result_civilian");
                }
                jsonObject.put("vote_status", -1);
                jsonObject.put("vote_result", ja);
                jsonVote = jsonObject.toString();
                System.out.println("kirim ke server : " + jsonVote);
                try {
                    sendToServer(jsonVote);
                } catch (Exception ex) {
                    Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            totalVote = 0;
        }
    }

}
