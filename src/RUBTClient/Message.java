package RUBTClient;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */


public class Message {

	private final static int choke_ID = 0;
	private final static int unchoke_ID = 1;
	private final static int interested_ID = 2;
	private final static int uninterested_ID = 3;
	private final static int have_ID = 4;
	private final static int bitfield_ID = 5;
	private final static int request_ID = 6;
	private final static int piece_ID = 7;

	public byte[] message;
	
	/**
	 * Constructor for handshake message.
	 * 
	 * @param peerID
	 * @param info_hash
	 * 
	 */
	public Message(byte[] peerID, byte[] info_hash)
    {
        message = new byte[68];
        byte[] bitTorrentByteArr = "BitTorrent protocol".getBytes();
        byte[] zeroArr = new byte[8];
        int currentIndex = 0;

        // First part of handshake 19!
        message[currentIndex] = 19;
        currentIndex++;

        // Next part of handshake 'BitTorrent protocol'
        System.arraycopy(bitTorrentByteArr, 0, message, currentIndex, bitTorrentByteArr.length);
        currentIndex = currentIndex + bitTorrentByteArr.length;

        // Now zero array;
        System.arraycopy(zeroArr, 0, message, currentIndex, zeroArr.length);
        currentIndex = currentIndex + zeroArr.length;

        // Next SHA-1 Hash
        System.arraycopy(info_hash, 0, message, currentIndex, info_hash.length);
        currentIndex = currentIndex + info_hash.length;

        // Finally the peer ID.
        System.arraycopy(peerID, 0, message, currentIndex, peerID.length);

    }
	
	/**
	 * 
	 * Constructor for all other messages.
	 * 
	 * Values of parameters that don't apply to the desired message can
	 * be anything (they are not used). For example, if we wanted to send
	 * a 'have' message, we would worry about haveIndex. But, we don't worry 
	 * about the value of pieceIndex, for example, as it is not used.
	 * 
	 * These two parameters apply to all messages.
	 * @param messageID - message ID
	 * @param lengthPrefix - length prefix
	 * 
	 * have:
	 * @param haveIndex - zero-based index of the piece that has just been downloaded and verified
	 * 
	 * request:
	 * @param requestIndex - is an integer specifying the zero-based piece index
	 * @param requestBegin - is an integer specifying the zero-based byte offset within the piece
	 * @param requestLength - is the integer specifying the requested length (typically 2^14)
	 * 
	 * piece:
	 * @param pieceIndex - is an integer specifying the zero-based piece index
	 * @param pieceBegin - is an integer specifying the zero-based byte offset within the piece,
	 * @param pieceBlock - which is a block of data, and is a subset of the piece specified by index
	 * 
	 * 
	 */
	public Message(byte messageID, int lengthPrefix, 
					int haveIndex,
					byte[] bitfield, 
					int requestIndex, int requestBegin, int requestLength,
					int pieceIndex, int pieceBegin, byte[] pieceBlock){

		int id = messageID;
		
		switch(id){
		case(choke_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(1), 0, message, 0, 4);
			message[4] = (byte) 0;
			break;
		case(unchoke_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(1), 0, message, 0, 4);
			message[4] = (byte) 1;
			break;
		case(interested_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(1), 0, message, 0, 4);
			message[4] = (byte) 2;
			break;
		case(uninterested_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(1), 0, message, 0, 4);
			message[4] = (byte) 3;
			break;
		case(have_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(5), 0, message, 0, 4);
			message[4] = (byte) 4;
			System.arraycopy(intToByteArr(haveIndex), 0, message, 5, 4);
			break;
		case(bitfield_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(lengthPrefix), 0, message, 0, 4);
			message[4] = (byte) 5;
			System.arraycopy(bitfield, 0, message, 0, lengthPrefix-1);
			break;
		case(request_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(lengthPrefix), 0, message, 0, 4);
			message[4] = (byte) 6;
			System.arraycopy(intToByteArr(requestIndex), 0, message, 5, 4);
			System.arraycopy(intToByteArr(requestBegin), 0, message, 9, 4);
			System.arraycopy(intToByteArr(requestLength), 0, message, 13, 4);	
			break;
		case(piece_ID):
			message = new byte[lengthPrefix+4];
			System.arraycopy(intToByteArr(lengthPrefix), 0, message, 0, 4);
			message[4] = (byte) 7;
			System.arraycopy(intToByteArr(pieceIndex), 0, message, 5, 4);
			System.arraycopy(intToByteArr(pieceBegin), 0, message, 9, 4);
			System.arraycopy(pieceBlock, 0, message, 13, lengthPrefix-9);
			break;
		}

		
	}
	
	public static String decodeMessage(byte[] messageReceived) throws Exception{
		
		String message;
		
		if(messageReceived.length <= 4){
			throw new Exception("Tried to decode a message, but it wasn't of the right form");
		}

		int messageID = messageReceived[4];
		
		switch(messageID){
		case(choke_ID):
			message = "choke";
			return message;
		case(unchoke_ID):
			message = "unchoke";
			return message;
		case(interested_ID):
			message = "interested";
			return message;
		case(uninterested_ID):
			message = "uninterested";
			return message;
		case(have_ID):
			message = "have";
			return message;
		case(bitfield_ID):
			message = "bitfield";
			return message;
		case(request_ID):
			message = "request";
			return message;
		case(piece_ID):
			message = "piece";
			return message;
		}
		
		// Error if it reaches here!
		throw new Exception("Cannot match message_id received to a command");

		
	}
		
	
	/**
	 * 
	 * Helper method to represent integers as 4-byte big-endian arrays.
	 * 
	 * BigInteger.valueof(i).toByteArray() will create a byte array of smallest 
	 * possible size to represent i. 
	 * 
	 * @param i - integer which we wish to convert to 4-byte big-endian
	 * @return byte array representing integer
	 * 
	 */
	private byte[] intToByteArr(int i){
		
		byte[] byteArray = {(byte)(i >>> 24), (byte)(i >>> 16), (byte) (i >>> 8), (byte)(i)};
		return byteArray;
		
	}
	
	
}
