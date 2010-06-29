import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.binding.*;
import org.teleal.cling.binding.annotations.*;
import org.teleal.cling.model.*;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.types.*;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.DefaultUpnpServiceConfiguration;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.message.UpnpResponse;
import java.io.IOException;

/**
 * Cling opens all accessible and usable NetworkInterfaces and uses them
 */

public class TestApp {
  public static void main(String[] args) {
    if(args[0].equals("client")) {
      runClient();
    } else if(args[0].equals("server")) {
      runServer();
    } else {
      System.out.println("Usage: java TestApp <mode>");
      System.out.println("Possible modes: \"server\" \"client\"");
      System.exit(1);
    }
  }
  
  //This method is almost copied exactly from the example documentation
  public static void runClient() {
    try {
      UpnpService upnpService = new UpnpServiceImpl(
        new DefaultUpnpServiceConfiguration(8082)
      );
      
      // Add a listener for device registration events
      upnpService.getRegistry().addListener(
        createRegistryListener(upnpService)
      );
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
  
  //This method is almost copied exactly from the example documentation
  //Listener for clients connecting
  public static RegistryListener createRegistryListener(final UpnpService upnpService) {
    return new DefaultRegistryListener() {
      ServiceId serviceId = new ServiceId("pipeep", "UPnPTestServer-JavaCling");
      
      public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        DeviceService testServer;
        System.out.println("Remote device discovered: " + device);
        DeviceService[] services = device.getDeviceServices();
        for(DeviceService i : services) {
          System.out.println("  contains service:" + i.getServiceId().getId());
        }
        if ((testServer = device.getDeviceService(serviceId)) != null) {
          System.out.println("UPnP Test Server Service discovered: " + testServer);
          executeAction(upnpService, testServer.getService());
        }
      }
      
      public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        DeviceService testServer;
        System.out.println("Remote device disappeared: " + device);
        if ((testServer = device.getDeviceService(serviceId)) != null) {
          System.out.println("Service disappeared: " + testServer);
        }
      }
      
    };
  }
  
  public static void executeAction(final UpnpService upnpService, Service service) {
    ActionInvocation getData = new GetDataActionInvocation(service, "GetData");
    ActionInvocation getChecksum =
                            new GetDataActionInvocation(service, "GetChecksum");
    final Byte[][] data = new Byte[1][0]; // making this a 2d array is a cheap
                                          // hack to allow this to work with
                                          // inner classes
    
    //GetChecksum
    final ActionCallback getChecksumCallback = new ActionCallback(getChecksum) {
      public void success(ActionInvocation actionInvocation) {
        System.out.println("Read checksum from server, comparing to data...");
        if(TestServer.calculateChecksum(data[0]) == ((Integer)((GetDataActionInvocation)actionInvocation).getData()).intValue()) {
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
  
  //This method is almost copied exactly from the example documentation
  public static void runServer() {
    try {
      final UpnpService upnpService = new UpnpServiceImpl();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          upnpService.shutdown();
        }
      });
      //Add the bound local device to the registry
      upnpService.getRegistry().addDevice(createDevice());
    } catch (Exception ex) {
      System.err.println("Exception occured: " + ex);
      ex.printStackTrace(System.err);
      System.exit(1);
    }
  }
  
  //This method is almost copied exactly from the example documentation
  public static LocalDevice createDevice() throws
                ValidationException, LocalServiceBindingException, IOException {
    
    DeviceIdentity identity = new DeviceIdentity(
      UDN.uniqueSystemIdentifier("Test UPnP App (Java/Cling)")
    );
    DeviceType type = new UDADeviceType("UPnPTest", 1);
    
    DeviceDetails details = new DeviceDetails(
      "UPnP Test App (Java/Cling)",
      new ManufacturerDetails("pipeep"),
      new ModelDetails("UPnPTestApp", "A UPnP test application made in Java "+
                                      "with the AGPL licensed Cling library",
                                      "v1")
    );
    Icon icon = null;
    
    DeviceService<LocalService> deviceService =
      new AnnotationLocalServiceBinder().read(TestServer.class);
    
    deviceService.getService().setManager(
      new DefaultServiceManager(deviceService.getService(), TestServer.class)
    );
    
    return new LocalDevice(
            identity,
            type,
            details,
            null, //new Icon[]{icon}, // Or 'null' for no icons
            new DeviceService[]{deviceService}
    );
  }
}
