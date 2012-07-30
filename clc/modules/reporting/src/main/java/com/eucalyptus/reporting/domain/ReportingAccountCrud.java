package com.eucalyptus.reporting.domain;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * <p>ReportingAccountCrud is an object for CReating, Updating, and Deleting accounts. This class should
 * be used instead of creating Account hibernate objects and storing them directly.
 *
 * @author tom.werges
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
		
	private ReportingAccountCrud()
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
