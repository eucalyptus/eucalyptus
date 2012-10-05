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

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * <p>ReportingAccountCrud is an object for Creating, Updating, and Deleting accounts. This class should
 * be used instead of creating Account hibernate objects and storing them directly.
 */
public class ReportingAccountCrud
{
	private static Logger LOG = Logger.getLogger( ReportingAccountCrud.class );

	private static ReportingAccountCrud instance = null;
	
	public static synchronized ReportingAccountCrud getInstance()
	{
		if (instance == null) {
			instance = new ReportingAccountCrud();
		}
		return instance;
	}
		
	protected ReportingAccountCrud()
	{
		
	}

	/**
 	 * Create or update an account. This method can be called repeatedly every time an event is
 	 * received that has account data. This method will update the account with a new name if
 	 * the name has changed, or will do nothing if the account already exists with the supplied
 	 * name.
 	 */
	public void createOrUpdateAccount(String id, String name)
	{
		if (id==null || name==null) throw new IllegalArgumentException("args cant be null");

		LOG.debug("createOrUpdateAccount id:" + id + " name:" + name);
		if (id.matches("\\D+") || name.matches("\\d+")) {
			LOG.error("Funnny account, id:" + id + " name:" + name, new IllegalArgumentException());
		}
		
		ReportingAccount account = new ReportingAccount(id, name);
		ReportingAccount oldAccount = ReportingAccountDao.getInstance().getReportingAccount(account.getId());
		if (oldAccount!=null && oldAccount.getName().equals(account.getName())) {
			return;
		} else if (oldAccount!=null) {
			updateInDb(account);
			ReportingAccountDao.getInstance().putCache(account);
		} else {
			try {
				addToDb(account);
				ReportingAccountDao.getInstance().putCache(account);
			} catch (RuntimeException e) {
				LOG.error(e);
			}
		}

	}
	
	private void updateInDb(ReportingAccount account)
	{
		LOG.debug("Update reporting account in db, account:" + account);

		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			ReportingAccount reportingAccount = (ReportingAccount)
			entityWrapper.createQuery("from ReportingAccount where id = ?")
				.setString(0, account.getId())
				.uniqueResult();
			reportingAccount.setName(account.getName());
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}			
	}
	
	private void addToDb(ReportingAccount account)
	{
		LOG.debug("Add reporting account to db, account:" + account);
		
		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			entityWrapper.add(account);
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

}
