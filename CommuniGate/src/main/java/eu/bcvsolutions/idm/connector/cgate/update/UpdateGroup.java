package eu.bcvsolutions.idm.connector.cgate.update;

import com.stalker.CGPro.CGProCLI;
import com.stalker.CGPro.CGProException;
import eu.bcvsolutions.idm.connector.cgate.CommuniGateConnector;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class UpdateGroup implements Update {

    private CGProCLI cli;

    private Log log = Log.getLog(UpdateGroup.class);

    public UpdateGroup(CGProCLI cli){
        this.cli = cli;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Uid update(Uid uid, Set<Attribute> attributes, OperationOptions options) {
        String name = null;
        List<Object> subscribers = null;
        String id = null;
        for (Attribute attr : attributes) {
            switch (attr.getName()) {
                case CommuniGateConnector.GROUP_NAME:
                    name = (String) attr.getValue().get(0);
                    break;
                case CommuniGateConnector.GROUP_SUBSCRIBERS:
                    subscribers = attr.getValue();
                    break;
                case CommuniGateConnector.GROUP_ID:
                    id = (String) attr.getValue().get(0);
                    break;
            }
        }
        try {
            Vector vector = cli.listSubscribers(uid.getUidValue(), null, 1000);
            for(Object subscriber : subscribers){
                cli.list(uid.getUidValue(),"subscribe", (String) subscriber, true, false);
                vector.remove(subscriber);
            }
            for(Object subscriber : vector){
                cli.list(uid.getUidValue(),"unsubscribe", (String) subscriber, true, false);
            }
            if(id != null){
                Hashtable list = cli.getList(uid.getUidValue());
                list.put(CommuniGateConnector.GROUP_ID, id);
                cli.updateList(name, list);
            }
        } catch (CGProException e) {
            e.printStackTrace();
        }
        return new Uid(uid.getUidValue());
    }
}
