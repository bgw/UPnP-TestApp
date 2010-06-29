import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.action.ActionException;

public class GetDataActionInvocation extends ActionInvocation {
  private Object data;
  
  public GetDataActionInvocation(Service service, String action) {
    super(service.getAction(action));
  }
  
  public Object getData() {
    if(data == null) { data = getOutput().getValues()[0].getValue(); }
    return data;
  }
}
