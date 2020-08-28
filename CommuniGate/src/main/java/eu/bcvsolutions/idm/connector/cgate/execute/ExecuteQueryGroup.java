package eu.bcvsolutions.idm.connector.cgate.execute;

import java.util.Vector;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stalker.CGPro.CGProCLI;

import eu.bcvsolutions.idm.connector.cgate.CommuniGateConfiguration;
import eu.bcvsolutions.idm.connector.cgate.CommuniGateConnector;

public class ExecuteQueryGroup implements ExecuteQuery{

    private CGProCLI cli;
    private CommuniGateConfiguration config;

    private static final Logger log = LoggerFactory.getLogger(ExecuteQueryGroup.class);

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
                log.error("Exception during listing accounts, error code: [{}]", cli.getErrCode());
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
        try {
            Vector subscribers = cli.listSubscribers(uid,null,10000);
            //
            if ((subscribers != null) && !subscribers.isEmpty()) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.GROUP_SUBSCRIBERS, subscribers.subList(0, subscribers.size())));
            }
        } catch (Exception e) {
            String mail = uid + "@" + config.getDomainName();
            log.error("Exception during getting account [{}], error code: [{}]", mail, cli.getErrCode());
            e.printStackTrace();
            return null;
        }
        return builder.build();
    }
}
