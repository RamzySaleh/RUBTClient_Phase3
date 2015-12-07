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
    }

    public void disconnectPeer()
    {
        if (peerSocket!=null)
        {
            try
            {
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

}
