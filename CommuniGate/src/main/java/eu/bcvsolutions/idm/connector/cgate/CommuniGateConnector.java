/**
 * CzechIdM
 * Copyright (C) 2014 BCV solutions s.r.o., Czech Republic
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License 2.1 as published by the Free Software Foundation;
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301 USA
 *
 * You can contact us on website http://www.bcvsolutions.eu.
 */

package eu.bcvsolutions.idm.connector.cgate;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stalker.CGPro.CGProCLI;

import eu.bcvsolutions.idm.connector.cgate.create.CreateAccount;
import eu.bcvsolutions.idm.connector.cgate.execute.ExecuteQuery;
import eu.bcvsolutions.idm.connector.cgate.execute.ExecuteQueryAccount;
import eu.bcvsolutions.idm.connector.cgate.execute.ExecuteQueryGroup;
import eu.bcvsolutions.idm.connector.cgate.update.Update;
import eu.bcvsolutions.idm.connector.cgate.update.UpdateAccount;
import eu.bcvsolutions.idm.connector.cgate.update.UpdateGroup;
import eu.bcvsolutions.idm.connector.cgate.utils.Utils;

/**
 * Class for CGate connector.
 *
 * @author Vojtěch Matocha
 * @author Roman Kucera
 * @author Marek Klement
 */
@ConnectorClass(displayNameKey="CommuniGate_Connector",
		configurationClass = CommuniGateConfiguration.class)
