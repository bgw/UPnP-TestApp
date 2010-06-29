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
      new TestClient().run();
    } else if(args[0].equals("server")) {
      runServer();
    } else {
      System.out.println("Usage: java TestApp <mode>");
      System.out.println("Possible modes: \"server\" \"client\"");
      System.exit(1);
    }
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
