import java.io.*;
import java.net.*;

public class Sender {
	
    public static void main(String[] args) throws IOException {

        assert Integer.parseInt(args[4]) <= 128 : "Window must be <= 128";
    	
        InetAddress address = InetAddress.getByName(args[0]);
        int outPort = new Integer(args[1]).intValue();
        int inPort = new Integer(args[2]).intValue();
        String filename = args[3];
        int windowSize = Integer.parseInt(args[4]);
        
        DatagramSocket socket = null;
        DatagramPacket sentPacket = null;
        FileInputStream finput = null;
        boolean fileExists;
        boolean fileReading = true;
    	byte[] seqNum = new byte[1];
		byte[] fileBuffer = new byte[124];
		byte[] ack = new byte[1];
		ack[0] = (byte)-1; //Set Default Ack to -1
		int bytesRead = 0;
		int timeout = 50;
		
		DatagramPacket[] windowPackets = new DatagramPacket[windowSize];
		int sendBase = 0;
        
        socket = new DatagramSocket(inPort);
        socket.setSoTimeout(timeout);
        
        try {
        	finput = new FileInputStream(filename);
        	fileExists = true;
        } catch (FileNotFoundException e) {
        	fileExists = false;
        }
        
        //Note: Need to Accommodate Sequence Number Rollover
        if (fileExists) {
            while((seqNum[0] - sendBase) < windowSize) { //Read and Send Initial Window Packets
            	if((bytesRead = finput.read(fileBuffer)) == -1) {
            		fileReading = false;
            		System.out.println("File Done");
            		break;
            	}
            	
            	sentPacket = sendPacket(socket,address,outPort,seqNum,fileBuffer);
                windowPackets[seqNum[0] - sendBase] = sentPacket; 
                seqNum[0] = (byte) (seqNum[0] +  1); // Increment sequence number
                
                System.out.println("Sent Packet " + (seqNum[0] - 1));
                fileBuffer = new byte[124]; //Clear Old Buffer Data
            }
    		while (fileReading) { //Read and Send More Packets as ACKs come
                ack = receiveACK(socket, ack);
                if (ack[0] != sendBase || ack[0] == -1) {
                	System.out.println("Received: " + ack[0] + " - Resending Packets");
                	resendWindow(socket,windowPackets);
                } else {
                	System.out.println("Received: " + ack[0] + " - Packet ACKed, Window Shifted");
                	sendBase += 1;
                	
                	if((bytesRead = finput.read(fileBuffer)) == -1) {
                		fileReading = false;
                		System.out.println("File Done");
                		break;
                	}
                	
                	sentPacket = sendPacket(socket,address,outPort,seqNum,fileBuffer);
                	System.arraycopy(windowPackets, 1, windowPackets, 0, windowSize-1); //Shift Window Packets Left
                    windowPackets[windowSize-1] = sentPacket;
                    
                    seqNum[0] = (byte) (seqNum[0] +  1);
                    System.out.println("Sent Packet " + (seqNum[0] - 1));
                    if(seqNum[0] < 0) { System.exit(0); }
                }
                
                ack[0] = (byte)-1; //Set Default Ack
                fileBuffer = new byte[124]; //Clear Old Buffer Data
    		}
    		
    		while((seqNum[0] - sendBase) != 0) { //Get ACKs for last Packets in Window
    			ack = receiveACK(socket, ack);
    			if (ack[0] != sendBase || ack[0] == -1) {
    				System.out.println("Received: " + ack[0] + " - Resending Packets");
                	resendWindow(socket,windowPackets);
    			} else {
    				System.out.println("Received: " + ack[0] + " - Packet ACKed");
    				sendBase += 1;
    			}
    			ack[0] = (byte)-1; //Set Default Ack
    		}
	        
	        System.out.println("Waiting for EOT ACK");
	        
    		seqNum[0] = (byte)-2; //Transmit Special EOT Packet with -2 Sequence Number
    		fileBuffer = new byte[124]; //Cleared Buffer Data
	        while (ack[0] != -2) {
	    		sendPacket(socket,address,outPort,seqNum,fileBuffer);
	        	ack = receiveACK(socket, ack);
	        }
	        
    	    System.out.println("EOT ACK Received.");
	        
	    	finput.close();
        }
        socket.close();
    }
    
    public static DatagramPacket sendPacket(DatagramSocket socket, InetAddress address, int outPort, byte[] seqNum, byte[] fileBuffer) {
        DatagramPacket packet = null;
		byte[] packetBuffer = new byte[125];
        
        packetBuffer = concat(seqNum, fileBuffer);
        packet = new DatagramPacket(packetBuffer, packetBuffer.length, address, outPort);
        try {
			socket.send(packet);
		} catch (IOException e) {
        	System.out.println("I/O Exception");
		}
        return packet;
    }
    
    
    public static byte[] receiveACK(DatagramSocket socket, byte[] ack) {
        DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
        
        try {
        	socket.receive(ackPacket);
        } catch (SocketTimeoutException ex) {
    	    System.out.println("Timeout");
        } catch (IOException ex) {
        	System.out.println("I/O Exception");
        }        
        return ack;
    }
    
    public static void resendWindow(DatagramSocket socket, DatagramPacket[] windowPackets) {
    	for (DatagramPacket packet : windowPackets) {
    		try {
    			if (packet != null) {
    				socket.send(packet);
    			}
			} catch (IOException e) {
				System.out.println("I/O Exception");
			}
    	}
    }
    
    public static byte[] concat(byte[] a, byte[] b) {
    	byte[] c = new byte[a.length + b.length];
    	System.arraycopy(a, 0, c, 0, a.length);
    	System.arraycopy(b, 0, c, a.length, b.length);
    	return c;
    }
}