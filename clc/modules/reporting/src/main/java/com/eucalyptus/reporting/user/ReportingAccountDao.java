package com.eucalyptus.reporting.user;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

public class ReportingAccountDao
{
	private static Logger log = Logger.getLogger( ReportingUserDao.class );

	private static ReportingAccountDao instance = null;
	
	public static ReportingAccountDao getInstance()
	{
		if (instance == null) {
			instance = new ReportingAccountDao();
		}
		return instance;
	}
	
	private final Map<String,String> accounts = new HashMap<String,String>();
	
	private ReportingAccountDao()
	{
		
	}

	public void addUpdateAccount(String id, String name)
	{

		loadIfNeeded();
		if (accounts.containsKey(id) && accounts.get(id).equals(name)) {
			return;
		} else if (accounts.containsKey(id)) {
			update(id, name);
		} else {
			add(id, name);
		}
		
	}

	public String getAccountName(String id)
	{
		loadIfNeeded();
		return accounts.get(id);
	}
	

	
	private void loadIfNeeded()
	{
		if (accounts.keySet().size() == 0) {

			EntityWrapper<ReportingAccount> entityWrapper =
				EntityWrapper.get(ReportingAccount.class);

			try {
				@SuppressWarnings("rawtypes")
				List reportingAccounts = (List)
					entityWrapper.createQuery("from ReportingAccount")
					.list();

				for (Object obj: reportingAccounts) {
					ReportingAccount Account = (ReportingAccount) obj;
					accounts.put(Account.getId(), Account.getName());
				}
				
				entityWrapper.commit();
			} catch (Exception ex) {
				log.error(ex);
				entityWrapper.rollback();
				throw new RuntimeException(ex);
			}			
		}
	}
	
	private void update(String id, String name)
	{
		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			ReportingAccount reportingAccount = (ReportingAccount)
			entityWrapper.createQuery("from ReportingAccount where id = ?")
				.setString(0, id)
				.uniqueResult();
			reportingAccount.setName(name);
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}			
	}
	
	private void add(String id, String name)
	{
		EntityWrapper<ReportingAccount> entityWrapper =
			EntityWrapper.get(ReportingAccount.class);

		try {
			entityWrapper.add(new ReportingAccount(id, name));
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

}
