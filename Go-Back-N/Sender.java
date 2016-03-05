import java.io.*;
import java.net.*;

public class Sender {
	
    public static void main(String[] args) throws IOException {
        
        InetAddress address = InetAddress.getByName(args[0]);
        int outPort = new Integer(args[1]).intValue();
        int inPort = new Integer(args[2]).intValue();
        String filename = args[3];
        byte[] seqMax = new byte[Integer.parseInt(args[4])];
        
        DatagramSocket socket = null;
        DatagramPacket packet = null;
        DatagramPacket ackPacket = null;
        FileInputStream finput = null;
        boolean fileExists;
		byte[] packetBuffer = new byte[125];
    	byte[] seqNum = new byte[1];
		byte[] fileBuffer = new byte[124];
		byte[] ack = new byte[1];
		int bytesRead = 0;
        
        socket = new DatagramSocket(inPort);
        socket.setSoTimeout(500); //Half Second Timeout
        
        try {
        	finput = new FileInputStream(filename);
        	fileExists = true;
        } catch (FileNotFoundException e) {
        	fileExists = false;
        }
        
        if (fileExists) {
            int winMax = seqMax.length;
            int winMin = 0;
    		while ((bytesRead = finput.read(fileBuffer)) != -1) { //Read and Send File Packets
                    while(seqNum[0] < winMax){
                        packetBuffer = concat(seqNum, fileBuffer);
                        packet = new DatagramPacket(packetBuffer, packetBuffer.length, address, outPort);
                        socket.send(packet);
                        seqNum[0] += 1; // Increment sequence number
                    }
    	        
                    while(ack[0] != 1) { //Wait for ACK packet
                            ack = receiveACK(socket, ack, ackPacket, packet);
                            // Shift the window right
                            winMax += 1;
                            winMin += 1;
                    }
                    System.out.println("ACK Received.");

                    ack = new byte[1]; //Clear ACK
                    fileBuffer = new byte[124]; //Clear Old Buffer Data
    		}
    		
    		seqNum[0] = (byte)-1; //Transmit Special EOT Packet with -1 Sequence Number
    		fileBuffer = new byte[124]; //Cleared Buffer Data
    		packetBuffer = concat(seqNum, fileBuffer);
	        packet = new DatagramPacket(packetBuffer, packetBuffer.length, address, outPort);
	        socket.send(packet);
	        
	        while(ack[0] != 1) { //Wait for ACK packet
	        	ack = receiveACK(socket, ack, ackPacket, packet);
	        }
    	    System.out.println("EOT ACK Received.");
	        
	    	finput.close();
        }
        socket.close();
    }
    
    public static byte[] receiveACK(DatagramSocket socket, byte[] ack, DatagramPacket ackPacket, DatagramPacket packet) {
        ackPacket = new DatagramPacket(ack, ack.length);
        
        try {
        	socket.receive(ackPacket);
        } catch (SocketTimeoutException ex) {
    	    System.out.println("Timeout, Resend");
        } catch (IOException ex) {
        	System.out.println("I/O Exception");
        }
        if (ack[0] != 1) { //Resend Packet if bad ACK or Timeout
        	try {
        		socket.send(packet);
        	} catch (IOException ex) {
	        	System.out.println("I/O Exception");
	        }
        }
        return ack;
    }
    
    public static byte[] concat(byte[] a, byte[] b) {
    	byte[] c = new byte[a.length + b.length];
    	System.arraycopy(a, 0, c, 0, a.length);
    	System.arraycopy(b, 0, c, a.length, b.length);
    	return c;
    }
}