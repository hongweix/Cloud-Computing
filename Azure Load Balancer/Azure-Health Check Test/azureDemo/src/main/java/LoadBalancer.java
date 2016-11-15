import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LoadBalancer {
	private static int THREAD_POOL_SIZE = 4;
	private final ServerSocket socket;
	private static DataCenterInstance[] instances;
	// float[] array to store each data center cpu utilization
    private static float[] CPUutilization = new float[3];
    // float to store the minimun cpu utilization
    private static float MinCPUutilization;
    // int[] array to store the status of the three data center, originally setting to 1 for each
    // it performs as a block, if vmstatus[i] value is 0, means it is blocked, we can not do anything 
    // on it until its value return to 1
    private static int[] vmstatus = {1,1,1};
	public LoadBalancer(ServerSocket socket1, DataCenterInstance[] instances1) {
		socket = socket1;
		instances = instances1;
	}
	/**
	 * Health Check method try to get the HTTP head information from a certain url, referenced from
	 * from Distributed System class
	 */
	public static int HealthCheck(String dcURL){
		String checkurl = dcURL;
		//status to return of this Health check, if it success then is 1, else if failed or has excpetion, then is 0
		int status = 0;
		try {
            URL url = new URL(checkurl);
            /*
             * Create an HttpURLConnection.  This is useful for setting headers
             * and for getting the path of the resource that is returned (which 
             * may be different than the URL above if redirected).
             * HttpsURLConnection (with an "s") can be used if required by the site.
             */
            HttpURLConnection connection = null;
            connection = (HttpURLConnection) url.openConnection();
            // use the "HEAD" HTTP method
            connection.setRequestMethod("HEAD");
            // if success then is 1, else then is 0
            status = connection.getResponseCode() == 200 ? 1:0;
        } catch (IOException e) {
             System.out.println("health check exception");
             // if has exception, then return 0
             return 0;
        }
        // return status
		return status;
	}
	/**
	 * check RPS method almost same with project2.1, referenced from Distributed System class
	 */
	public static float checkRPS(String dcURL){
		// construct the logurl the check cpu info
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
	
	// In the health check test, I used multi-threading, because it is difficult to arrive requirements using single thread
	public void start() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		while (true) {
			// check if we can access the first data center and check is it alive or not
			// if it is alive, then we just parse request to it
			if(vmstatus[0] == 1 && HealthCheck(instances[0].getUrl()) == 1){
	    		Runnable requestHandler = new RequestHandler(socket.accept(), instances[0]);
	            executorService.execute(requestHandler);
	    	}
	    	// if it is not alive but we can access to it
	    	else if(vmstatus[0] == 1){
	    		// we firstly need to set the access to 0, means now we cannot access it
	    		vmstatus[0] = 0;
	    		// start an anonymous Thread
	    		new Thread(new Runnable(){
	    			// implement the run method
	    			public void run(){
	    				String newvm = null;
	    				// begin creating a new vm
						try {
							newvm = AzureVMApi.Azuresupercreate();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// sleep 15 second for the creating to finish
	    				try {
							Thread.sleep(15000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                        // return information when finish creating
                        System.out.println("New vm is" + newvm);
                        // try to connect this new vm
	    				do{
                             HealthCheck("http://"+newvm);
	    				}while(HealthCheck("http://"+newvm) != 1);
	    				// if connecting success, give some information
	    				System.out.println("finish creating" + newvm);
	    				// add this new vm to instances[] array
	    				instances[0] = new DataCenterInstance("Savior", "http://"+newvm);
	    				// reset vmstatus to 1
	    				vmstatus[0] = 1;
	    		}	
	    	}).start();}
			// check if we can access the second data center and check is it alive or not
			// if it is alive, then we just parse request to it
			if(vmstatus[1] == 1 && HealthCheck(instances[1].getUrl()) == 1){
	    		Runnable requestHandler = new RequestHandler(socket.accept(), instances[1]);
	            executorService.execute(requestHandler);
	    	}
	    	// if it is not alive but we can access to it
	    	else if(vmstatus[1] == 1){
	    		// we firstly need to set the access to 0, means now we cannot access it
	    		vmstatus[1] = 0;
	    		// start an anonymous Thread
	    		new Thread(new Runnable(){
	    			// implement the run method
	    			public void run(){
	    				String newvm = null;
	    				// begin creating a new vm
						try {
							newvm = AzureVMApi.Azuresupercreate();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// sleep 15 second for the creating to finish
	    				try {
							Thread.sleep(15000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// return information when finish creating
                                        System.out.println("new vm is"+newvm);
                                        // try to connect this new vm
	    				do{
                                               HealthCheck("http://"+newvm);
	    				}while(HealthCheck("http://"+newvm) != 1);
	    				// if connecting success, give some information
	    				System.out.println("finish creating" + newvm);
	    				// add this new vm to instances[] array
	    				instances[1] = new DataCenterInstance("Savior","http://"+newvm);
	    				// reset vmstatus to 1
	    				vmstatus[1] = 1;
	    		}	
	    	}).start();}
			// check if we can access the third data center and check is it alive or not
			// if it is alive, then we just parse request to it
	    	if(vmstatus[2] == 1 && HealthCheck(instances[2].getUrl()) == 1){
	    		Runnable requestHandler = new RequestHandler(socket.accept(), instances[2]);
	            executorService.execute(requestHandler);
	    	}
	    	// if it is not alive but we can access to it
	    	else if(vmstatus[2] == 1){
	    		// we firstly need to set the access to 0, means now we cannot access it
	    		vmstatus[2] = 0;
	    		// start an anonymous Thread
	    		new Thread(new Runnable(){
	    			// implement the run method
	    			public void run(){
	    				String newvm = null;
	    				// begin creating a new vm
						try {
							newvm = AzureVMApi.Azuresupercreate();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// sleep 15 second for the creating to finish
	    				try {
							Thread.sleep(15000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// return information when finish creating
                                        System.out.println("new vm is" + newvm);
                                        // try to connect this new vm
	    				do{
	    				       HealthCheck("http://"+newvm);	
	    				}while(HealthCheck("http://"+newvm) != 1);
	    				// if connecting success, give some information
	    				System.out.println("finish creating" + newvm);
	    				// add this new vm to instances[] array
	    				instances[2] = new DataCenterInstance("Savior", "http://"+newvm);
	    				// reset vmstatus to 1
	    				vmstatus[2] = 1;
	    		}	
	    	}).start();}
		}
	}
}

