package com.eucalyptus.reporting.domain;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

/**
 * <p>ReportingUserCrud is an object for CReating, Updating, and Deleting users. This class should
 * be used instead of creating User hibernate objects and storing them directly.
 *
 * @author tom.werges
 */
public class ReportingUserCrud
{
	private static Logger LOG = Logger.getLogger( ReportingUserCrud.class );

	private static ReportingUserCrud instance = null;

	public static synchronized ReportingUserCrud getInstance()
	{
		if (instance == null) {
			instance = new ReportingUserCrud();
		}
		return instance;
	}

	private ReportingUserCrud()
	{
	}

	/**
 	 * Create or update a user. This method can be called repeatedly every time an event is
 	 * received that has user data. This method will update the user with a new name if
 	 * the name has changed, or will do nothing if the user already exists with the supplied
 	 * name.
 	 */
	public void createOrUpdateUser(String id, String accountId, String name)
	{
		if (id==null || accountId==null || name==null) throw new IllegalArgumentException("args cant be null");

		ReportingUser user = new ReportingUser(id, accountId, name);
		ReportingUser oldUser = ReportingUserDao.getInstance().getReportingUser(id);
		if (oldUser!=null && oldUser.getName().equals(name)) {
			return;
		} else if (oldUser!=null) {
			updateInDb(id, name);
			ReportingUserDao.getInstance().putCache(user);
		} else {
			try {			
				addToDb(id, accountId, name);
				ReportingUserDao.getInstance().putCache(user);
			} catch (RuntimeException e) {
				LOG.error(e);
			}
		}

	}

	private void updateInDb(String id, String name)
	{
		LOG.debug("Update reporting user in db, id:" + id + " name:" + name);

		EntityWrapper<ReportingUser> entityWrapper =
			EntityWrapper.get(ReportingUser.class);

		try {
			ReportingUser reportingUser = (ReportingUser)
			entityWrapper.createQuery("from ReportingUser where id = ?")
			.setString(0, id)
			.uniqueResult();
			reportingUser.setName(name);
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}			
	}

	private void addToDb(String id, String accountId, String name)
	{
		LOG.debug("Add reporting user to db, id:" + id + " accountId:" + accountId + " name:" + name);

		EntityWrapper<ReportingUser> entityWrapper =
			EntityWrapper.get(ReportingUser.class);

		try {
			entityWrapper.add(new ReportingUser(id, accountId, name));
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

}
