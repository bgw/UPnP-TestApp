import org.teleal.cling.DefaultUpnpServiceConfiguration;
import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.controlpoint.ActionCallback;

public class TestClient {
  private UpnpService upnpService;
  
  public TestClient() {
    
  }
  
  public void run() {
    try {
      upnpService = new UpnpServiceImpl(
        new DefaultUpnpServiceConfiguration(8082)
      );
      
      // Add a listener for device registration events
      upnpService.getRegistry().addListener(new ClientListener());
      System.out.println("Listening for servers");
      
      System.out.println("Broadcasting search message");
      // Broadcast a search message for all devices
      upnpService.getControlPoint().search(
        new STAllHeader()
      );
      System.out.println("Waiting for a response");
      
    } catch (Exception ex) {
      System.err.println("Exception occured: " + ex);
      ex.printStackTrace(System.err);
      System.exit(1);
    }
  }
  
  //Listener for clients connecting
  private class ClientListener extends DefaultRegistryListener {
    private final ServiceId SERVICE_ID =
                            new ServiceId("pipeep", "UPnPTestServer-JavaCling");
    
    public ClientListener() {
    }
    
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
      DeviceService testServer;
      System.out.println("Remote device discovered: " + device);
      DeviceService[] services = device.findDeviceServices();
      for(DeviceService i : services) {
        System.out.println("  contains service:" + i.getServiceId().getId());
      }
      if ((testServer = device.findDeviceService(SERVICE_ID)) != null) {
        System.out.println("Test Server Service discovered: " + testServer);
        executeAction(testServer.getService());
      }
    }
    
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
      DeviceService testServer;
      System.out.println("Remote device disappeared: " + device);
      if ((testServer = device.findDeviceService(SERVICE_ID)) != null) {
        System.out.println("Service disappeared: " + testServer);
      }
    }
  }
  
  
  // After the service has been found...
  
  public void executeAction(Service service) {
    ActionInvocation getData =
                  new GetDataActionInvocation(service, "GetData", "RandomData");
    ActionInvocation getChecksum =
            new GetDataActionInvocation(service, "GetChecksum", "DataChecksum");
    final Byte[][] data = new Byte[1][0]; // making this a 2d array is a cheap
                                          // hack to allow this to work with
                                          // inner classes
    
    //GetChecksum
    final ActionCallback getChecksumCallback = new ActionCallback(getChecksum) {
      public void success(ActionInvocation actionInvocation) {
        System.out.println("Read checksum from server, comparing to data...");
        int serverChecksum = // leave it to Java to make things really verbose
          ((Integer)((GetDataActionInvocation)actionInvocation).getData())
                    .intValue();
        if(TestServer.calculateChecksum(data[0]) == serverChecksum) {
          System.out.println("Match!");
        } else {
          System.out.println("Failure.");
        }
      }
      public void failure(ActionInvocation actionInvoc, UpnpResponse oper) {
        System.out.println("Failed to read checksum from server: " + oper);
      }
    };
    
    // GetData
    ActionCallback getDataCallback = new ActionCallback(getData) {
      public void success(ActionInvocation actionInvocation) {
        System.out.println("Read data from server, reading checksum...");
        data[0] = (Byte[])((GetDataActionInvocation)actionInvocation).getData();
        upnpService.getControlPoint().execute(getChecksumCallback);
      }
      public void failure(ActionInvocation actionInvoc, UpnpResponse oper) {
        System.out.println("Failed to read data from server: " + oper);
      }
    };
    
    System.out.println("Reading data from server");
    // Executes asynchronous in the background
    upnpService.getControlPoint().execute(getDataCallback);
  }
}
