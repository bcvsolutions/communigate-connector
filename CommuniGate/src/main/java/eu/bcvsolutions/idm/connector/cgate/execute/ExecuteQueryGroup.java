package eu.bcvsolutions.idm.connector.cgate.execute;

import com.stalker.CGPro.CGProCLI;
import eu.bcvsolutions.idm.connector.cgate.CommuniGateConfiguration;
import eu.bcvsolutions.idm.connector.cgate.CommuniGateConnector;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.*;

import java.util.Hashtable;
import java.util.Vector;

public class ExecuteQueryGroup implements ExecuteQuery{

    private CGProCLI cli;
    private CommuniGateConfiguration config;

    private Log log = Log.getLog(ExecuteQueryGroup.class);

    public ExecuteQueryGroup(CGProCLI cli, CommuniGateConfiguration config){
        this.cli = cli;
        this.config = config;
    }

    @Override
    public void execute(String query, ResultsHandler handler, OperationOptions opt) {
        ConnectorObject object = null;
        if (StringUtil.isBlank(query)) {
            try {
                Vector lists = cli.listLists(config.getDomainName());
                for (Object list : lists) {
                    object = getGroupConnectorObject((String) list);
                    if (object != null) {
                        handler.handle(object);
                    }
                }
            } catch (Exception e) {
                log.error("Exception during listing accounts, error code: " + cli.getErrCode());
                e.printStackTrace();
            }
        } else {
            object = getGroupConnectorObject(query);
            if (object != null) {
                handler.handle(object);
            }
        }
    }

    /**
     * Gets all attributes for group
     * @param uid
     * @return
     */
    @SuppressWarnings("rawtypes")
    private ConnectorObject getGroupConnectorObject(String uid) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setName(uid);
        builder.setUid(uid);
        builder.setObjectClass(ObjectClass.GROUP);
        Hashtable settings;
        try {
            settings = cli.getList(uid);
            //
            String name = (String) settings.get(CommuniGateConnector.REAL_NAME_SETTING);
            Vector subscribers = cli.listSubscribers(uid,null,10000);
            //
            if ((subscribers != null) && !subscribers.isEmpty()) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.GROUP_SUBSCRIBERS, subscribers.subList(0, subscribers.size())));
            }
            if (name != null) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.GROUP_NAME, CGProCLI.decodeString(name)));
            }
            builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.GROUP_NAME, uid));
        } catch (Exception e) {
            log.error("Exception during getting account " + uid + "@" + config.getDomainName()+ ", error code: " + cli.getErrCode());
            e.printStackTrace();
            return null;
        }
        return builder.build();
    }
}
