package com.eucalyptus.reporting.domain;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * <p>ReportingUserDao is an object for reading ReportingUser objects from the
 * database.
 *
 * @author tom.werges
 */
public class ReportingUserDao
{
	private static Logger LOG = Logger.getLogger( ReportingUserDao.class );

	private static ReportingUserDao instance = null;
	
	public static synchronized ReportingUserDao getInstance()
	{
		if (instance == null) {
			instance = new ReportingUserDao();
			instance.loadFromDb();
		}
		return instance;
	}
	
	private ReportingUserDao()
	{
		
	}

	private final Map<String,ReportingUser> users = new HashMap<String,ReportingUser>();

	public ReportingUser getReportingUser(String userId)
	{
		return users.get(userId);
	}
	
	public List<ReportingUser> getReportingUsersByAccount(String accountId)
	{
		List<ReportingUser> rv = new ArrayList<ReportingUser>();
		for (ReportingUser user: users.values()) {
			if (user.getAccountId().equals(accountId)) {
				rv.add(user);
			}
		}
		return rv;
	}
	
	private void loadFromDb()
	{
		LOG.debug("Load users from db");

		EntityWrapper<ReportingUser> entityWrapper =
			EntityWrapper.get(ReportingUser.class);

		try {
			@SuppressWarnings("rawtypes")
			List reportingUsers = (List)
			entityWrapper.createQuery("from ReportingUser")
			.list();

			for (Object obj: reportingUsers) {
				ReportingUser user = (ReportingUser) obj;
				users.put(user.getId(), user);
				LOG.debug("load user from db, id:" + user.getId() + " name:" + user.getName());
			}

			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}			
	}
	
	void putCache(ReportingUser user)
	{
		users.put(user.getId(), user);
	}

}

