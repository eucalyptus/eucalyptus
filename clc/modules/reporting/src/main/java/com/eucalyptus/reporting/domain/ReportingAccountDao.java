/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.reporting.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * <p>ReportingAccountDao is an object for reading ReportingAccount objects from the
 * database.
 */
public class ReportingAccountDao
{
	private static Logger LOG = Logger.getLogger( ReportingAccountDao.class );

	private static ReportingAccountDao instance = null;
	
	public static synchronized ReportingAccountDao getInstance()
	{
		if (instance == null) {
			instance = new ReportingAccountDao();
			instance.loadFromDb();
		}
		return instance;
	}
	
	private final Map<String,ReportingAccount> accounts =
		new ConcurrentHashMap<String,ReportingAccount>();

	private ReportingAccountDao()
	{
		
	}

	public ReportingAccount getReportingAccount(String accountId)
	{
		return accounts.get(accountId);
	}
	
	private void loadFromDb()
	{
		LOG.debug("Load accounts from db");
		
		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			@SuppressWarnings("rawtypes")
			List reportingAccounts = (List)
				entityWrapper.createQuery("from ReportingAccount")
				.list();

			for (Object obj: reportingAccounts) {
				ReportingAccount account = (ReportingAccount) obj;
				accounts.put(account.getId(), account);
				LOG.debug("load account from db, id:" + account.getId() + " name:" + account.getName());
			}
				
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}			
	}
	
	void putCache(ReportingAccount account)
	{
		accounts.put(account.getId(), account);
	}
	

}

