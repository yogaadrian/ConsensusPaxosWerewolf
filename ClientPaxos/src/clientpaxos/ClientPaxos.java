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
    
}
