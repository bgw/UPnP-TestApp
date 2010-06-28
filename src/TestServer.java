import org.teleal.cling.binding.annotations.*;
import java.util.Random;
import java.util.zip.CRC32;

//TODO: add options that can be set on the server by the client to demo
//      transfers in both directions

@UpnpService(
  serviceId = @UpnpServiceId(value = "UPnPTestServer-JavaCling",
                             namespace = "pipeep"),
  serviceType = @UpnpServiceType(value = "UPnPTestServer",
                                 namespace = "schemas-pipeep",
                                 version = 1)
)
public class TestServer {
  
  @UpnpStateVariable(datatype = "byte[]", sendEvents = false)
  private byte[] data = new byte[1024*1024]; //one MB of data
  
  @UpnpStateVariable(datatype = "java.util.zip.CRC32", sendEvents = false)
  private CRC32 checksum = new CRC32();
  
  public TestServer() {
    new Random().nextBytes(data);
    checksum.update(data);
  }
  
  
  @UpnpAction(out = @UpnpOutputArgument(name = "DataChecksum"))
  public long getChecksum() {
    return checksum.getValue();
  }
  
  @UpnpAction(out = @UpnpOutputArgument(name = "RandomData"))
  public byte[] getData() {
    return data;
  }
}
