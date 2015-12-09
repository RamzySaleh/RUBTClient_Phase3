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
		
		int maxConnections = 3;
		peersConnectedTo = new ArrayList<Peer>();

		downloadPeers = new ArrayList<>();

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
		
	}
	
	
	// Method responsible for keepAlive messages
	public void keepAlive() {
		long startTime = System.nanoTime();
		long currentTime;
		int i = 0;
		byte[] keepAlive = new byte[4];
		keepAlive = Message.intToByteArr(0);
		while(!Client.userInput.equals("-1")){
			currentTime = System.nanoTime();
  			long interval = i*120*(long)Math.pow(10, 9);
			if(currentTime-startTime>interval){
				System.out.println("KEEP ALIVE!");
				for (int j = 0; j<downloadPeers.size(); j++){
					try {
						if (downloadPeers.get(j) != null) {
							downloadPeers.get(j).out.write(keepAlive);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				i++;
			}
		}
		System.out.println("Keep-alive quit!");
	}
	
	// Finds peers to download from. 
	public void findPeersDownload(){
		
		if (peers == null) System.out.println("Peers is null!");

		for (int i = 0; i < peers.size(); i++)
		{
			String currentPeerIP = peers.get(i).getIP();
            if (currentPeerIP.equals("128.6.171.130") || currentPeerIP.equals("128.6.171.131"))
            {
            	downloadPeers.add(peers.get(i));                    
            }           
		}

	}
	
	// Returns the peer object of the corresponding IP address
	public Peer containsDownloadPeer(String ip){
		
		for (int j = 0; j < downloadPeers.size(); j++){
			
			if(downloadPeers.get(j).getIP() == ip){
				return downloadPeers.get(j);
			}
			
		}
		return null;
	}
}
