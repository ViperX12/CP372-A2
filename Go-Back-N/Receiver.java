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
                byte[] lastAckSeqNum = new byte[1];
                byte[] resendSeqNum = new byte[1];
                lastAckSeqNum[0] = -1;
		byte[] fileBuffer = new byte[124];
		byte[] ack = new byte[1];
		ack[0] = (byte)1; //Set ACK to 1
		int packetcount = 0;
		boolean lostpacket = false;
		
        socket = new DatagramSocket(inPort);
		
		final long startTime = System.currentTimeMillis(); //Timer Start
		while(true) {
		    packet = new DatagramPacket(packetBuffer, packetBuffer.length);
		    socket.receive(packet);
		    System.arraycopy(packetBuffer, 0, seqNum, 0, seqNum.length); //Separate out Sequence Number
		    System.arraycopy(packetBuffer, 1, fileBuffer, 0, fileBuffer.length); //Separate out File Buffer
		    packetcount += 1;
		    System.out.println(seqNum[0]);
                    System.out.println(lastAckSeqNum[0]);
		    if (reliability != 0) lostpacket  = packetcount % reliability == 0;
		    if (seqNum[0] != -1 && !lostpacket) {
		    	if (seqNum[0] == lastAckSeqNum[0] + 1) { //Write to File if Correct Sequence Number
		    		foutput.write(fileBuffer);
		    		lastAckSeqNum[0] = seqNum[0];
                                packet = new DatagramPacket(seqNum, seqNum.length, address, outPort);
                                socket.send(packet);
                                System.out.println("ACK sent.");
		    	}
                        else {
                            resendSeqNum[0] = (byte) (lastAckSeqNum[0] + 1);
                            packet = new DatagramPacket(resendSeqNum, resendSeqNum.length, address, outPort);
                            socket.send(packet);
                            System.out.println("Packet out of order. Dropped and resending ACK #" + resendSeqNum[0] + ".");
                        }
		    } else if (lostpacket) {
		    	System.out.println("Packet lost.");
		    } else {
	    		packet = new DatagramPacket(ack, ack.length, address, outPort);
	    		socket.send(packet);
	    		System.out.println("EOT ACK sent.");
		    	break;
		    }
	    }
		final long endTime = System.currentTimeMillis(); //Timer End
		System.out.println("Total Transmission Time: " + (endTime - startTime));
    	
		foutput.close();
        socket.close();
    }
}