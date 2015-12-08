package RUBTClient;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */

import GivenTools.*;
import java.util.ArrayList;

public class RarestPiece {
	
	private class Piece {
		int pieceIndex;
		int rarityValue;
		boolean done;
		
		private Piece(int pieceIndex, int rarityValue){
			this.pieceIndex = pieceIndex;
			this.rarityValue = rarityValue;
			done = false;
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
		initializeRarity();
	}
	
	private void initializeRarity(){
		Piece p;
		for(int i = 0; i<numPieces; i++){
			p = new Piece(i, 0);
			ordered.add(p);
		}
	}
	
	private void indexIncrement(int index){
		
		int j = 0;
		if (index>numPieces){ System.out.println("Tried to update piece index that is greater than number of pieces"); }
		
		while(j < ordered.size()){
			if (ordered.get(j).pieceIndex == index){
				ordered.get(j).rarityValue ++;
				break;
			}
			j++;
		}
		sort();
		
	}
	
	private void indexSet(int index, int rarityVal){
		
		int j = 0;
		if (index>numPieces){ System.out.println("Tried to update piece index that is greater than number of pieces"); }
		
		while(j < ordered.size()){
			if (ordered.get(j).pieceIndex == index){
				ordered.get(j).rarityValue = rarityVal;
				break;
			}
			j++;
		}
		sort();
		
	}
	
	private void sort(){
		
		int count = 0;
		int currentRarityVal = 0;
		ArrayList<Piece> temp = new ArrayList<Piece>();
		
		while(count < ordered.size()){	
			for(int j = 0; j < ordered.size(); j++){	
				if(ordered.get(j).rarityValue == currentRarityVal){
					temp.add(ordered.get(j));
					count++;
				}	
			}
			currentRarityVal ++;
		}
		ordered = temp;
		
	}
	
	public int findNextPiece(){
		
		int j = 0;

		while(j < ordered.size()){
			if (ordered.get(j).done == false){
				ordered.get(j).done = true;
				return ordered.get(j).pieceIndex;
			}
		}
		return -1;	
	}
	
	public void bitfieldUpdate(byte[] bitfield){
		
		for (int j = 0; j < bitfield.length; j++){
			if (bitfield[j] != 0){
				indexIncrement(j);
			}
		}
		
	}
	
}
