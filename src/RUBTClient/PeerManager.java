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

public class PeerManager {

	
	private TorrentInfo torrentInfo;
	private ArrayList<Peer> downloadPeers;
	private ArrayList<Peer> uploadPeers;
	private List<Peer> peers;
	private Tracker tracker;
	
	public PeerManager(Tracker tracker){
		this.tracker = tracker;
		this.torrentInfo = tracker.torrentInfo;	
    	this.peers = tracker.peers;

	}
	
	
	public void ConnectionManager(){
		
		int maxConnections = 15;
		
		System.out.println("The number of peers is = "+peers.size());
		
		int peerConnectedCount = 0;
		
		while(peerConnectedCount < maxConnections && peerConnectedCount < peers.size()){
			
			try {
				peers.get(peerConnectedCount).connectPeer();
			} catch (IOException e) {
				System.out.println("Could not connect to Peer at IP = "+peers.get(peerConnectedCount).getIP());
			}
			
		}	
	}
	
	
	public List<Peer> findPeersDownload(){
		
		List<Peer> peersToDownload = new LinkedList<Peer>();
		if (peers == null) System.out.println("Peers is null!");
		for (int i = 0; i < peers.size(); i++)
		{
			String currentPeerIP = peers.get(i).getIP();
            if (currentPeerIP.equals("128.6.171.130") || currentPeerIP.equals("128.6.171.131"))
            {
            	peersToDownload.add(peers.get(i));                    
            }           
		}

		return peersToDownload;
	}
	
}
