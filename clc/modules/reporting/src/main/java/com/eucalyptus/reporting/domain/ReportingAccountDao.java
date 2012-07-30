package com.eucalyptus.reporting.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * <p>ReportingAccountDao is an object for reading ReportingAccount objects from the
 * database.
 *
 * @author tom.werges
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

