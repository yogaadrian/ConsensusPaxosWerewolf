package clientpaxos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author yoga
 */
public class UnreliableSender {
    private DatagramSocket datagramSocket;
    private Random random;

	public UnreliableSender(DatagramSocket datagramSocket) throws SocketException {
		this.datagramSocket = datagramSocket;
		random = new Random();
	}

	public void send(DatagramPacket packet) throws IOException {
		double rand = random.nextDouble();
		if (rand < 0.85) {
			datagramSocket.send(packet);
		}
	}
    
}
