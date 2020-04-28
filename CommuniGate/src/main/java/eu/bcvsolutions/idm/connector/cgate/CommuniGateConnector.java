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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
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

import com.stalker.CGPro.CGProCLI;
import com.stalker.CGPro.CGProException;

/**
 * Třída implementující funkcionalitu poskytovanou CGate konektorem.
 *
 * @author Vojtěch Matocha
 * @author Roman Kucera
 */
@ConnectorClass(displayNameKey="CommuniGate_Connector",
		configurationClass = CommuniGateConfiguration.class)
public class CommuniGateConnector implements Connector, CreateOp, DeleteOp, SearchOp<String>,
		UpdateOp, SchemaOp, TestOp, AuthenticateOp {

	/* ==================================================
	 * Názvy atributů pro konektor
	 */
	private static final String REAL_NAME_ATTR = "realName";
	private static final String PASSWORD_ATTR = "__PASSWORD__";
	private static final String ACCOUNT_ATTR = "account";
	private static final String DISABLED_ATTR = "disabled";
	private static final String ACCESS_MODES_ATTR = "accessModes";
	private static final String UNIT_ATTR = "unit";
	private static final String CITY_ATTR = "city";
	private static final String ALIASES_ATTR = "aliases";
	//====================================================

	/* ===================================================
	 * Názvy nastavení v CGate
	 */
	private static final String ACCESS_MODES_SETTING = "AccessModes";
	private static final String UNIT_SETTING = "ou";
	private static final String REAL_NAME_SETTING = "RealName";
	private static final String CITY_SETTING = "l";
	private static final String MAIL_STORAGE_LIMIT_SETTING = "MaxAccountSize";
	private static final String MAIL_BOX_LIMIT_SETTING = "MaxMailboxes";


	private static final String ACCESS_MODES_DEFAULT = "default";
	private static final String ACCESS_MODES_NONE = "None";
	/**
	 * Reserved words for access modes settings.
	 */
	private static final String[] ACCESS_MODES_RESERVED_VALUES = new String[] { "All", ACCESS_MODES_NONE, ACCESS_MODES_DEFAULT };

	/**
	 * Konfigurace
	 */
	private CommuniGateConfiguration config = null;

	/**
	 * Objekt, jehož prostřednictvím jsou volány webové služby CGate
	 */
	private CGProCLI cli = null;
	private static Schema schema = null;

	//Logger
	Log log = Log.getLog(CommuniGateConnector.class);

	/**
	 * Implicitní konstruktor.
	 */
	public CommuniGateConnector() {
	}

	/**
	 * Autentizační metoda. Vrací null, pokud je autentizace neúspěšná. Jinak vrací uid
	 * autentizovaného uživatele.
	 */
	public Uid authenticate(ObjectClass arg0, String uid, GuardedString password,
							OperationOptions arg3) {
		log.info("CGate connector - authenticate");
		boolean result = false;
		try {
			//ověření hesla pomocí webové služby
			result = cli.verifyAccountPassword(uid, asString(password));
		} catch (Exception e) {
			log.error("Exception during authentication, error code: " + cli.getErrCode());
			e.printStackTrace();
		}
		if (result) {
			return new Uid(uid);
		} else {
			return null;
		}
	}

	/**
	 * Metoda sloužící k ověření spojení. Vylistuje účty a pokud se nějaké vrátí (ne null), tak
	 * je test považován za úspěšný.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void test() {
		log.info("CGate connector - test");
		if (cli == null) {
			throw new RuntimeException("Connection to CommuniGate not bound.");
		}
		try {
			//listuji účty
			Hashtable accounts = cli.listAccounts();
			if (accounts == null) {
				log.error("Connection to CommuniGate failed.");
			}
		} catch (Exception e) {
			log.error("Exception during test, error message: " + cli.getErrCode());
			e.printStackTrace();
		}
	}

	/**
	 * Metoda, která vrací schéma konektoru. Nijak se k tomu nepoužívá napojení na běžící CGate,
	 * schéma, které konektor vrací, je "zadrátované" a statické.
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

		schemaBuilder.defineObjectClass(info);
		schema = schemaBuilder.build();
		return schema;
	}

	/**
	 * Aktualizace účtu na koncovém systému
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Uid update(ObjectClass oclass, Uid uid, Set<Attribute> attributes,
					  OperationOptions options) {
		log.info("CGate connector - update");
		String accountUid = null;
		String uidValue = (String) uid.getValue().get(0);
		GuardedString password = null;
		String realName = null;
		boolean disabled = false;
		List accessModes = null;
		String unit = null;
		String city = null;
		List aliases = null;

		//zpracuji atributy, ktere jsem dostal na vstupu, do promennych
		for (Attribute attr : attributes) {
			String name = attr.getName();
			if (ACCOUNT_ATTR.equals(name)) {
				accountUid = (String) attr.getValue().get(0);
			} else if (PASSWORD_ATTR.equals(name)) {
				password = (GuardedString) attr.getValue().get(0);
			} else if (REAL_NAME_ATTR.equals(name)) {
				realName = (String) attr.getValue().get(0);
			} else if (DISABLED_ATTR.equals(name)) {
				disabled = (Boolean) attr.getValue().get(0);
			} else if (ACCESS_MODES_ATTR.equals(name)) {
				accessModes = attr.getValue();
			} else if (UNIT_ATTR.equals(name)) {
				unit = (String) attr.getValue().get(0);
			} else if (CITY_ATTR.equals(name)) {
				city = (String) attr.getValue().get(0);
			} else if (ALIASES_ATTR.equals(name)) {
				aliases = attr.getValue();
			}
		}

		//osetrime zmenu hesla
		if (password != null) {
			try {
				cli.setAccountPassword(uidValue + "@" + config.getDomainName(), asString(password));
			} catch (Exception e) {
				log.error("Exception during changing password, error code: " + cli.getErrCode());
				e.printStackTrace();
			}
		}



		//osetrime prejmenovani
		if (!uidValue.equals(accountUid)) {
			try {
				cli.renameAccount(uidValue + "@" + config.getDomainName(), accountUid + "@" + config.getDomainName());
			} catch (Exception e) {
				log.error("Exception during renaming account, error code: " + cli.getErrCode());
				e.printStackTrace();
			}
		}


		//nastaveni aliasu
		try {
			if (aliases != null) {
				if (aliases.size() == 1 && ((String)aliases.get(0)).isEmpty()) {
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

		//nastaveni uctu
		Hashtable settings = null;
		try {
			settings = cli.getAccountSettings(accountUid + "@" + config.getDomainName());
			if (realName != null) {
				settings.put(REAL_NAME_SETTING, CGProCLI.encodeString(realName));
			}
			if (unit != null) {
				settings.put(UNIT_SETTING, CGProCLI.encodeString(unit));
			}
			if (city != null) {
				settings.put(CITY_SETTING, CGProCLI.encodeString(city));
			}

			//nastaveni pristupovych metod
			Vector accModes = asVector(settings.get(ACCESS_MODES_SETTING));
			if (accModes == null) {
				accModes = new Vector();
			}

			//zablokovani vyresime odebranim vsech pristupovych metod
			if (disabled) {
				accModes.clear();
				accModes.addElement(ACCESS_MODES_NONE);
			} else if (accessModes != null && accessModes.size() > 0) {
				//prepiseme pristupove metody tim, co bylo poslano
				accModes.clear();
				for (Object mode : accessModes) {
					accModes.addElement(mode);
				}
			} else if (accModes.contains(CGProCLI.encodeString(ACCESS_MODES_NONE))) {
				//zablokovanemu uctu pro odblokovani opet nastavime puvodni pristupove metody
				accModes.clear();
				String accModesString = config.getDefaultAccModes();
				String[] modes = accModesString.split(",");
				for (String mode : modes) {
					accModes.addElement(mode);
				}
			} else if (accModes.isEmpty()) {
				// prazdne accModes odpovidaji nastaveni "default"
				accModes.addElement(ACCESS_MODES_DEFAULT);
			}
			settings.put(ACCESS_MODES_SETTING, transformAccModes(accModes));

			//webova sluzba, ktera nastavi ucet
			cli.setAccountSettings(accountUid + "@" + config.getDomainName(), settings);
		} catch (Exception e) {
			log.error("Exception during updating account, error code: " + cli.getErrCode());
			e.printStackTrace();
		}


		return new Uid(accountUid);
	}

	/**
	 * Transforms access modes for use in CLI setAccountSettings method.
	 * Returns unchanged Vector, or String if it is in reserved words - see {@link #ACCESS_MODES_RESERVED_VALUES}.
	 *
	 * @param accModes access modes
	 * @return transformed access modes for CLI
	 */
	@SuppressWarnings("rawtypes")
	private Object transformAccModes(Vector accModes) {
		if (accModes != null && accModes.size() == 1) {
			String mode = (String) accModes.get(0);
			if (Arrays.asList(ACCESS_MODES_RESERVED_VALUES).contains(mode)) {
				return mode;
			}
		}
		return accModes;
	}

	public FilterTranslator<String> createFilterTranslator(ObjectClass oclass,
														   OperationOptions opt) {
		log.info("CGate connector - createFilterTranslator");
		if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
			return new AbstractFilterTranslator<String>() {
				/**
				 * Metoda vytvářející dotaz pro vyhledání daného objektu. V tomto případě si necháme navrátit
				 * jen uid identifikátor daného objektu.
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
	 * Vraci seznam uctu na pripojenem systemu
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void executeQuery(ObjectClass oclass, String query,
							 ResultsHandler handler, OperationOptions opt) {
		log.info("CGate connector - executeQuery");
		ConnectorObject object = null;
		if (StringUtil.isBlank(query)) {
			//Vylistovat vsechny objekty dane tridy.
			try {
				Hashtable accounts = cli.listAccounts(config.getDomainName());
				for(Object key : accounts.keySet()) {
					//na kazdy ucet zavolam get a zaradim ho mezi nalezene ucty
					object = getConnectorObject((String) key);
					if (object != null) {
						handler.handle(object);
					}
				}
			} catch(Exception e) {
				log.error("Exception during listing accounts, error code: " + cli.getErrCode());
				e.printStackTrace();
			}
		} else {
			//Vylistovat pouze zaznam odpovidajici zadanemu uid.
			object = getConnectorObject(query);
			if (object != null) {
				handler.handle(object);
			}
		}

	}

	/**
	 * Načte atributy daného účtu
	 * @param uid identifikátor účtu (vše před zavináčem)
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private ConnectorObject getConnectorObject(String uid) {
		log.info("CGate connector - getObject, uid: " + uid);
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
		builder.setName(uid);
		builder.setUid(uid);
		builder.setObjectClass(ObjectClass.ACCOUNT);
		Hashtable settings;
		try {
			//Načtu aktuální nastavení účtu
			String email = uid + "@" + config.getDomainName();
			settings = cli.getAccountSettings(email);
			//log.info("CGate connector - getObject, settings ==========");
			//for (Object key : settings.keySet()) {
			//	log.info("CGate connector - getObject, settingKey: " + key.toString());
			//}

			//Načtu popisné atributy a přístupové módy
			String realName = (String) settings.get(REAL_NAME_SETTING);
			String unit = (String) settings.get(UNIT_SETTING);
			String city = (String) settings.get(CITY_SETTING);
			Vector accModes = asVector(settings.get(ACCESS_MODES_SETTING));
			if (accModes == null || !accModes.contains(ACCESS_MODES_NONE)) {
				builder.addAttribute(AttributeBuilder.build(DISABLED_ATTR, false));
			} else {
				builder.addAttribute(AttributeBuilder.build(DISABLED_ATTR, true));
			}
			if ((accModes != null) && !accModes.isEmpty()) {
				builder.addAttribute(AttributeBuilder.build(ACCESS_MODES_ATTR, accModes.subList(0, accModes.size())));
			}
			if (realName != null) {
				builder.addAttribute(AttributeBuilder.build(REAL_NAME_ATTR, CGProCLI.decodeString(realName)));
			}
			if (unit != null) {
				builder.addAttribute(AttributeBuilder.build(UNIT_ATTR, CGProCLI.decodeString(unit)));
			}
			if (city != null) {
				builder.addAttribute(AttributeBuilder.build(CITY_ATTR, CGProCLI.decodeString(city)));
			}
			builder.addAttribute(AttributeBuilder.build(ACCOUNT_ATTR, uid));

			// Načtení aliasů
			Vector accountAliases = cli.getAccountAliases(email);
			if (accountAliases != null) {
				builder.addAttribute(AttributeBuilder.build(ALIASES_ATTR, accountAliases.subList(0, accountAliases.size())));
			}
		} catch (Exception e) {
			log.error("Exception during getting account " + uid + "@" + config.getDomainName()+ ", error code: " + cli.getErrCode());
			e.printStackTrace();
			return null;
		}
		return builder.build();
	}

	/**
	 * Smazání účtu
	 */
	@Override
	public void delete(ObjectClass arg0, Uid arg1, OperationOptions arg2) {
		log.info("CGate connector - delete");
		try {
			//webová služba, která smaže účet
			cli.deleteAccount(arg1.getUidValue() + "@" + config.getDomainName());
		} catch (Exception e) {
			log.error("ExceptioCommuniGateConnectorn during delete: " + arg1.getUidValue() + "@" + config.getDomainName());
			e.printStackTrace();
		}
	}

	/**
	 * Vytvoří nový účet.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Uid create(ObjectClass objectClass, Set<Attribute> attributes,
					  OperationOptions opt) {
		log.info("CommuniGateConnector - create");

		Hashtable settings = new Hashtable();

		String accountUid = null;
		GuardedString password = null;
		boolean disabled = false;
		List accessModes = null;
		List aliases = null;
		String defaultStorageLimit = config.getDefaultMailStorageLimit();
		String defaultMailboxLimit = config.getDefaultMailBoxLimit();


		if ((defaultStorageLimit != null) && !"".equals(defaultStorageLimit)) {
			settings.put(MAIL_STORAGE_LIMIT_SETTING, defaultStorageLimit);
		}
		if ((defaultMailboxLimit != null) && !"".equals(defaultMailboxLimit)) {
			settings.put(MAIL_BOX_LIMIT_SETTING, defaultMailboxLimit);
		}

		//zpracovávám vstupy do promennych
		for (Attribute attr : attributes) {
			String name = attr.getName();
			if (REAL_NAME_ATTR.equalsIgnoreCase(name)) {
				settings.put(REAL_NAME_SETTING, CGProCLI.encodeString((String) attr.getValue().get(0)));
			} else if (ACCOUNT_ATTR.equals(name)) {
				accountUid = (String) attr.getValue().get(0);
			} else if (PASSWORD_ATTR.equals(name)) {
				password = (GuardedString) attr.getValue().get(0);
			} else if (DISABLED_ATTR.equals(name)) {
				disabled = (Boolean) attr.getValue().get(0);
			} else if (ACCESS_MODES_ATTR.equals(name)) {
				accessModes = attr.getValue();
			} else if (UNIT_ATTR.equals(name)) {
				settings.put(UNIT_SETTING, CGProCLI.encodeString((String) attr.getValue().get(0)));
			} else if (CITY_ATTR.equals(name)) {
				settings.put(CITY_SETTING, CGProCLI.encodeString((String) attr.getValue().get(0)));
			} else if (ALIASES_ATTR.equals(name)) {
				aliases = attr.getValue();
			}
		}

		//ucet vznikne a bude moci pouzivat vsechny sluzby. Pokud je disabled, nebude pouzivat nic.
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
			accModes.addElement(ACCESS_MODES_NONE);
		}
		settings.put(ACCESS_MODES_SETTING, transformAccModes(accModes));

		try {
			if ((accountUid != null) && (!"".equals(accountUid))) {
				//webova sluzba, ktera vytvori ucet
				String email = accountUid + "@" + config.getDomainName();
				cli.createAccount(email,settings,null,false);
				if (password != null) {
					//bylo-li zadano heslo, nastavim ho
					cli.setAccountPassword(email, asString(password));
				}

				//nastaveni aliasu
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

	/**
	 * Odhlášení
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
	 * Login do webové služby, začátek session
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

	/**
	 * Načte z konfigurace parametry připojení a vytvoří objekt, kterým se budou volat webové služby.
	 * @param config konfigurace připojení
	 * @return objekt, kterým lze volat webové služby CGate
	 */
	private CGProCLI createCGProCLI(CommuniGateConfiguration config) {
		String host = config.getAddress();
		int port = config.getPort();
		String username = config.getUsername();
		GuardedString password = config.getPassword();
		//log.error("config: ", host, username, port, password);
		try {
			return new CGProCLI(host, port, username, asString(password));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Statická metoda, která slouží pro transformaci různých objektů na String.
	 *
	 * @param object
	 * @return
	 * Transformace:
	 * 	GuardedString	- transformace na heslo ve Stringu.
	 * 	String[]		- transformace na String, jednotlive radky pole jsou oddleny "\n".
	 * 	Object 			- transformace objektu na String metodou toString.
	 *  jinak			- jinak navrací null.
	 */
	public static String asString(Object object) {
		if (object instanceof GuardedString) {
			GuardedString guarded = (GuardedString)object;
			GuardedStringAccessor accessor = new GuardedStringAccessor();
			guarded.access(accessor);
			char[] result = accessor.getArray();
			return new String(result);
		} else if (object instanceof String [] ) {
			//Transformuje pole Stringu do jednoho Stringu.
			StringBuilder st = new StringBuilder();
			String item = null;

			String[] array = (String[]) object;
			for (int i = 0; i < array.length; i++) {
				item = array[i];
				if (item == null) {
					continue;
				}
				st.append(item);
				st.append("\n");
			}
			return st.toString();
		} else if (object != null) {
			return object.toString();
		} else {
			return null;
		}
	}

	/**
	 * Transforms object as Vector.
	 * Handles object of type String and Vector, otherwise returns empty Vector.
	 *
	 * @param attr input object
	 * @return object as Vector
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Vector asVector(Object attr) {
		if (attr == null) {
			return null;
		}
		if (attr instanceof Vector) {
			return (Vector) attr;
		}
		if (attr instanceof String) {
			Vector vec = new Vector();
			vec.addElement((String) attr);
			return vec;
		}
		log.error("CGate connector - asVector: Unexpected type of attr: " + attr.getClass().getName()
				+ ". Returning empty Vector.");
		return new Vector();
	}
}
