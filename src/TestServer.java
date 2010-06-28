import org.teleal.cling.binding.annotations.*;
import org.teleal.common.util.ByteArray;
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
  
  @UpnpStateVariable(sendEvents = false)
  private Byte[] data;
  
  @UpnpStateVariable(sendEvents = false)
  private Integer checksum;
  
  public TestServer() {
    byte[] rawData = new byte[1024*1024]; //one MB of data
    new Random().nextBytes(rawData);
    data = ByteArray.toWrapper(rawData);
    CRC32 crc = new CRC32();
    crc.update(rawData);
    checksum = (int)crc.getValue(); // autoboxing!
  }
  
  
  @UpnpAction(out = @UpnpOutputArgument(name = "DataChecksum"))
  public Integer getChecksum() {
    return checksum;
  }
  
  @UpnpAction(out = @UpnpOutputArgument(name = "RandomData"))
  public Byte[] getData() {
    return data;
  }
}
