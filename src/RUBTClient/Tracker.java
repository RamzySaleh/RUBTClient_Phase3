package RUBTClient;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import GivenTools.*;

public class Tracker {

    public TorrentInfo torrentInfo;
    public String peer_id;
    public int port;
    public int uploaded;
    public int downloaded;
    public int left;
    /**
     * Decoded interval from tracker of how frequently to send GET requests
     */
    public int interval;
    /**
     * Decoded list of peers from tracker
     */
    public List<Peer> peers;
	
	public Tracker(String torrentFileName){
		readTorrent(torrentFileName);
	}

    /**
     * Keys used to retrieve data from HTTP GET response to tracker
     */
    private static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {'p','e','e','r','s'});
    private static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] {'i','p'});
    private static final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {'p','e','e','r',' ','i','d'});
    private static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {'p','o','r','t'});
    private static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {'i','n','t','e','r','v','a','l'});

    /**
     * Event types that can be sent with HTTP GET request to tracker
     */
    public enum Event
    {
        NONE,
        STARTED,
        STOPPED,
        COMPLETED
    }

    /**
     * Read the torrent file and use it to create the torrentInfo object
     * @param path the path to the torrent file
     */
    public void readTorrent(String path)
    {
        File torrentFile = new File(path);
        byte[] torrentFileBytes = new byte[(int)torrentFile.length()];

        try
        {
            // Read torrent file into torrentFileBytes byte array
            DataInputStream dataInputStream = new DataInputStream(new FileInputStream(torrentFile));
            dataInputStream.readFully(torrentFileBytes);
            dataInputStream.close();

            // Create TorrentInfo object using torrentFileBytes
            torrentInfo = new TorrentInfo(torrentFileBytes);

            peer_id = generateRandomString(20);
            port = 6881;
            uploaded = 0;
            downloaded = 0;
            left = torrentInfo.file_length;
        }
        catch (BencodingException | FileNotFoundException e)
        {
            System.out.println("Error: " + e);
        }
        catch (IOException e)
        {
            System.out.println("Error: " + e);
        }
    }

    /**
     * Send an HTTP GET to the tracker announce url
     * @param eventType the event type to be sent with the HTTP GET request
     */
    public void sendTrackerRequest(Event eventType)
    {
        URL url;

        try
        {
            // Generate info_hash query
            StringBuilder sb = new StringBuilder();
            for (byte b : torrentInfo.info_hash.array())
            {
                sb.append("%").append(String.format("%02X", b));
            }
            String info_hash = "info_hash=" + sb.toString();

            // Generate peer_id query
            String peer_id = "peer_id=" + this.peer_id;

            // Generate port query
            String port = "port=" + this.port;

            // Generate uploaded query
            String uploaded = "uploaded=" + this.uploaded;

            // Generate downloaded query
            String downloaded = "downloaded=" + this.downloaded;

            // Generate left query
            String left = "left=" + this.left;

            // Generate event query
            String event = "";
            switch (eventType)
            {
                case NONE:
                    break;
                case STARTED:
                    event = "&event=started";
                    break;
                case STOPPED:
                    event = "&event=stopped";
                    break;
                case COMPLETED:
                    event = "&event=completed";
                    break;
            }

            // Generate url to send HTTP GET request to
            url = new URL(torrentInfo.announce_url.toString() + "?" + info_hash + "&" + peer_id + "&" + port + "&" + uploaded + "&" + downloaded + "&" + left + event);

            // Send HTTP GET request
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Receive GET response and put it into a byte array
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer, 0, buffer.length)) != -1)
            {
                out.write(buffer, 0, read);
            }
            out.flush();
            byte[] responseBytes = out.toByteArray();

            // Decode the BEncoded response
            Map<ByteBuffer,Object> response = (Map<ByteBuffer,Object>)Bencoder2.decode(responseBytes);

            // Decode the peer list from within the response dictionary
            List peers1 = (List)response.get(KEY_PEERS);
            decodePeerList(peers1);

            // Decode the interval from within the response dictionary
            this.interval = (int)response.get(KEY_INTERVAL);
        }
        catch (BencodingException | IOException e)
        {
            System.out.println("Error: " + e);
        }
    }

    /**
     * Decode the BEncoded peers list into an array list of Peer objects
     * @param peers the list of dictionaries returned from the peers key in the HTTP GET response dictionary
     */
    private void decodePeerList(List peers)
    {
        this.peers = new ArrayList<>();

        // Loop through all of the peers in the list
        for (int i = 0; i < peers.size(); i++)
        {
            try
            {
                // Get peer's dictionary from the list
                Map<ByteBuffer,Object> peerDictionary = (Map<ByteBuffer,Object>)peers.get(i);

                // Decode ip
                ByteBuffer ip = (ByteBuffer)peerDictionary.get(KEY_IP);

                // Decode peer_id
                ByteBuffer peer_id = (ByteBuffer)peerDictionary.get(KEY_PEER_ID);

                // Convert port to int
                int port = (int)peerDictionary.get(KEY_PORT);

                Peer peer = new Peer(new String(ip.array(), "ASCII"), peer_id.array(), port);

                // Add Peer object to peers list
                this.peers.add(i, peer);
            }
            catch (UnsupportedEncodingException e)
            {
                System.out.println("Error: " + e);
            }
        }
    }

    /**
     * Helper function to generate a random string
     * @param length the desired length of the string
     * @return randomly generated string
     */
    private String generateRandomString(int length)
    {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-";

        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
        {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }
	
}
