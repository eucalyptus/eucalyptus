package com.eucalyptus.reporting.user;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;

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

	private final Map<String,String> users = new HashMap<String,String>();

	private ReportingUserDao()
	{

	}

	public void addUpdateUser(String id, String name)
	{
		if (id==null || name==null) throw new IllegalArgumentException("args cant be null");

		if (users.containsKey(id) && users.get(id).equals(name)) {
			return;
		} else if (users.containsKey(id)) {
			users.put(id, name);
			updateInDb(id, name);
		} else {
			try {			
				addToDb(id, name);
				users.put(id, name);
			} catch (RuntimeException e) {
				LOG.error(e);
			}
		}

	}

	public String getUserName(String id)
	{
		return users.get(id);
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
				users.put(user.getId(), user.getName());
				LOG.debug("load user from db, id:" + user.getId() + " name:" + user.getName());
			}

			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
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

	private void addToDb(String id, String name)
	{
		LOG.debug("Add reporting user to db, id:" + id + " name:" + name);

		EntityWrapper<ReportingUser> entityWrapper =
			EntityWrapper.get(ReportingUser.class);

		try {
			entityWrapper.add(new ReportingUser(id, name));
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}					
	}

}
