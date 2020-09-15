package eu.bcvsolutions.idm.connector.cgate.create;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Uid;

import com.stalker.CGPro.CGProCLI;

import eu.bcvsolutions.idm.connector.cgate.CommuniGateConfiguration;
import eu.bcvsolutions.idm.connector.cgate.CommuniGateConnector;
import eu.bcvsolutions.idm.connector.cgate.utils.Utils;

public class CreateAccount {

    private CGProCLI cli;
    private CommuniGateConfiguration config;

    public CreateAccount(CGProCLI cli, CommuniGateConfiguration config){
        this.cli = cli;
        this.config = config;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uid create(Set<Attribute> attributes){
        Hashtable settings = new Hashtable();

        String accountUid = null;
        GuardedString password = null;
        boolean disabled = false;
        List accessModes = null;
        List aliases = null;
        String defaultStorageLimit = config.getDefaultMailStorageLimit();
        String defaultMailboxLimit = config.getDefaultMailBoxLimit();


        if ((defaultStorageLimit != null) && !"".equals(defaultStorageLimit)) {
            settings.put(CommuniGateConnector.MAIL_STORAGE_LIMIT_SETTING, defaultStorageLimit);
        }
        if ((defaultMailboxLimit != null) && !"".equals(defaultMailboxLimit)) {
            settings.put(CommuniGateConnector.MAIL_BOX_LIMIT_SETTING, defaultMailboxLimit);
        }

        //iterate all attributes
        for (Attribute attr : attributes) {
            String name = attr.getName();
            if (CommuniGateConnector.REAL_NAME_ATTR.equalsIgnoreCase(name)) {
                settings.put(CommuniGateConnector.REAL_NAME_SETTING, CGProCLI.encodeString((String) attr.getValue().get(0)));
            } else if (CommuniGateConnector.ACCOUNT_ATTR.equals(name)) {
                accountUid = (String) attr.getValue().get(0);
            } else if (CommuniGateConnector.PASSWORD_ATTR.equals(name)) {
                password = (GuardedString) attr.getValue().get(0);
            } else if (CommuniGateConnector.DISABLED_ATTR.equals(name)) {
                disabled = (Boolean) attr.getValue().get(0);
            } else if (CommuniGateConnector.ACCESS_MODES_ATTR.equals(name)) {
                accessModes = attr.getValue();
            } else if (CommuniGateConnector.UNIT_ATTR.equals(name)) {
                settings.put(CommuniGateConnector.UNIT_SETTING, CGProCLI.encodeString((String) attr.getValue().get(0)));
            } else if (CommuniGateConnector.CITY_ATTR.equals(name)) {
                settings.put(CommuniGateConnector.CITY_SETTING, CGProCLI.encodeString((String) attr.getValue().get(0)));
            } else if (CommuniGateConnector.ALIASES_ATTR.equals(name)) {
                aliases = attr.getValue();
            }
        }
        Vector accModes = new Vector();
        if (!disabled) {
            if (accessModes == null) {
                String accModesString = config.getDefaultAccModes();
                String[] modes = accModesString.split(",");
                for (String mode : modes) {
                    accModes.addElement(mode);
                }
            } else {
                for (Object mode : accessModes) {
                    accModes.addElement(mode);
                }
            }
        } else {
            accModes.addElement(CommuniGateConnector.ACCESS_MODES_NONE);
        }
        settings.put(CommuniGateConnector.ACCESS_MODES_SETTING, Utils.transformAccModes(accModes));

        try {
            if ((accountUid != null) && (!"".equals(accountUid))) {
                String email = accountUid + "@" + config.getDomainName();
                cli.createAccount(email,settings,null,false);
                if (password != null) {
                    // set password
                    cli.setAccountPassword(email, Utils.asString(password));
                }

                // set aliases
                if (aliases != null) {
                    Vector aliasesVector = new Vector(aliases);
                    cli.setAccountAliases(email, aliasesVector);
                }

                return new Uid(accountUid);
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
