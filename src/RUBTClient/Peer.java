package RUBTClient;

import java.io.*;
import java.net.Socket;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */

public class Peer
{

    private String ip;
    private byte[] peerID;
    private int port;
    private long startTime;
    private long endTime;
    private int amountDownloadedFrom;
    private boolean isChoked;

    private boolean isPeerConnected;
    public Socket peerSocket = null;
    public DataInputStream in;
    public DataOutputStream out;

    /**
     * Peer Constructor
     * @param port - port number of peer
     * @param ip - IP address of peer
     * @param peerID - byte array of peerID
     *
     */
    public Peer(String ip, byte[] peerID, int port)
    {
        this.ip = ip;
        this.port = port;
        this.peerID = peerID;
    }

    public void connectPeer() throws IOException
    {
        peerSocket = new Socket(ip, port);
        out = new DataOutputStream(peerSocket.getOutputStream());
        in =  new DataInputStream(peerSocket.getInputStream());
        amountDownloadedFrom = 0;
        isChoked = false;
        isPeerConnected = true;
    }

    public void disconnectPeer()
    {
        if (peerSocket!=null)
        {
            try
            {
            	isPeerConnected = false;
                peerSocket.close();
            }
            catch (IOException e)
            {
                System.out.println("Error closing socket: "+e);
            }
        }
    }

    public byte[] generateHandshake(byte[] peerID, byte[] info_hash)
    {
        byte[] handshake = new byte[68];
        byte[] bitTorrentByteArr = "BitTorrent protocol".getBytes();
        byte[] zeroArr = new byte[8];
        int currentIndex = 0;

        // First part of handshake 19!
        handshake[currentIndex] = 19;
        currentIndex++;

        // Next part of handshake 'BitTorrent protocol'
        System.arraycopy(bitTorrentByteArr, 0, handshake, currentIndex, bitTorrentByteArr.length);
        currentIndex = currentIndex + bitTorrentByteArr.length;

        // Now zero array;
        System.arraycopy(zeroArr, 0, handshake, currentIndex, zeroArr.length);
        currentIndex = currentIndex + zeroArr.length;

        // Next SHA-1 Hash
        System.arraycopy(info_hash, 0, handshake, currentIndex, info_hash.length);
        currentIndex = currentIndex + info_hash.length;

        // Finally the peer ID.
        System.arraycopy(peerID, 0, handshake, currentIndex, peerID.length);

        return handshake;
    }

    //______________GET  METHODS______________\\

    public String getIP()
    {
        return ip;
    }

    public byte[] getPeerID()
    {
        return peerID;
    }

    public int getPortNumber()
    {
        return port;
    }

    public boolean getIsPeerConnected()
    {
        return isPeerConnected;
    }
    
    public boolean getIsPeerChoked () {
    	return isChoked;
    }
    
    public void startTimer(){
    	startTime = System.nanoTime();
    }
    
    public long elapsedTime(){
    	long currentTime = System.nanoTime();
    	return startTime - currentTime;
    }
    
    public void chokePeer(){
    	isChoked = true;
    	amountDownloadedFrom = 0;
    }
    
    public void unchokePeer(){
    	isChoked = false;
    }
    
    public void incrementDownloaded(int amount) {
    	amountDownloadedFrom += amount;
    }
    
    public double downloadRate(){
    	
    	long elapsedTime = elapsedTime();
    	double rate = amountDownloadedFrom/elapsedTime;
    	
    	return rate;
    }

    
}
