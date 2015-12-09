package RUBTClient;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */

import java.io.*;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import GivenTools.*;
import RUBTClient.Tracker.Event;

public class Client implements Runnable{
	
	private static Tracker tracker;
	private static List<Peer> peers;
	private static TorrentInfo torrentInfo;
    private static String peer_id;
    public static byte[] fileOut;
    private static int pieceLength;
    private static int numPieces;
    private static int fileLength;
    private static int alreadyDownloaded;
    private static String fileName;
    private static RarestPiece rarestPiece;
    private static PeerManager peerManager;
    public static File fp;
    public static boolean[] pieceDownloaded;
    public static ArrayList<Integer> listPiecesDownloaded;
    public static String userInput = "";
   
    
    public Client (Tracker tracker, PeerManager peerManager, File fp, String fileName){
    	this.tracker = tracker;
    	this.peerManager = peerManager;
    	Client.torrentInfo = tracker.torrentInfo;
    	this.peers = tracker.peers;
    	Client.fp = fp;
		Client.fileOut = new byte[torrentInfo.file_length];
		Client.pieceLength = torrentInfo.piece_length;
		Client.fileLength = torrentInfo.file_length;
		Client.alreadyDownloaded = 0;
        numPieces = (int)Math.ceil((double)torrentInfo.file_length / (double)torrentInfo.piece_length);
        pieceDownloaded = new boolean[numPieces];
        Client.peer_id = tracker.peer_id;
        listPiecesDownloaded=new ArrayList<Integer>(numPieces);
        Client.fileName = fileName;
        Client.rarestPiece = new RarestPiece(tracker);
				
	}