public class CommuniGateConnector implements Connector, CreateOp, DeleteOp, SearchOp<String>,
		UpdateOp, SchemaOp, TestOp, AuthenticateOp {

	//
	public static final String REAL_NAME_ATTR = "realName";
	public static final String PASSWORD_ATTR = "__PASSWORD__";
	public static final String ACCOUNT_ATTR = "account";
	public static final String DISABLED_ATTR = "disabled";
	public static final String ACCESS_MODES_ATTR = "accessModes";
	public static final String UNIT_ATTR = "unit";
	public static final String CITY_ATTR = "city";
	public static final String ALIASES_ATTR = "aliases";
	//
	public static final String ACCESS_MODES_SETTING = "AccessModes";
	public static final String UNIT_SETTING = "ou";
	public static final String REAL_NAME_SETTING = "RealName";
	public static final String CITY_SETTING = "l";
	public static final String MAIL_STORAGE_LIMIT_SETTING = "MaxAccountSize";
	public static final String MAIL_BOX_LIMIT_SETTING = "MaxMailboxes";
	//
	public static final String GROUP_NAME = "__NAME__";
	public static final String GROUP_SUBSCRIBERS = "subscribers";
	public static final String GROUP_ID = "id";

	public static final String ACCESS_MODES_DEFAULT = "default";
	public static final String ACCESS_MODES_NONE = "None";

	// Reserved words for access modes settings.
	public static final String[] ACCESS_MODES_RESERVED_VALUES = new String[] { "All", ACCESS_MODES_NONE, ACCESS_MODES_DEFAULT };

	private CommuniGateConfiguration config = null;

	// CGate client
	private CGProCLI cli = null;
	private static Schema schema = null;

	//Logger
	private static final Logger log = LoggerFactory.getLogger(CommuniGateConnector.class);

	public CommuniGateConnector() {
	}

	public Uid authenticate(ObjectClass arg0, String uid, GuardedString password,
							OperationOptions arg3) {
		log.info("CGate connector - authenticate");
		boolean result = false;
		try {
			// verify accounts password
			result = cli.verifyAccountPassword(uid, Utils.asString(password));
		} catch (Exception e) {
			log.error("Exception during authentication, error code: [{}]", cli.getErrCode());
			e.printStackTrace();
		}
		if (result) {
			return new Uid(uid);
		} else {
			return null;
		}
	}

	/**
	 * Test connection method
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void test() {
		log.info("CGate connector - test");
		if (cli == null) {
			throw new RuntimeException("Connection to CommuniGate not bound.");
		}
		try {
			// list accounts
			Hashtable accounts = cli.listAccounts();
			if (accounts == null) {
				log.error("Connection to CommuniGate failed.");
			}
		} catch (Exception e) {
			log.error("Exception during test, error message: [{}]", cli.getErrCode());
			e.printStackTrace();
		}
	}

	/**
	 * Method for generating schema
	 */
	@Override
	public Schema schema() {
		log.info("CGate connector - schema");
		if (schema != null) {
			return schema;
		}
		final SchemaBuilder schemaBuilder = new SchemaBuilder(getClass());

		Set<AttributeInfo> attributesInfoSet = new HashSet<AttributeInfo>();
		ObjectClassInfo info = null;

		Set<AttributeInfo.Flags> flagSet = new TreeSet<AttributeInfo.Flags>();
		attributesInfoSet.add(AttributeInfoBuilder.build(UNIT_ATTR, String.class, flagSet));


		flagSet.add(AttributeInfo.Flags.REQUIRED);

		attributesInfoSet.add(AttributeInfoBuilder.build(REAL_NAME_ATTR, String.class, flagSet));
		attributesInfoSet.add(AttributeInfoBuilder.build(ACCOUNT_ATTR, String.class, flagSet));
		attributesInfoSet.add(AttributeInfoBuilder.build(PASSWORD_ATTR, GuardedString.class, flagSet));
		attributesInfoSet.add(AttributeInfoBuilder.build(DISABLED_ATTR, Boolean.class, flagSet));

		flagSet = new TreeSet<AttributeInfo.Flags>();
		flagSet.add(AttributeInfo.Flags.MULTIVALUED);
		attributesInfoSet.add(AttributeInfoBuilder.build(ACCESS_MODES_ATTR, String.class, flagSet));
		attributesInfoSet.add(AttributeInfoBuilder.build(ALIASES_ATTR, String.class, flagSet));

		info = new ObjectClassInfoBuilder().addAllAttributeInfo(attributesInfoSet).build();

		// group object
		ObjectClassInfoBuilder groupObjectClassBuilder = new ObjectClassInfoBuilder();
		groupObjectClassBuilder.setType(ObjectClass.GROUP_NAME);
		groupObjectClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(GROUP_NAME, String.class));
		groupObjectClassBuilder.addAttributeInfo(
				AttributeInfoBuilder.define(GROUP_SUBSCRIBERS).setMultiValued(true).setType(String.class).build());
		groupObjectClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(GROUP_ID, String.class));

		schemaBuilder.defineObjectClass(info);
		schemaBuilder.defineObjectClass(groupObjectClassBuilder.build());
		schema = schemaBuilder.build();
		return schema;
	}

	/**
	 * Update account on the system
	 */
	@Override
	public Uid update(ObjectClass oclass, Uid uid, Set<Attribute> attributes,
					  OperationOptions options) {

		log.info("CGate connector - update");
		Update update;
		if (oclass.getObjectClassValue().equals(ObjectClass.ACCOUNT_NAME)) {
			update = new UpdateAccount(cli, config);
		} else if(oclass.getObjectClassValue().equals(ObjectClass.GROUP_NAME)) {
			update = new UpdateGroup(cli);
		} else {
			throw new IllegalArgumentException("Something went terribly wrong!");
		}
		return update.update(uid, attributes, options);
	}

	public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions opt) {
		log.info("CGate connector - createFilterTranslator");
		if (oclass.is(ObjectClass.ACCOUNT_NAME) || oclass.is(ObjectClass.GROUP_NAME)) {
			return new AbstractFilterTranslator<String>() {
				/**
				 * Method returns object
				 */
				@Override
				protected String createEqualsExpression(EqualsFilter filter, boolean not) {
					if (not) {
						throw new UnsupportedOperationException("This type of equals expression is not allow for now.");
					}

					Attribute attr = filter.getAttribute();

					if (attr == null || !attr.is(Uid.NAME)) {
						throw new IllegalArgumentException("Attribute is null or not UID attribute.");
					}

					return ((Uid)attr).getUidValue();
				}
			};
		}
		return null;
	}

	/**
	 * Return list of accounts for query
	 */
	@Override
	public void executeQuery(ObjectClass oclass, String query,
							 ResultsHandler handler, OperationOptions opt) {
		log.info("CGate connector - executeQuery");
		ExecuteQuery executeQuery;
		if (oclass.getObjectClassValue().equals(ObjectClass.ACCOUNT_NAME)) {
			executeQuery = new ExecuteQueryAccount(cli,config);
		} else if(oclass.getObjectClassValue().equals(ObjectClass.GROUP_NAME)){
			executeQuery = new ExecuteQueryGroup(cli,config);
		} else {
			throw new IllegalArgumentException("Object type not supported in executeQuery!");
		}
		executeQuery.execute(query,handler,opt);
	}

	@Override
	public void delete(ObjectClass arg0, Uid arg1, OperationOptions arg2) {
		log.info("CGate connector - delete");
		if (arg0.getObjectClassValue().equals(ObjectClass.ACCOUNT_NAME)) {
			try {
				cli.deleteAccount(arg1.getUidValue() + "@" + config.getDomainName());
			} catch (Exception e) {
				String mail = arg1.getUidValue() + "@" + config.getDomainName();
				log.error("ExceptioCommuniGateConnectorn during delete: [{}]", mail);
				e.printStackTrace();
			}
		} else {
			throw new IllegalArgumentException("Object type not supported in delete!");
		}
	}

	/**
	 * Creates new account
	 */
	@Override
	public Uid create(ObjectClass objectClass, Set<Attribute> attributes,
					  OperationOptions opt) {
		log.info("CommuniGateConnector - create");
		if (objectClass.getObjectClassValue().equals(ObjectClass.ACCOUNT_NAME)) {
			CreateAccount createAccount = new CreateAccount(cli,config);
			return createAccount.create(attributes);
		} else {
			throw new IllegalArgumentException("Object type not supported in create!");
		}
	}

	/**
	 * Log out
	 */
	@Override
	public void dispose() {
		log.info("CommuniGateConnector - dispose");
		if(cli!=null) {
			cli.logout();
		}
		log.info("CommuniGateConnector - dispose finished");
	}

	@Override
	public Configuration getConfiguration() {
		log.info("CommuniGateConnector - get configuration");
		return config;
	}

	/**
	 * Login to client and session begins
	 */
	@Override
	public void init(Configuration cfg) {
		log.info("CommuniGateConnector - init");
		config = (CommuniGateConfiguration) cfg;
		try {
			cli = createCGProCLI(config);
		} catch (Exception ex){
			log.error("Exception during initialization.");
			ex.printStackTrace();
		}
		log.info("CommuniGateConnector - init - finished");
	}

	private CGProCLI createCGProCLI(CommuniGateConfiguration config) {
		String host = config.getAddress();
		int port = config.getPort();
		String username = config.getUsername();
		GuardedString password = config.getPassword();
		try {
			return new CGProCLI(host, port, username, Utils.asString(password));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
