package RUBTClient;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import GivenTools.*;
import RUBTClient.Tracker.Event;

public class PeerManager {

	
	private TorrentInfo torrentInfo;
	public ArrayList<Peer> downloadPeers;
	public ArrayList<Peer> uploadPeers;
	private List<Peer> peers;
	private Tracker tracker;
	public ArrayList<Peer> peersConnectedTo;
	
	public PeerManager(Tracker tracker){
		this.tracker = tracker;
		this.torrentInfo = tracker.torrentInfo;	
    	this.peers = tracker.peers;

		ConnectionManager();
	}
	
	
	public void ConnectionManager(){
		
		int maxConnections = 15;
		peersConnectedTo = new ArrayList<Peer>();
		
		System.out.println("The number of peers is = "+peers.size());
		
		int peerConnectedCount = 0;
		
		while(peerConnectedCount < maxConnections && peerConnectedCount < peers.size()){
			
			peersConnectedTo.add((peers.get(peerConnectedCount)));
			try {
				peers.get(peerConnectedCount).connectPeer();
				peerConnectedCount ++;
			} catch (IOException e) {
				System.out.println("Could not connect to Peer at IP = "+peers.get(peerConnectedCount).getIP());
			}
			
		}	
		
		findPeersDownload();
		
		/**
		 * 
		 * Keep Alive
		 * 
		 */
		long startTime = System.nanoTime();
		long currentTime;
		int i = 0;
		byte[] keepAlive = new byte[4];
		keepAlive = Message.intToByteArr(0);
		while(!Client.userInput.equals("-1")){
			currentTime = System.nanoTime();
			if(startTime-currentTime>i*(120*1000)){
				for (int j = 0; j<peersConnectedTo.size(); j++){
					try {
						peersConnectedTo.get(j).out.write(keepAlive);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			i++;
		}
	}
	
	
	public void findPeersDownload(){
		
		if (peers == null) System.out.println("Peers is null!");
		int addedCount = 0;
		for (int i = 0; i < peers.size(); i++)
		{
			String currentPeerIP = peers.get(i).getIP();
            if (currentPeerIP.equals("128.6.171.130") || currentPeerIP.equals("128.6.171.131"))
            {
            	addedCount++;
            	downloadPeers.add(peers.get(i));                    
            }           
		}
		
		for (int i = 0; i < peers.size(); i++)
		{
			String currentPeerIP = peers.get(i).getIP();
            if (!currentPeerIP.equals("128.6.171.130") || !currentPeerIP.equals("128.6.171.131"))
            {
            	addedCount++;
            	if (addedCount > 6) break;
            	downloadPeers.add(peers.get(i));  
            }           
		}
		
	}
	
	public Peer containsDownloadPeer(String ip){
		
		for (int j = 0; j < downloadPeers.size(); j++){
			
			if(downloadPeers.get(j).getIP() == ip){
				return downloadPeers.get(j);
			}
			
		}
		return null;
	}
}