    public void run(){
    	try {
			fetchFile(fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Download the file from a peer
     * @return the full file as a byte array
     * @throws Exception
     */
    public synchronized void fetchFile(String fileName) throws Exception {
    	
        // Select peer to connect to
        if (peerManager.downloadPeers == null)
        {
            throw new Exception("Could not connect to a peer!");
        } else if (peerManager.downloadPeers.size() == 1) {
        	
        	System.out.println("Only found one peer. Downloading from this peer only");
        	
            // Send HTTP GET to tracker to indicate download started
        	tracker.sendTrackerRequest(Event.STARTED);
         	long timeBegin = System.nanoTime();
        	downloadPiece(peerManager.downloadPeers.get(0));
        	long timeEnd = System.nanoTime();
        	System.out.println("Download time = "+(timeEnd-timeBegin)/1000000000+" seconds");
   
        	saveCompletedFileToDisk(fileName);
            // Send HTTP GET to tracker to indicate download is complete
        	tracker.sendTrackerRequest(Event.COMPLETED);
        }

        if (alreadyDownloaded == numPieces){
        	System.out.println("Already Downloaded!");
        	System.out.println("Download time = 0 seconds!");
        	File fileN = new File(fileName);
        	fp.renameTo(fileN);
        	return;
        }

        	// Send HTTP GET to tracker to indicate download started
        	tracker.sendTrackerRequest(Event.STARTED);
        	long timeBegin = System.nanoTime();
        	
        	ExecutorService threadPool = Executors.newFixedThreadPool(20);

        	for (int i = 0; i < peerManager.downloadPeers.size(); i++) {
        		final int j = i;
        	    threadPool.submit(
        	    		new Runnable() {public void run() { try {
        	    			downloadPiece(peerManager.downloadPeers.get(j));
        	    			peerManager.downloadPeers.get(j).startTimer();
						} catch (Exception e) {
							e.printStackTrace();
						} 
        	    		
        	    		}} );
        	
        	}
        	threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            
        	long timeEnd = System.nanoTime();
        	System.out.println("Download time = "+(timeEnd-timeBegin)/1000000000+" seconds");
        
        	if(userInput.equals("-1")){
                // Send HTTP GET to tracker to indicate download is stopped
            	tracker.sendTrackerRequest(Event.STOPPED);

        	} else {
        		saveCompletedFileToDisk(fileName);
                // Send HTTP GET to tracker to indicate download is complete
            	tracker.sendTrackerRequest(Event.COMPLETED);
            	waitForUserInput();
        	}
        

    }

    /**
     * 
     * Method responsible for downloading pieces from the peer specified.
     * This method is called for each peer we download from, and runs 
     * in parallel. 
     * 
     * @param peer, peer which we want to download pieces from
     * @throws Exception
     */
    private static void downloadPiece(Peer peer) throws Exception{

        try {
            // Create socket and connect to peer
            System.out.println("Connecting to peer to download from: " + peer.getIP());
            peer.connectPeer();

            
            // Create handshake
            Message handshake = new Message(peer_id.getBytes(), torrentInfo.info_hash.array());

            // Send handshake to peers
            System.out.println("Sending handshake to peer: "+peer.getIP());
            peer.out.write(handshake.message);
            peer.out.flush();

            // Receive handshake from peers
            byte[] handshakeResponse = new byte[68];
            
            peer.in.read(handshakeResponse);

            // Verify handshake
            if (!verifyHandshake(handshakeResponse)) {
                throw new Exception("Could not verify handshake from " + peer.getIP());
            }

            // Create interested message
            Message interested = new Message((byte) 2, 1, -1, "-1".getBytes(), -1, -1, -1,-1, -1, "-1".getBytes());

            @SuppressWarnings("unused")
			int length;
            int response_id;

            // Send interested message until peer unchokes
            for (int i = 0; i < 20; i++)
            {
            	peer.out.write(interested.message);
            	
                peer.out.flush();
                
                
                length = peer.in.readInt();
                response_id = (int) peer.in.readByte();
                
               
                if (response_id == 1)
                {
                    System.out.println("Peer at: "+peer.getIP()+" unchoked.");
                    break;
                }
                
                if(response_id == 5){
                	byte[] bitfield = new byte[length-1];
                	rarestPiece.bitfieldUpdate(bitfield);
                	peer.in.readFully(bitfield);
                }
                
                else
                {
                    if (i == 19)
                    {
                        throw new Exception("Peer not sending unchoke message");
                    }
                }
            }

            int i;
            int count = alreadyDownloaded;
            // Loop through each piece
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter -1 and enter to cancel download -->");
            

            while(count < numPieces && !userInput.equals("-1")){
            
            	if(bufIn.ready()){
            		userInput = bufIn.readLine();
            		if (userInput.equals("-1")){
            			break;
            		}
            	}
            	
            	i = findPieceToDownload();
            	
            	if (i == -1) return;
                
            	
            	if(i%50 == 0 && count!= 0){
            		System.out.println("Enter -1 and enter to cancel download -->");
            	}
            	
                int currentPieceLength;
                
                if (i == numPieces - 1)
                {
                    if (fileLength % pieceLength == 0)
                    {
                        currentPieceLength = pieceLength;
                    }
                    else
                    {
                        currentPieceLength = fileLength % pieceLength;
                    }
                }
                else
                {
                    currentPieceLength = pieceLength;
                }
                
                byte[] piece = new byte[currentPieceLength];
                
                piece = downloadPiece(peer,i);
                
                // Verify SHA-1 for piece
                if (verifyPiece(piece, i))
                {
                    System.out.println("Piece #"+i+" verified, downloaded from: "+peer.getIP());
                    pieceDownloaded[i] = true;
                    peer.incrementDownloaded(currentPieceLength);
                    listPiecesDownloaded.add(i);
                    System.arraycopy(piece, 0, fileOut, i * pieceLength, currentPieceLength);
                    updateSaveFile(piece,i);
                    count ++;
                }
                else
                {
                	System.out.println("Could not verify SHA-1 hash, or peer does not have piece");
                }   
            }
        }
        catch (EOFException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return;
        }
        catch (IOException e) {
            System.out.println("Couldn't connect to " + peer.getIP());
            Thread.currentThread().interrupt();
            return;
        }
        finally{
        }
    	
    }

    /**
     * Compare info_hash in peer response handshake to torrent
     * @param handshake peer response to handshake message
     * @return true if info_hash matches
     */
    public static boolean verifyHandshake(byte[] handshake)
    {
        byte[] info_hash = new byte[20];

        System.arraycopy(handshake, 28, info_hash, 0, 20);

        return (Arrays.equals(info_hash, torrentInfo.info_hash.array()));
    }

    /**
     * Compare piece SHA-1 to torrent
     * @param piece byte array of piece data
     * @param index index of piece
     * @return true if SHA-1 hashes match
     */
    private static boolean verifyPiece(byte[] piece, int index)
    {
        byte sha1Piece[] = new byte[20];
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            sha1Piece = md.digest(piece);
        } catch (NoSuchAlgorithmException e){
            System.err.println(e);
        }

        byte[] hashFromTor = torrentInfo.piece_hashes[index].array();

        return Arrays.equals(sha1Piece, hashFromTor);
    }
    
    /**
     * Check if the download has been previously started.
     * Load the pieces from .tmp file, first verify the SHA1 hash, and them to the 
     * byte array which contains the file's byte values. This byte array will eventually 
     * contain all the pieces (after the rest of the file downloads) and is saved to the
     * user's directory.
     * 
     */
    public void checkFileState(){
    	
    	if(fp.exists()){
    		try {
    			System.out.println("Existing download!");
    			FileInputStream in = new FileInputStream(fp);
				alreadyDownloaded = 0;
				int length;
				byte[] pieceData;
				int countChecked = 0;
				
				while(countChecked<numPieces){
					
						if (countChecked==435){
							length = fileLength%pieceLength;
						} else {
							length = pieceLength;
						}
						
						pieceData = new byte[length];
						in.read(pieceData);
						
						if (verifyPiece(pieceData,countChecked)){ 
							System.out.println("Verified SHA1 hash of piece at index = "+countChecked+" = "+verifyPiece(pieceData,countChecked));
							pieceDownloaded[countChecked] = true;
							listPiecesDownloaded.add(countChecked);
							System.arraycopy(pieceData, 0, fileOut, countChecked*length, pieceData.length);
							alreadyDownloaded++;
							tracker.downloaded += length;
							tracker.left -= length;

						} else {
							pieceDownloaded[countChecked] = false;	
						}
						
						countChecked++;

					}
				
				in.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
    	} else{
    		System.out.println("This is a new download.");
    	}
    	
    	
    }
    
    /**
     * This saves pieces, one by one, to the temporary file.
     * Then, it saves the piece into the file byte array.
     * Update the tracker download and left values.
     * 
     * @param piece, piece to save in the .tmp file and byte array
     * @param index, index of that piece
     */
    public static synchronized void updateSaveFile(byte[] piece, int index){
    	
    	try {
			FileOutputStream out = new FileOutputStream(fp, true);
	    	System.arraycopy(piece, 0, fileOut, index*pieceLength, piece.length);
	    	tracker.downloaded += piece.length;
	    	tracker.left -= piece.length;
	    	out.write(piece);
	    	out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
    
    /**
     * 
     * @return index of piece to download
     */
    private static synchronized int findPieceToDownload(){
    	
    	int j = rarestPiece.findNextPiece();
        if (j != -1) {
            System.out.println("The rarest piece not yet downloaded is at = " + j);
        }
    	return j;
    }
    
    /**
     * 
     * Downloads a specific piece from a peer, subpiece by subpiece.
     * 
     * @param peer, peer to download piece from
     * @param pieceIndex, index of the piece that we want
     * @return returns the byte[] of the piece
     */
    private static byte[] downloadPiece(Peer peer, int pieceIndex){
    	byte[] piece;
    	pieceLength = torrentInfo.piece_length;
        fileLength = torrentInfo.file_length;
        int currentPieceLength;
        int numBlocks;
        int blockLength = 16384;
        int currentBlockLength;
        int length;
        int response_id;

        if (pieceIndex == numPieces - 1)
        {
            if (fileLength % pieceLength == 0)
            {
                currentPieceLength = pieceLength;
            }
            else
            {
                currentPieceLength = fileLength % pieceLength;
            }
        }
        else
        {
            currentPieceLength = pieceLength;
        }

        piece = new byte[currentPieceLength];

        numBlocks = (int)Math.ceil((double)currentPieceLength / (double)blockLength);

        for (int j = 0; j < numBlocks; j++)
        {
            // Calculate block length if last block
            if (j == numBlocks - 1)
            {
                if (currentPieceLength % blockLength == 0)
                {
                    currentBlockLength = blockLength;
                }
                else
                {
                    currentBlockLength = currentPieceLength % blockLength;
                }
            }
            else
            {
                currentBlockLength = blockLength;
            }

            // Create request message
            Message request = new Message((byte) 6, 13, -1, "-1".getBytes(), pieceIndex, j * blockLength, currentBlockLength, -1, -1, "-1".getBytes());

            try {
            // Send request message to peer
            peer.out.write(request.message);
            peer.out.flush();

            length = peer.in.readInt() - 9;
            response_id = (int) peer.in.readByte();
            int index = peer.in.readInt();
            int begin = peer.in.readInt();

            // Copy block into piece byte array
            if ((response_id == 7) && (index == pieceIndex))
            {
                byte[] block = new byte[length];
                peer.in.readFully(block);
                System.arraycopy(block, 0, piece, begin, length);
            }
            }
            catch (Exception e){
            	System.out.println(e);
            }
        }
    	
    	return piece;
    	
    }
    
    /**
     * 
     * This is executed when all the pieces are done downloading. This saves
     * the file to the directory and deletes the temporary file. 
     * 
     * @param fileName, what we want to name the file
     * 
     */
    private static synchronized void saveCompletedFileToDisk(String fileName){
    	
    	 try{
             FileOutputStream fileOutStream = new FileOutputStream(new File(fileName));
             fileOutStream.write(fileOut);
             fileOutStream.close();
             fp.delete();
         } catch (Exception e){
             System.out.println("Error writing file to hard disk. "+e);
         }
    	
    }
    
    private static void waitForUserInput(){
    	
    	BufferedReader bufIn = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter -1 and enter to stop. Download is now complete! -->");
        
        while(!userInput.equals("-1")){
        	try {
				if(bufIn.ready()){
					userInput = bufIn.readLine();
					if (userInput.equals("-1")){
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    	System.out.println("Download client quit!");
    }
    
    /**
     * 
     * Helper method to convert a byte[] to its integer representation.
     * 
     * @param byteArr, the byte array
     * @return integer representation of byte array
     */
    public static int byteArrToInt(byte[] byteArr){
    	return ByteBuffer.wrap(byteArr).getInt();
    }
}
