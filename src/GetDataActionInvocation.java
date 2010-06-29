import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionArgumentValue;

public class GetDataActionInvocation extends ActionInvocation {
  private Object data;
  private String arg;
  
  public GetDataActionInvocation(Service service, String action, String arg) {
    super(service.getAction(action));
    if(getAction() == null) {
      System.out.println("Warning: Action \""+action+"\" was not found.");
    }
    this.arg = arg;
  }
  
  public Object getData() {
    if(data == null) {
      for(ActionArgumentValue i : getOutput().getValues()) {
        if(i.getArgument().getName().equals(arg)) {
          data = i.getValue(); break;
        }
      }
    }
    return data;
  }
}
