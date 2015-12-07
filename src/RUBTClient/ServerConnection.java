package RUBTClient;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */

import java.util.ArrayList;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import GivenTools.TorrentInfo;

public class ServerConnection extends Thread{

	private Tracker tracker;
	private int pieceLength;
	private TorrentInfo torrentInfo;
	private boolean[] pieceDownloaded;
	private ArrayList<Integer> listPiecesDownloaded;
	private byte[] fileOut;
	private DataInputStream in;
	private DataOutputStream out;
	private Socket conn;

	public ServerConnection(Tracker tracker, Socket conn) {
		this.tracker = tracker;
		torrentInfo = tracker.torrentInfo;
		pieceLength = torrentInfo.piece_length;
		pieceDownloaded=Client.pieceDownloaded;
		listPiecesDownloaded=Client.listPiecesDownloaded;
		fileOut = Client.fileOut;
		this.conn = conn;

		try{
			out = new DataOutputStream(conn.getOutputStream());
			in =  new DataInputStream(conn.getInputStream());
			System.out.println("_____________________");
			System.out.println("Uploading to peer at port = "+conn.getPort());
			System.out.println("Peer's IP address = "+conn.getInetAddress());
		}
		catch(IOException e ){
			System.out.println("Could not get the input and output streams of a connection");
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * This method is responsible for handshaking, unchoking, and piece messages
	 * that are to be sent to the peer that is interested.
	 * 
	 */
	public void run() {
		byte[] handshake = null;
		byte[] length = new byte[4];
		int lenInt;
		
		// Read handshake from client
		try{
			lenInt = 68;
			handshake= new byte[lenInt];
			in.readFully(handshake);
		}
		catch(IOException e){
			System.out.println("Could not read from input stream.");
			e.printStackTrace();
		}

		// Verify handshake
		if(!Client.verifyHandshake(handshake)){
			System.out.println("Could not verify handshake.");
			try{
				conn.close();
			}
			catch(IOException e){
				System.out.println("Closing connection failed.");
				e.printStackTrace();
			}
			return;
		}
		System.out.println("Handshake verified for "+conn.getInetAddress());
		Message handshakeOut = new Message(tracker.peer_id.getBytes(), torrentInfo.info_hash.array());
		try {
			out.write(handshakeOut.message);
		} catch (IOException e2) {
			System.out.println("Could not send handshake message!");
			return;
		}
		// Send have message for each piece we have
		for (int i = 0; i < listPiecesDownloaded.size(); i++) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				Message havePiece = new Message((byte) 4, 5, listPiecesDownloaded.get(i), "-1".getBytes(), -1, -1, -1, -1, -1, "-1".getBytes());
				out.write(havePiece.message);
			}
			catch (IOException e) {
				System.out.println("Could not write to out stream.");
				return;
			}
		}
		System.out.println("Sent have messages to "+conn.getInetAddress());
		// Check if client sent interested message
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		byte[] interest = null;
			
		// Read the interested message from the peer.
		try {
			in.readFully(length);
			lenInt = Client.byteArrToInt(length); 
			interest = new byte[lenInt];
			in.readFully(interest);
			
		} catch (IOException e) {
			System.out.println("Could not read from input stream.");
			e.printStackTrace();
		}
			
			
		// Send unchoke message to client
		Message unchoke = new Message((byte)1, 1, -1, "-1".getBytes(), -1, -1, -1, -1, -1, "-1".getBytes());
		try{
			out.write(unchoke.message);
			out.flush();
		}
		catch(IOException e){
			System.out.println("Could not write to outstream.");
			e.printStackTrace();
		}

		// Read request messages
		byte[] request = null;
		try {
			while (!Client.userInput.equals("-1")) {
				in.readFully(length);
				lenInt = Client.byteArrToInt(length); 
				request = new byte[lenInt];
				in.readFully(request);
				ByteBuffer buffer = ByteBuffer.wrap(request);

				int id = buffer.get();

				if (id == 1) {
					// Client sent not interested
					break;
				} else if (id != 6) {
					// Client sent message other than request
					continue;
				} else {
					int index = buffer.getInt();
					int begin = buffer.getInt();
					int len = buffer.getInt();

					if (pieceDownloaded[index] && len < 32768) {
						// Send block to client
						byte[] block = new byte[len];
						System.arraycopy(fileOut, index*pieceLength+begin, block, 0, len);
						Message piece = new Message((byte)7, 9 + len, -1, "-1".getBytes(), -1, -1, -1, index, begin, block);
						out.write(piece.message);
						out.flush();
						System.out.println("Sent "+conn.getInetAddress()+" --> piece #"+index+" beginning at "+begin+" of length = "+len+".");
					}
				}
			}
	    	System.out.println("Server quit!");
		}
		catch (IOException e) {
			System.out.println("Could not read from input stream.");
			e.printStackTrace();
		}
		catch (Exception e) {
			System.out.println("The message received from the peer was not of the expected format.");
			e.printStackTrace();
		}

		try {
			conn.close();
		}
		catch (IOException e) {
			System.out.println("Could not close socket.");
			e.printStackTrace();
		}
	}
}
