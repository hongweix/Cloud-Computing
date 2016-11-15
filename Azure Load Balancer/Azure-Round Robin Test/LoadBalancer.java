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
	
	public LoadBalancer(ServerSocket socket, DataCenterInstance[] instances) {
		this.socket = socket;
		this.instances = instances;
	}

	// Complete this function
	public void start() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		//when begining the round robin test, rotately parse request to each data center
		while (true) {
		        //parse request to data center 1 which is instance[0]
		    	Runnable requestHandler = new RequestHandler(socket.accept(), instances[0]);
	            executorService.execute(requestHandler);
	            //parse request to data center 2 which is instance[1]
	            Runnable requestHandler1 = new RequestHandler(socket.accept(), instances[1]);
	            executorService.execute(requestHandler1);
	            //parse request to data center 3 which is instance[2]
	            Runnable requestHandler2 = new RequestHandler(socket.accept(), instances[2]);
	            executorService.execute(requestHandler2);
		    }      
	}	
}
