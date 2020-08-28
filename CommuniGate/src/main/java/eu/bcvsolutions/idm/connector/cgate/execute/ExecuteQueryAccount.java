package eu.bcvsolutions.idm.connector.cgate.execute;

import java.util.Hashtable;
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
import eu.bcvsolutions.idm.connector.cgate.utils.Utils;

public class ExecuteQueryAccount implements ExecuteQuery {

    private CGProCLI cli;
    private CommuniGateConfiguration config;

    private static final Logger log = LoggerFactory.getLogger(ExecuteQueryAccount.class);

    public ExecuteQueryAccount(CGProCLI cli, CommuniGateConfiguration config){
        this.cli = cli;
        this.config = config;
    }

    @Override
    public void execute(String query, ResultsHandler handler, OperationOptions opt) {
        ConnectorObject object = null;
        if (StringUtil.isBlank(query)) {
            try {
                Hashtable accounts = cli.listAccounts(config.getDomainName());
                for (Object key : accounts.keySet()) {
                    object = getConnectorObject((String) key);
                    if (object != null) {
                        handler.handle(object);
                    }
                }
            } catch (Exception e) {
                log.error("Exception during listing accounts, error code: [{}]", cli.getErrCode());
                e.printStackTrace();
            }
        } else {
            object = getConnectorObject(query);
            if (object != null) {
                handler.handle(object);
            }
        }
    }

    /**
     * Gets all attributes for account
     * @param uid
     * @return
     */
    @SuppressWarnings("rawtypes")
    private ConnectorObject getConnectorObject(String uid) {
        log.info("CGate connector - getObject, uid: [{}]", uid);
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setName(uid);
        builder.setUid(uid);
        builder.setObjectClass(ObjectClass.ACCOUNT);
        Hashtable settings;
        try {
            String email = uid + "@" + config.getDomainName();
            settings = cli.getAccountSettings(email);

            String realName = (String) settings.get(CommuniGateConnector.REAL_NAME_SETTING);
            String unit = (String) settings.get(CommuniGateConnector.UNIT_SETTING);
            String city = (String) settings.get(CommuniGateConnector.CITY_SETTING);
            Vector accModes = Utils.asVector(settings.get(CommuniGateConnector.ACCESS_MODES_SETTING));
            if (accModes == null || !accModes.contains(CommuniGateConnector.ACCESS_MODES_NONE)) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.DISABLED_ATTR, false));
            } else {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.DISABLED_ATTR, true));
            }
            if ((accModes != null) && !accModes.isEmpty()) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.ACCESS_MODES_ATTR, accModes.subList(0, accModes.size())));
            }
            if (realName != null) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.REAL_NAME_ATTR, CGProCLI.decodeString(realName)));
            }
            if (unit != null) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.UNIT_ATTR, CGProCLI.decodeString(unit)));
            }
            if (city != null) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.CITY_ATTR, CGProCLI.decodeString(city)));
            }
            builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.ACCOUNT_ATTR, uid));

            Vector accountAliases = cli.getAccountAliases(email);
            if (accountAliases != null) {
                builder.addAttribute(AttributeBuilder.build(CommuniGateConnector.ALIASES_ATTR, accountAliases.subList(0, accountAliases.size())));
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
