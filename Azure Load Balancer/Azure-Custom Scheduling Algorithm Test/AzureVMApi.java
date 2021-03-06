/**
 * This is the copy of a working example
 */
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.ComputeManagementService;
import com.microsoft.azure.management.compute.models.*;
import com.microsoft.azure.management.network.NetworkResourceProviderClient;
import com.microsoft.azure.management.network.NetworkResourceProviderService;
import com.microsoft.azure.management.network.models.AzureAsyncOperationResponse;
import com.microsoft.azure.management.network.models.PublicIpAddressGetResponse;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.ResourceManagementService;
import com.microsoft.azure.management.storage.StorageManagementClient;
import com.microsoft.azure.management.network.models.DhcpOptions;
import com.microsoft.azure.management.storage.StorageManagementService;
import com.microsoft.azure.management.network.models.VirtualNetwork;
import com.microsoft.azure.utility.AuthHelper;
import com.microsoft.azure.utility.*;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Math;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
// The almost same sample code to create a vm, I just change some parameters and method name
public class AzureVMApi {
    private static ResourceManagementClient resourceManagementClient;
    private static StorageManagementClient storageManagementClient;
    private static ComputeManagementClient computeManagementClient;
    private static NetworkResourceProviderClient networkResourceProviderClient;

    // the source URI of VHD
    private static String sourceVhdlbUri = "https://xhwstorageaccount.blob.core.windows.net/system/Microsoft.Compute/Images/vhds/cc15619p22lbv2-osDisk.1cf68388-ac67-4165-bec0-67341257d50a.vhd";
    private static String sourceVhddcUri = "https://xhwstorageaccount.blob.core.windows.net/system/Microsoft.Compute/Images/vhds/cc15619p22dcv6-osDisk.b0c453f3-f75f-4a2d-bd9c-ae055b830124.vhd";
    private static String sourceVhdlgUri = "https://xhwstorageaccount.blob.core.windows.net/system/Microsoft.Compute/Images/vhds/cc15619p22lgv7-osDisk.c0410b8f-821e-4de3-b725-2a834fd10060.vhd";
    // configuration for your application token
    private static String baseURI = "https://management.azure.com/";
    private static String basicURI = "https://management.core.windows.net/";
    private static String endpointURL = "https://login.windows.net/";
    // these information is removed due to privacy
    private static String subscriptionId = "";
    private static String tenantID = "";
    private static String applicationID = "";
    private static String applicationKey = "";

    // configuration for your resource account/storage account
    private static String storageAccountName = "xhwstorageaccount";
    private static String resourceGroupNameWithVhd = "testcreatevm";
    private static String lbsize = "Standard_D1";
    private static String lgsize = "Standard_D1";
    private static String dcsize = VirtualMachineSizeTypes.STANDARD_A1;
    private static String region = "EastUs";
    private static String vmName = "";
    private static String resourceGroupName = "";

    // configuration for your virtual machine
    private static String adminName = "ubuntu";
    /**
      * Password requirements:
      * 1) Contains an uppercase character
      * 2) Contains a lowercase character
      * 3) Contains a numeric digit
      * 4) Contains a special character.
      */
    private static String adminPassword = "Cloud@123";

    public AzureVMApi() throws Exception{
        Configuration config = createConfiguration();
        resourceManagementClient = ResourceManagementService.create(config);
        storageManagementClient = StorageManagementService.create(config);
        computeManagementClient = ComputeManagementService.create(config);
        networkResourceProviderClient = NetworkResourceProviderService.create(config);
    }

    public static Configuration createConfiguration() throws Exception {
        // get token for authentication
        String token = AuthHelper.getAccessTokenFromServicePrincipalCredentials(
                        basicURI,
                        endpointURL,
                        tenantID,
                        applicationID,
                        applicationKey).getAccessToken();

        // generate Azure sdk configuration manager
        return ManagementConfiguration.configure(
                null, // profile
                new URI(baseURI), // baseURI
                subscriptionId, // subscriptionId
                token// token
                );
    }

