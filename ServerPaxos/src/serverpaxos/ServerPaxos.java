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
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
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
public class ServerPaxos {

    static public ServerSocket server;
    static public int totalClient = 0;
    static public int totalPlayer = 0;
    static public int totalReady = 0;
    static public ArrayList<Player> listPlayer = new ArrayList();
    static public boolean play = false;
    static public boolean day = true;
    static public int days = 0;
    static public int ncivilian;
    static public int nwerewolf;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, Exception {
        String ip;
        String hostname;
        Scanner scan = null;
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
            sleep(100);
        }

        // TODO code application logic here
    }

    //identitas tiap client thread
    public static class ClientController
            extends Thread {

        public Socket socket;
        public int player_id = -1;
        public static boolean randomrole = false;

        public ClientController(Socket clientSocket) {
            this.socket = clientSocket;
        }

        //mengirim string ke client
        void SendToClient(String msg) throws Exception {
            //create output stream attached to socket
            PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            //send msg to server
            outToClient.print(msg + '\n');
            outToClient.flush();
        }

        //mengirim ke client tentang fail response
        void FailResponse(String msg) throws Exception {
            String response;
            //build jsonObject
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", "fail");
            jsonObject.put("description", msg);

            //convert JSONObject to JSON to String
            response = jsonObject.toString();
            System.out.println("kirim : " + response);
            SendToClient(response);
        }

        //mengirim ke client tentang wrong response
        void WrongResponse() throws Exception {
            String response;
            //build jsonObject
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", "error");
            jsonObject.put("description", "Wrong Response");

            //convert JSONObject to JSON to String
            response = jsonObject.toString();
            System.out.println("kirim : " + response);
            SendToClient(response);
        }

        //parse request dari client
        void Parse(String msg) throws ParseException, Exception {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(msg);
            if (json.get("method") != null) {
                String method = (String) json.get("method");
                System.out.println(method);
                if (method.equals("join")) {
                    String username = (String) json.get("username");
                    if (checkuser(username) && !play) {
                        listPlayer.add(new Player(totalClient++, 1, (String) json.get("udp_address"), Integer.parseInt(json.get("udp_port").toString()), username));
                        totalPlayer++;
                        player_id = totalClient - 1;
                        String response;
                        //build jsonObject
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("status", "ok");
                        jsonObject.put("player_id", totalClient - 1);

                        //convert JSONObject to JSON to String
                        response = jsonObject.toString();
                        System.out.println("kirim : " + response);
                        SendToClient(response);
                    } else if (!checkuser(username)) {
                        FailResponse("User Exists");

                    } else if (checkid(player_id) > -1) {
                        FailResponse("you have joined");
                    } else if (play) {
                        FailResponse("please wait, game is currently running");
                    }
                } else if (method.equals("leave")) { // untuk command leave
                    if (player_id != -1) {//jika sudah termasuk dalam list
                        if (checkid(player_id) > -1) {
                            totalPlayer--;
                            listPlayer.remove(checkid(player_id));
                            player_id = -1;

                            String response;
                            //build jsonObject
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("status", "ok");

                            //convert JSONObject to JSON to String
                            response = jsonObject.toString();
                            System.out.println("kirim : " + response);
                            SendToClient(response);
                        }
                    } else {//jika belum
                        FailResponse("you haven't joined the game");
                    }
                } else if (method.equals("ready")) {//comand ready
                    if (!listPlayer.get(checkid(player_id)).getIsReady()) {//belum ready
                        String response;
                        //build jsonObject
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("status", "ok");
                        jsonObject.put("description", "waiting for other player to start");
                        //convert JSONObject to JSON to String
                        response = jsonObject.toString();
                        System.out.println("kirim : " + response);
                        SendToClient(response);
                    } else {//sudah ready
                        FailResponse("you have been ready");
                    }
                } else if (method.equals("client_address")) {
                    String response;
                    //build jsonObject
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("status", "ok");

                    //tempjason untuk array
                    JSONArray ja = new JSONArray();
                    for (int i = 0; i < listPlayer.size(); i++) {
                        JSONObject tempobject = new JSONObject();
                        tempobject.put("player_id", listPlayer.get(i).getPlayerId());
                        tempobject.put("is_alive", listPlayer.get(i).getIsAlive());
                        tempobject.put("address", listPlayer.get(i).getAddress());
                        tempobject.put("port", listPlayer.get(i).getPort());
                        tempobject.put("username", listPlayer.get(i).getUsername());
                        if (listPlayer.get(i).getIsAlive() == 0) {

                            tempobject.put("role", listPlayer.get(i).getRole());
                        }
                        ja.add(tempobject);
                    }

                    jsonObject.put("clients", ja);
                    jsonObject.put("description", "list of clients retrieved");

                    //convert JSONObject to JSON to String
                    response = jsonObject.toString();
                    System.out.println("kirim : " + response);
                    SendToClient(response);
                    StartGame();
                } else {// command tidak terdefinisi
                    WrongResponse();
                }
            }
        }

        //mengecek adakah id dalam list player
        int checkid(int id) {
            for (int i = 0; i < listPlayer.size(); i++) {
                if (listPlayer.get(i).getPlayerId() == id) {
                    return i;
                }
            }
            return -1;
        }

        //mengecek adakah username dalam list player
        boolean checkuser(String username) {
            for (int i = 0; i < listPlayer.size(); i++) {
                if (listPlayer.get(i).getUsername().equals(username)) {
                    return false;
                }
            }
            return true;
        }

        //prosedur untuk merandom role setiap pemain.
        public void RandomRole() {
            Random rand = new Random();
            boolean[] randomed = new boolean[listPlayer.size()];
            for (int i = 0; i < listPlayer.size(); i++) {
                randomed[i] = false;
            }
            for (int i = 0; i < listPlayer.size(); i++) {
                int nrand = rand.nextInt(listPlayer.size());
                while (randomed[nrand]) {
                    nrand--;
                    if (nrand < 0) {
                        nrand = listPlayer.size() - 1;
                    }
                }
                if (i < listPlayer.size() / 3) {
                    listPlayer.get(nrand).setRole("werewolf");
                } else {
                    listPlayer.get(nrand).setRole("civilian");
                }
                randomed[nrand] = true;
            }
            nwerewolf = listPlayer.size() / 3;
            ncivilian = listPlayer.size() - nwerewolf;
        }

        //METHOD UNTUK MENGIRIM PROTOCOL START GAME
        //DIPANGGIL KETIKA SEMUA SUDAH READY DAN PLAYER >= 6
        public void StartGame() throws Exception {
            String json;
            if (!randomrole) {
                randomrole = true;
                RandomRole();
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "start");
            play = true;
            jsonObject.put("time", "day");
            day = true;
            jsonObject.put("role", listPlayer.get(checkid(player_id)).getRole());
            if (listPlayer.get(checkid(player_id)).getRole().equals("werewolf")) {
                //tempjason untuk array
                JSONArray ja = new JSONArray();
                for (int i = 0; i < listPlayer.size(); i++) {
                    if (listPlayer.get(i).getRole().equals("werewolf") && i != checkid(player_id)) {
                        ja.add(listPlayer.get(i).getUsername());
                    }
                }

                jsonObject.put("friend", ja);
            }
            jsonObject.put("description", "game is started");
            json = jsonObject.toString();
            System.out.println(json);
            SendToClient(json);
        }

        //PROSEDUR UNTUK MENGGANTI TIME
        //GTAU KAPAN DIPANGGIL
        public void ChangePhase(String daystring, int numberday) throws Exception {
            String json;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "change_phase");
            play = true;
            jsonObject.put("time", daystring);
            if (daystring.equals("day")) {
                day = true;
            } else {
                day = false;
            }
            jsonObject.put("days", numberday);
            jsonObject.put("description", "Time has changed");
            json = jsonObject.toString();
            System.out.println(json);
            SendToClient(json);
        }

        public void CheckGameOver() throws Exception {
            String json;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "game_over");
            play = false;
            if(nwerewolf==0){
                jsonObject.put("winner", "civilian");
            } else if(nwerewolf == ncivilian){
                jsonObject.put("winner", "werewolf");
            }
            jsonObject.put("description", "game over");
            json = jsonObject.toString();
            System.out.println(json);
            SendToClient(json);

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

    //vote now
    public static void ParseCommand(String cmd, Socket socket) throws Exception {
        System.out.println("parse command");
        if (cmd.equals("vote_now")) {
            String json;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "vote_now");

            Scanner scan = new Scanner(System.in);
            System.out.print("masukan phase: ");
            String phase = scan.nextLine();
            jsonObject.put("phase", phase);

            //Send To Client
            String response;
            response = jsonObject.toString();
            System.out.println("kirim : " + response);
            PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            //send msg to client
            outToClient.print(response + '\n');
            outToClient.flush();
        }
    }
}
