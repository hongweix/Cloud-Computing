import java.io.IOException;
import java.net.ServerSocket;

public class Main {
	private static final int PORT = 80;
	private static DataCenterInstance[] instances;
	private static ServerSocket serverSocket;

	//Update this list with the DNS of your data center instances
	//Remember to put your azure dns here. Don't put IP ADDRESS.
	static {
		// put the original data center in the instances[] array
		instances = new DataCenterInstance[3];
		instances[0] = new DataCenterInstance("First", "http://cloud-867821vm.eastus.cloudapp.azure.com");
		instances[1] = new DataCenterInstance("Second", "http://cloud-546495vm.eastus.cloudapp.azure.com");
		instances[2] = new DataCenterInstance("Third", "http://cloud-586991vm.eastus.cloudapp.azure.com");
	}
    // originally this method only throws IOException, but in fact it can throw other exception, so we change it to normal Exception
	public static void main(String[] args) throws Exception {
		initServerSocket();
		LoadBalancer loadBalancer = new LoadBalancer(serverSocket, instances);
		loadBalancer.start();
	}

	/**
	 * Initialize the socket on which the Load Balancer will receive requests from the Load Generator
	 */
	private static void initServerSocket() {
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			System.err.println("ERROR: Could not listen on port: " + PORT);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
