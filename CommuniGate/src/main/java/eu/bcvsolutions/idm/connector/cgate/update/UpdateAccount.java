package eu.bcvsolutions.idm.connector.cgate.update;

import com.stalker.CGPro.CGProCLI;
import com.stalker.CGPro.CGProException;
import eu.bcvsolutions.idm.connector.cgate.CommuniGateConfiguration;
import eu.bcvsolutions.idm.connector.cgate.CommuniGateConnector;
import eu.bcvsolutions.idm.connector.cgate.utils.Utils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;

import java.util.*;

public class UpdateAccount implements Update {

    private CGProCLI cli;
    private CommuniGateConfiguration config;

    private Log log = Log.getLog(UpdateAccount.class);

    public UpdateAccount(CGProCLI cli, CommuniGateConfiguration config){
        this.cli = cli;
        this.config = config;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Uid update(Uid uid, Set<Attribute> attributes, OperationOptions options){
        String accountUid = null;
        String uidValue = (String) uid.getValue().get(0);
        GuardedString password = null;
        String realName = null;
        boolean disabled = false;
        List accessModes = null;
        String unit = null;
        String city = null;
        List aliases = null;

        for (Attribute attr : attributes) {
            String name = attr.getName();
            if (CommuniGateConnector.ACCOUNT_ATTR.equals(name)) {
                accountUid = (String) attr.getValue().get(0);
            } else if (CommuniGateConnector.PASSWORD_ATTR.equals(name)) {
                password = (GuardedString) attr.getValue().get(0);
            } else if (CommuniGateConnector.REAL_NAME_ATTR.equals(name)) {
                realName = (String) attr.getValue().get(0);
            } else if (CommuniGateConnector.DISABLED_ATTR.equals(name)) {
                disabled = (Boolean) attr.getValue().get(0);
            } else if (CommuniGateConnector.ACCESS_MODES_ATTR.equals(name)) {
                accessModes = attr.getValue();
            } else if (CommuniGateConnector.UNIT_ATTR.equals(name)) {
                unit = (String) attr.getValue().get(0);
            } else if (CommuniGateConnector.CITY_ATTR.equals(name)) {
                city = (String) attr.getValue().get(0);
            } else if (CommuniGateConnector.ALIASES_ATTR.equals(name)) {
                aliases = attr.getValue();
            }
        }
        passwordChange(password,uidValue);
        rename(accountUid,uidValue);
        setAlias(aliases,accountUid);
        accountSetting(realName,unit,city,accountUid,disabled,accessModes);
        //
        return new Uid(accountUid);
    }

    @SuppressWarnings("rawtypes")
    private void accountSetting(String realName, String unit, String city, String accountUid, boolean disabled, List accessModes){
        Hashtable settings = null;
        try {
            settings = cli.getAccountSettings(accountUid + "@" + config.getDomainName());
            if (realName != null) {
                settings.put(CommuniGateConnector.REAL_NAME_SETTING, CGProCLI.encodeString(realName));
            }
            if (unit != null) {
                settings.put(CommuniGateConnector.UNIT_SETTING, CGProCLI.encodeString(unit));
            }
            if (city != null) {
                settings.put(CommuniGateConnector.CITY_SETTING, CGProCLI.encodeString(city));
            }

            Vector accModes = Utils.asVector(settings.get(CommuniGateConnector.ACCESS_MODES_SETTING));
            if (accModes == null) {
                accModes = new Vector();
            }

            if (disabled) {
                accModes.clear();
                accModes.addElement(CommuniGateConnector.ACCESS_MODES_NONE);
            } else if (accessModes != null && accessModes.size() > 0) {
                accModes.clear();
                for (Object mode : accessModes) {
                    accModes.addElement(mode);
                }
            } else if (accModes.contains(CGProCLI.encodeString(CommuniGateConnector.ACCESS_MODES_NONE))) {
                accModes.clear();
                String accModesString = config.getDefaultAccModes();
                String[] modes = accModesString.split(",");
                for (String mode : modes) {
                    accModes.addElement(mode);
                }
            } else if (accModes.isEmpty()) {
                accModes.addElement(CommuniGateConnector.ACCESS_MODES_DEFAULT);
            }
            settings.put(CommuniGateConnector.ACCESS_MODES_SETTING, Utils.transformAccModes(accModes));

            cli.setAccountSettings(accountUid + "@" + config.getDomainName(), settings);
        } catch (Exception e) {
            log.error("Exception during updating account, error code: " + cli.getErrCode());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("rawtypes")
    private void setAlias(List aliases, String accountUid){
        try {
            if (aliases != null) {
                if (aliases.size() == 1 && ((String) aliases.get(0)).isEmpty()) {
                    log.info("Setting empty Vector.");
                    cli.setAccountAliases(accountUid + "@" + config.getDomainName(), new Vector<>());
                } else {
                    Vector aliasesVector = new Vector(aliases);
                    cli.setAccountAliases(accountUid + "@" + config.getDomainName(), aliasesVector);
                }
            }
        } catch (CGProException e) {
            log.error("Exception during updating account aliases, error code: " + cli.getErrCode());
            e.printStackTrace();
        }
    }

    private void rename(String accountUid, String uidValue){
        if (!uidValue.equals(accountUid)) {
            try {
                cli.renameAccount(uidValue + "@" + config.getDomainName(), accountUid + "@" + config.getDomainName());
            } catch (Exception e) {
                log.error("Exception during renaming account, error code: " + cli.getErrCode());
                e.printStackTrace();
            }
        }
    }

    private void passwordChange(GuardedString password, String uidValue){
        if (password != null) {
            try {
                cli.setAccountPassword(uidValue + "@" + config.getDomainName(), Utils.asString(password));
            } catch (Exception e) {
                log.error("Exception during changing password, error code: " + cli.getErrCode());
                e.printStackTrace();
            }
        }
    }
}
