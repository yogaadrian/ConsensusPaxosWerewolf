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
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yoga
 */
public class ClientPaxos {

    static public Socket clientSocket;
    static public BufferedReader objectFromServer;
    static public PrintWriter objectToServer;
    static public Scanner scan;

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
        clientSocket = new Socket(host, 2000);
        Thread t = new Thread(new StringGetter());
        t.start();
        while (true) {
            sleep(100);
            System.out.print("COMMAND : ");
            //send msg to server
            String msg = scan.nextLine();
            sendToServer(msg);
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
                }
            } catch (IOException ex) {
                Logger.getLogger(ClientPaxos.class.getName()).log(Level.SEVERE, null, ex);
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
    
    public static void sendMessage(String IP, int targetPort) throws Exception {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        InetAddress IPAddress = InetAddress.getByName(IP);

	DatagramSocket datagramSocket = new DatagramSocket();
	UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);
	
        while (true) {
            String sentence = inFromUser.readLine();
            if (sentence.equals("quit")) {
                break;
            }

            byte[] sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
            unreliableSender.send(sendPacket);
        }
        datagramSocket.close();
    }
    
    public static void receiveMessage(int listenPort) throws Exception {
	DatagramSocket serverSocket = new DatagramSocket(listenPort);

	byte[] receiveData = new byte[1024];
	while(true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("RECEIVED: " + sentence);
        }
    }
    
}
