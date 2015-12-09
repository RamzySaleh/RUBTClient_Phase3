package RUBTClient;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import RUBTClient.Tracker.Event;

/**
 * Group 24
 * @author Rohan Vernekar
 * @author Yaman Saadi
 * @author Ramzy Saleh
 *
 */
public class RUBTClient
{

    public static void main(final String[] args) throws Exception
    {
    	if (args.length < 2){
    		System.out.println("Too few arguments");
    		return;
    	}
    	
    	// Start the tracker class. Decodes the torrent file. 
    	final Tracker tracker = new Tracker(args[0]);
        tracker.sendTrackerRequest(Event.NONE);
        
        // Determine the interval length for updates.
        final int trackerUpdateInterval = Math.min(tracker.interval/2, 180/2);
        
        /** Create a file pointer to the temporary file where we will save verified pieces
         * in case the user cancels the download.
         */
       
        File fp = new File("File.tmp");
        
        final PeerManager peerManager = new PeerManager(tracker);
        
        // Create a Client object.
        final Client downloadClient = new Client(tracker, peerManager, fp, args[1]);
        
        // Check if the file was ever downloaded before. If so, load verified pieces.
        downloadClient.checkFileState();
        

        final Server server = new Server(tracker, peerManager);
        

        final updateTracker ut = new updateTracker(tracker, trackerUpdateInterval);
        
        // Pool that includes upload, download, and tracker update threads.
        ExecutorService pool = Executors.newFixedThreadPool(4);

        Runnable r0 = new Runnable()
        {
            @Override
            public void run()
            { // Keep alive messages, sent every 2 minutes
                peerManager.keepAlive();
            }
        };
        pool.execute(r0);

        Runnable r1 = new Runnable()
        {
           @Override
           public void run()
           { // Client (used to download)
        	   downloadClient.run();
           }
        };
       pool.execute(r1);
        
        Runnable r2 = new Runnable()
        {
           @Override
           public void run()
           { // Server (used to upload)
        	   server.run();
           }
        };
        pool.execute(r2);
        
        Runnable r3 = new Runnable()
        {
           @Override
           public void run()
           { // Update tracker
        	   ut.run();
           }
        };
        pool.execute(r3);
        
        while(!Client.userInput.equals("-1")){
        	Thread.sleep(700);
        }
        
        for (int j = 0; j < peerManager.peersConnectedTo.size(); j++){
        	peerManager.peersConnectedTo.get(j).disconnectPeer();
        }
        pool.shutdown();

    }
    
    /**
     * 
     * This class is responsible for updating the tracker at the appropriate
     * interval. A message is sent every update interval time and stops
     * when the user enters '-1'. 
     *
     */
    public static class updateTracker implements Runnable{ 
    	
    	public static Tracker tracker;
    	public static int trackerUpdateInterval;
    	
    	public updateTracker(Tracker tracker, int trackerUpdateInterval){
    		updateTracker.tracker = tracker;
    		updateTracker.trackerUpdateInterval = trackerUpdateInterval;
    	}
    	public void run(){
    		long startTime = System.nanoTime();
    		long currentTime;
    		int i = 1;
    		while(!Client.userInput.equals("-1")){
    			currentTime = System.nanoTime();
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
      			long interval = i*trackerUpdateInterval*(long)Math.pow(10, 9);

    			if((currentTime-startTime)>interval){
        			System.out.println("Tracker updated!");        			
        			tracker.sendTrackerRequest(Event.NONE);
        			i++;
    			}

    		}
    		System.out.println("Tracker update quit!");
    	}
    }
}
