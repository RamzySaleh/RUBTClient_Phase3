package RUBTClient;

import GivenTools.*;
import java.util.ArrayList;

public class RarestPiece {
	
	private class Piece {
		int pieceIndex;
		int rarityValue;
		
		private Piece(int pieceIndex, int rarityValue){
			this.pieceIndex = pieceIndex;
			this.rarityValue = rarityValue;
		}
	}

	private ArrayList<Piece> ordered;
	private Tracker tracker;
	private int numPieces;
	private TorrentInfo torrentInfo;
	
	public RarestPiece(Tracker tracker){
		this.tracker = tracker;
		ordered = new ArrayList<Piece>();
		this.torrentInfo = tracker.torrentInfo;
		numPieces = (int)Math.ceil((double)torrentInfo.file_length / (double)torrentInfo.piece_length);
	}
	
	private void initializeRarity(){
		
		for(int i = 0; i<numPieces; i++){
			
		}
	}
	
}
