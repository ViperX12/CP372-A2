import java.io.*;
import java.net.*;

public class Receiver {
	
    public static void main(String[] args) throws IOException {
        
        InetAddress address = InetAddress.getByName(args[0]);
        int outPort = new Integer(args[1]).intValue();
        int inPort = new Integer(args[2]).intValue();
        int reliability = new Integer(args[3]).intValue();
        String filename = args[4];
        
        DatagramSocket socket = null;
        DatagramPacket packet = null;
		FileOutputStream foutput = new FileOutputStream(filename);
		byte[] packetBuffer = new byte[125];
		byte[] seqNum = new byte[1];
		
        byte[] expectedSeqNum = new byte[1];
        byte[] lastSeqNum = new byte[1];
        
		byte[] fileBuffer = new byte[124];
		int packetcount = 0;
		boolean lostpacket = false;
		
        socket = new DatagramSocket(inPort);
		
        long startTime = 0;
		while(true) {
			System.out.println("Waiting for Packet.");
		    packet = new DatagramPacket(packetBuffer, packetBuffer.length);
		    socket.receive(packet);
		    if (packetcount == 0) { startTime = System.currentTimeMillis(); } //Timer Start
		    System.arraycopy(packetBuffer, 0, seqNum, 0, seqNum.length); //Separate out Sequence Number
		    System.arraycopy(packetBuffer, 1, fileBuffer, 0, fileBuffer.length); //Separate out File Buffer
		    packetcount += 1;
	
            System.out.println("Expected " + expectedSeqNum[0]);
		    System.out.println("Received " + seqNum[0]);
             
		    if (reliability != 0) lostpacket  = packetcount % reliability == 0;
		    if (seqNum[0] != -2 && !lostpacket) {
		    	if (seqNum[0] == expectedSeqNum[0]) { //Write to File if Correct Sequence Number
		    		foutput.write(fileBuffer);
                    packet = new DatagramPacket(seqNum, seqNum.length, address, outPort);
                    socket.send(packet);
                    System.out.println("ACK sent.");
                    expectedSeqNum[0] = (byte) (expectedSeqNum[0] +  1);
		    	} else {
		    		lastSeqNum[0] = (byte) (expectedSeqNum[0] - 1);
                    packet = new DatagramPacket(lastSeqNum, lastSeqNum.length, address, outPort);
                    socket.send(packet);
                    System.out.println("Packet out of order. Dropped and resending ACK " + lastSeqNum[0] + ".");
                }
		    } else if (lostpacket) {
		    	System.out.println("(Packet lost)");
		    } else {
		    	//EOT ACK, special seqNum sent is -2, so send back
	    		packet = new DatagramPacket(seqNum, seqNum.length, address, outPort);
	    		socket.send(packet);
	    		System.out.println("EOT ACK sent.");
		    	break;
		    }
	    }
		long endTime = System.currentTimeMillis(); //Timer End
		System.out.println("Total Transmission Time: " + (endTime - startTime));
    	
		foutput.close();
        socket.close();
    }
}