    /***
     * Create a virtual machine given configurations.
     *
     * @param resourceGroupName: a new name for your virtual machine [customized], will create a new one if not already exist
     * @param vmName: a PUBLIC UNIQUE name for virtual machine
     * @param resourceGroupNameWithVhd: the resource group where the storage account for VHD is copied
     * @param sourceVhdUri: the Uri for VHD you copied
     * @param instanceSize
     * @param subscriptionId: your Azure account subscription Id
     * @param storageAccountName: the storage account where you VHD exist
     * @return created virtual machine IP
     */
    public static ResourceContext createVM (
        String resourceGroupName,
        String vmName,
        String resourceGroupNameWithVhd,
        String sourceVhdUri,
        String instanceSize,
        String subscriptionId,
        String storageAccountName) throws Exception {

        ResourceContext contextVhd = new ResourceContext(
                region, resourceGroupNameWithVhd, subscriptionId, false);
        ResourceContext context = new ResourceContext(
                region, resourceGroupName, subscriptionId, false);

        ComputeHelper.createOrUpdateResourceGroup(resourceManagementClient,context);
        context.setStorageAccountName(storageAccountName);
        contextVhd.setStorageAccountName(storageAccountName);
        context.setStorageAccount(StorageHelper.getStorageAccount(storageManagementClient,contextVhd));

        if (context.getNetworkInterface() == null) {
            if (context.getPublicIpAddress() == null) {
                NetworkHelper
                    .createPublicIpAddress(networkResourceProviderClient, context);
            }
            if (context.getVirtualNetwork() == null) {
                NetworkHelper
                    .createVirtualNetwork(networkResourceProviderClient, context);
            }

            VirtualNetwork vnet =  context.getVirtualNetwork();

            // set DhcpOptions
            DhcpOptions dop = new DhcpOptions();
            ArrayList<String> dnsServers = new ArrayList<String>(2);
            dnsServers.add("8.8.8.8");
            dop.setDnsServers(dnsServers);
            vnet.setDhcpOptions(dop);

            try {
                AzureAsyncOperationResponse response = networkResourceProviderClient.getVirtualNetworksOperations()
                    .createOrUpdate(context.getResourceGroupName(), context.getVirtualNetworkName(), vnet);
            } catch (ExecutionException ee) {
                if (ee.getMessage().contains("RetryableError")) {
                    AzureAsyncOperationResponse response2 = networkResourceProviderClient.getVirtualNetworksOperations()
                        .createOrUpdate(context.getResourceGroupName(), context.getVirtualNetworkName(), vnet);
                } else {
                    throw ee;
                }
            }


            NetworkHelper
                .createNIC(networkResourceProviderClient, context, context.getVirtualNetwork().getSubnets().get(0));

            NetworkHelper
                .updatePublicIpAddressDomainName(networkResourceProviderClient, resourceGroupName, context.getPublicIpName(), vmName);
        }

        System.out.println("[15319/15619] "+context.getPublicIpName());
        System.out.println("[15319/15619] Start Create VM...");

        try {
            // name for your VirtualHardDisk
            String osVhdUri = ComputeHelper.getVhdContainerUrl(context) + String.format("/os%s.vhd", vmName);

            VirtualMachine vm = new VirtualMachine(context.getLocation());

            vm.setName(vmName);
            vm.setType("Microsoft.Compute/virtualMachines");
            vm.setHardwareProfile(createHardwareProfile(context, instanceSize));
            vm.setStorageProfile(createStorageProfile(osVhdUri, sourceVhdUri));
            vm.setNetworkProfile(createNetworkProfile(context));
            vm.setOSProfile(createOSProfile(adminName, adminPassword, vmName));

            context.setVMInput(vm);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Remove the resource group will remove all assets (VM/VirtualNetwork/Storage Account etc.)
        // Comment the following line to keep the VM.
        // resourceManagementClient.getResourceGroupsOperations().beginDeleting(context.getResourceGroupName());
        // computeManagementClient.getVirtualMachinesOperations().beginDeleting(resourceGroupName,"project2.2");
        return context;
        }

    /***
     * Check public IP address of virtual machine
     *
     * @param context
     * @param vmName
     * @return public IP
     */
    public static String checkVM(ResourceContext context, String vmName) {
        String ipAddress = null;

        try {
            VirtualMachine vmHelper = ComputeHelper.createVM(
                    resourceManagementClient, computeManagementClient, networkResourceProviderClient, storageManagementClient,
                    context, vmName, "ubuntu", "Cloud@123").getVirtualMachine();

            System.out.println("[15319/15619] "+vmHelper.getName() + " Is Created :)");
            while(ipAddress == null) {
                PublicIpAddressGetResponse result = networkResourceProviderClient.getPublicIpAddressesOperations().get(resourceGroupName, context.getPublicIpName());
                ipAddress = result.getPublicIpAddress().getIpAddress();
                Thread.sleep(10);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ipAddress;
    }

    /***
     * Create a HardwareProfile for virtual machine
     *
     * @param context
     * @param instanceSize
     * @return created HardwareProfile
     */
    public static HardwareProfile createHardwareProfile(ResourceContext context, String instanceSize) {
        HardwareProfile hardwareProfile = new HardwareProfile();
        if (context.getVirtualMachineSizeType()!=null && !context.getVirtualMachineSizeType().isEmpty()) {
            hardwareProfile.setVirtualMachineSize(context.getVirtualMachineSizeType());
        } else {
            hardwareProfile.setVirtualMachineSize(instanceSize);
        }
        return hardwareProfile;
    }

    /***
     * Create a StorageProfile for virtual machine
     *
     * @param osVhdUri
     * @param sourceVhdUri
     * @return created StorageProfile
     */
    public static StorageProfile createStorageProfile(String osVhdUri, String sourceVhdUri) {
        StorageProfile storageProfile = new StorageProfile();

        VirtualHardDisk vHardDisk = new VirtualHardDisk();
        vHardDisk.setUri(osVhdUri);
        //set source image
        VirtualHardDisk sourceDisk = new VirtualHardDisk();
        sourceDisk.setUri(sourceVhdUri);

        OSDisk osDisk = new OSDisk("osdisk", vHardDisk, DiskCreateOptionTypes.FROMIMAGE);
        osDisk.setSourceImage(sourceDisk);
        osDisk.setOperatingSystemType(OperatingSystemTypes.LINUX);
        osDisk.setCaching(CachingTypes.NONE);

        storageProfile.setOSDisk(osDisk);

        return storageProfile;
    }

    /***
     * Create a NetworkProfile for virtual machine
     *
     * @param context
     * @return created NetworkProfile
     */
    public static NetworkProfile createNetworkProfile(ResourceContext context) {
        NetworkProfile networkProfile = new NetworkProfile();
        NetworkInterfaceReference nir = new NetworkInterfaceReference();
        nir.setReferenceUri(context.getNetworkInterface().getId());
        ArrayList<NetworkInterfaceReference> nirs = new ArrayList<NetworkInterfaceReference>(1);
        nirs.add(nir);
        networkProfile.setNetworkInterfaces(nirs);

        return networkProfile;
    }

    /***
     * Create a OSProfile for virtual machine
     *
     * @param adminName
     * @param adminPassword
     * @param vmName
     * @return created OSProfile
     */
    public static OSProfile createOSProfile(String adminName, String adminPassword, String vmName) {
        OSProfile osProfile = new OSProfile();
        osProfile.setAdminPassword(adminPassword);
        osProfile.setAdminUsername(adminName);
        osProfile.setComputerName(vmName);

        return osProfile;
    }

    /**
     * Originally this is the main method of Demo code, I just change the name to make it easier to
     * distinguish from Main.java
     *
     */
    public static String Azuresupercreate() throws Exception {
      // instantiate an AzureVMApi object
        AzureVMApi vm = new AzureVMApi();
        // generate the new data center name and resource group
        String seed = String.format("%d%d", (int) System.currentTimeMillis()%1000, (int)(Math.random()*1000));
        String vmdcName = String.format("cloud%s%s", seed, "vm");
        String resourcedcGroupName = String.format("cloud%s%s", seed, "ResourceGroup");
        // beging creat the new vm
        ResourceContext context = createVM (
                resourcedcGroupName,
                vmdcName,
                resourceGroupNameWithVhd,
                sourceVhddcUri,
                dcsize,
                subscriptionId,
                storageAccountName);
        System.out.println(checkVM(context, vmdcName));
       // construct and return the DNS of the new data center
         return vmdcName+".eastus.cloudapp.azure.com";
    }
    
   // the same method used in Project2.1 to check the rps of a vm, it is not used in this project, i just keep it there
    public static float checkRPS(String lgName, String log){
    	String logurl = "http://" + lgName + ".eastus.cloudapp.azure.com" + log;
        String response2 = fetch(logurl);
        int recentMinute = response2.lastIndexOf("Minute");
        int cutLeft = response2.indexOf("=", recentMinute);
        cutLeft += 1;
        float rpsrecord = 0;
        do{
        	rpsrecord += Float.parseFloat(response2.substring(cutLeft, cutLeft+6));
        	cutLeft += 6;
        }while(response2.indexOf("=", cutLeft)!=-1);
    	return rpsrecord;
    }
    // the same method used in Project2.1 to get all page content through a url, it is not used in this project, i just keep it there
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
