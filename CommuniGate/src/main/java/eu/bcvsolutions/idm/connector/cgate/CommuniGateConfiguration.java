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

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Configuration of CGate connector.
 *
 * @author Vojtěch Matocha
 */
public class CommuniGateConfiguration extends AbstractConfiguration {

	public CommuniGateConfiguration() {

	}

	private String address;
	private int port;
	private String username;
	private GuardedString password;
	private String domainName;

	// List of all allowed access points which have to be in setting for new account.
	private String defaultAccModes;

	// Size of newly created storages
	private String defaultMailStorageLimit;

	// max number of mailboxes
	private String defaultMailBoxLimit;

	@Override
	public void validate() {
		if (StringUtil.isBlank(getAddress())) {
			throw new IllegalArgumentException("Hostname must be set.");
		}
		if (getPort() < 0 || getPort() >= 65535) {
			throw new IllegalArgumentException("Port must be in range between 1 to 65535.");
		}
		if (StringUtil.isBlank(getUsername())) {
			throw new IllegalArgumentException("Username must be specified.");
		}
	}

	@ConfigurationProperty(order = 1,
			displayMessageKey = "CGATE_HOST_NAME",
			helpMessageKey = "CGATE_HOST_HELP",
			required = true)
	public String getAddress() {
		return address;
	}

	@ConfigurationProperty(order = 2,
			displayMessageKey = "CGATE_PORT_NAME",
			helpMessageKey = "CGATE_PORT_HELP",
			required = true)
	public int getPort() {
		return port;
	}

	@ConfigurationProperty(order = 3,
			displayMessageKey = "CGATE_USER_NAME",
			helpMessageKey = "CGATE_USER_HELP",
			required = true)
	public String getUsername() {
		return username;
	}

	@ConfigurationProperty(order = 4,
			displayMessageKey = "CGATE_USER_PASSWORD_NAME",
			helpMessageKey = "CGATE_USER_PASSWORD_HELP",
			confidential = true)
	public GuardedString getPassword() {
		return password;
	}

	@ConfigurationProperty(order = 5,
			displayMessageKey = "CGATE_DOMAIN_NAME_NAME",
			helpMessageKey = "CGATE_DOMAIN_NAME_HELP")
	public String getDomainName() {
		return domainName;
	}

	@ConfigurationProperty(order = 6,
			displayMessageKey = "CGATE_DEFAULT_ACC_MODES_NAME",
			helpMessageKey = "CGATE_DEFAULT_ACC_MODES_HELP")
	public String getDefaultAccModes() {
		return defaultAccModes;
	}

	@ConfigurationProperty(order = 7,
			displayMessageKey = "CGATE_DEFAULT_MAIL_STORAGE_LIMIT_NAME",
			helpMessageKey = "CGATE_DEFAULT_MAIL_STORAGE_LIMIT_HELP")
	public String getDefaultMailStorageLimit() {
		return defaultMailStorageLimit;
	}

	@ConfigurationProperty(order = 8,
			displayMessageKey = "CGATE_DEFAULT_MAIL_BOX_LIMIT_NAME",
			helpMessageKey = "CGATE_DEFAULT_MAIL_BOX_LIMIT_HELP")
	public String getDefaultMailBoxLimit() {
		return defaultMailBoxLimit;
	}

	public String getMessage(String key) {
		return getConnectorMessages().format(key, key);
	}

	public String getMessage(String key, Object... objects) {
		return getConnectorMessages().format(key, key, objects);
	}

	public void setDefaultMailStorageLimit(String defaultMailStorageLimit) {
		this.defaultMailStorageLimit = defaultMailStorageLimit;
	}

	public void setDefaultMailBoxLimit(String defaultMailBoxLimit) {
		this.defaultMailBoxLimit = defaultMailBoxLimit;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(GuardedString password) {
		this.password = password;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public void setDefaultAccModes(String modes) {
		this.defaultAccModes = modes;
	}

}
