package RUBTClient;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */

import java.net.ServerSocket;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.net.Socket;

/** The server needs to be able to send have messages to all connected peers whenever a new piece gets verified.  I created an arraylist that contains a list
 * of verified pieces, and an int to keep track of the last piece downloaded.  periodically, check to see if the last element is different then the recorded last element
 * if it is, send have messages for all pieces between the index of previous last, and the index of current length-1.
 */
public class Server extends Thread implements Runnable{

	private Tracker tracker;
	private int port;
    private static ServerSocket serveSocket;
    private PeerManager pm;
    private final ArrayList<Socket> peerSockets;
    
	public Server(Tracker tracker, PeerManager pm){
		this.tracker = tracker;
		this.pm = pm;
		port=tracker.port;
		this.peerSockets = new ArrayList<Socket>();
	}
	
	public void run(){
		
		try{
			serveSocket= new ServerSocket(port);
			System.out.println("Server socket opened at port = "+port);
		}
		catch(IOException e){
			System.out.println("Could not listen on port = "+port);
			return;
		}
		
		Executor pool = Executors.newFixedThreadPool(6);
		
		int ind=0;
		while(!Client.userInput.equals("-1")){

			try{
				peerSockets.add(ind,serveSocket.accept());
			}catch(IOException e){
				e.printStackTrace();
				System.out.println("Something went wrong with setting up a peer connection with server.");
			}
			
			if (peerSockets.size() > 6){ 
				System.out.println("The maximum number of peers to upload to has been reached.");
				continue;
			}
			
			
			final Socket peerSocket = peerSockets.get(ind);
			final ServerConnection s = new ServerConnection(tracker, peerSockets.get(ind));
			
			if (numberOfUnchokedPeers() < 3){
				Runnable r = new Runnable()
				{
					@Override
					public void run()
					{
						peerSocket.getInetAddress();
						s.run();
					}
				};
				pool.execute(r);
			}
			ind++;
		}
    	System.out.println("Server quit!");
	}
	
	public int numberOfUnchokedPeers(){
		
		int numberUnchokedPeers = 0;
		
		for (int j = 0; j < peerSockets.size(); j++){
			Peer peerFromConnectedTo = pm.containsDownloadPeer(peerSockets.get(j).getInetAddress().toString());
			if(peerFromConnectedTo != null){
				if(!peerFromConnectedTo.getIsPeerChoked()) numberUnchokedPeers ++;
			}
		}
		
		return numberUnchokedPeers;
	}
	
}
