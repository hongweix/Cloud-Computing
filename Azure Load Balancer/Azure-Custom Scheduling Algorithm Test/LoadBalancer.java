import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoadBalancer {
	private static final int THREAD_POOL_SIZE = 4;
	private final ServerSocket socket;
	private final DataCenterInstance[] instances;
	// float[] array to store the cpu utilization of each data center
    private float[] CPUutilization = new float[3];
    // float to store the smallest cpu utilization
    private float MinCPUutilization;
    
	public LoadBalancer(ServerSocket socket, DataCenterInstance[] instances) {
		this.socket = socket;
		this.instances = instances;
	}

	/**
	 * My strategy for this test is: set a counter begining with 0, when counter does not arrive at 3, we do 
	 * like the round robin strategy, let every data center rotately get request, when counter arrives at 3,
	 * we begin check the rps of each cpu to find the lowest utilization, and parse request to the data center
	 * with lowest utilization
	 */
	public void start() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		int counter = 0;
		while (true) {
			//counter plus 1 each time enter the loop
		    counter += 1;
		    // when counter arrives 3
			if(counter == 3){
				/**
				 * check the cpu utilization of each data center
				 */
				CPUutilization[0] = checkRPS(instances[0].getUrl());
				CPUutilization[1] = checkRPS(instances[1].getUrl());
				CPUutilization[2] = checkRPS(instances[2].getUrl());
				/**
				 * compare each cpu utilization to find the minimum cpu utilization
				 */
				MinCPUutilization = CPUutilization[0] <= CPUutilization[1] ? CPUutilization[0] : CPUutilization[1];
				MinCPUutilization = MinCPUutilization <= CPUutilization[2] ? MinCPUutilization : CPUutilization[2];
				// in the for loop find the cpu with the minimum utilization
				for(int i = 0;i <3;i++){
					// if found, then parse request to this cpu and jump out of the for loop to save time
					if(MinCPUutilization == CPUutilization[i]){
						Runnable requestHandler3 = new RequestHandler(socket.accept(), instances[i]);
			            executorService.execute(requestHandler3);
			            break;
					}
				}
				// set counter back to 0
				counter = 0;
			}
			// when counter does not arrives 3
			else{
				// just using the round robin strategy to parse request to each data center 
		    	Runnable requestHandler = new RequestHandler(socket.accept(), instances[0]);
	            executorService.execute(requestHandler);
	            Runnable requestHandler1 = new RequestHandler(socket.accept(), instances[1]);
	            executorService.execute(requestHandler1);
	            Runnable requestHandler2 = new RequestHandler(socket.accept(), instances[2]);
	            executorService.execute(requestHandler2);
		    }
	    }
	/**
	 * Method to check the rps of data center, almost same with Project2.1, the idea comes from Distributed
	 * System class: when we fetch the plain text of the html content, just cut the information we need
	 */
	public static float checkRPS(String dcURL){
		// construct the logurl the check cpu infor
    	String logurl = dcURL + ":8080/info/cpu";
    	// using fetch method to get all information from that web page
        String response2 = fetch(logurl);
        // the cpu utilization located between <body>...</body>
        // cutLeft is the pointer to "<"
        int cutLeft = response2.indexOf("<body>");
        // move cutLeft to make it point to the first digital
        cutLeft += 6;
        // cutRight is the pointer to the second "<"
        int cutRight = response2.indexOf("</body>");
        // cut the cpu utilization and convert to float number and return
        float CPUutilization = Float.parseFloat(response2.substring(cutLeft, cutRight));
    	return CPUutilization;
    }
	/**
	 * Method to fetch all information, almost same with Project2.1, the idea comes from Distributed
	 * System class: when we fetch the plain text of the html content, just cut the information we need
	 */
	public static String fetch(String urlString) {
        String response = "";
        try {
            URL url = new URL(urlString);
            /*
             * Create an HttpURLConnection.  This is useful for setting headers
             * and for getting the path of the resource that is returned (which 
             * may be different than the URL above if redirected).
             * HttpsURLConnection (with an "s") can be used if required by the site.
             */
            HttpURLConnection connection;
            do{
            	connection = (HttpURLConnection) url.openConnection();
            }while(connection.getResponseCode() != 200);
            	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String str;
                // Read each line of "in" until done, adding each to "response"
                while ((str = in.readLine()) != null) {
                    // str is one line of text readLine() strips newline characters
                    response += str;
                }
                in.close();
            // Read all the text returned by the server  
        } catch (IOException e) {
            System.out.println("Eeek, an exception");
            // Do something reasonable.  This is left for students to do.
        }
        return response;
    }
}
