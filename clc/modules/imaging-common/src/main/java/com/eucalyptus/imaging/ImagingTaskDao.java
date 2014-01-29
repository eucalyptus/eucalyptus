/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.entities.EntityWrapper;

/**
 * <p>ImagingTaskCrud is an object for Creating, Updating, and Deleting ImagingTasks. This class should
 * be used instead of creating ImagingTask hibernate objects and storing them directly.</p>
 */
public class ImagingTaskDao
{
	private static Logger LOG = Logger.getLogger( ImagingTaskDao.class );

	private static ImagingTaskDao instance = null;
	
	public static synchronized ImagingTaskDao getInstance()
	{
		if (instance == null) {
			instance = new ImagingTaskDao();
		}
		return instance;
	}
		
	protected ImagingTaskDao() { }

	public List<ImagingTask> loadAllFromDb()
	{
		LOG.debug("Loading ImagingTasks from db");
		
		EntityWrapper<ImagingTask> entityWrapper =
			EntityWrapper.get(ImagingTask.class);
		List<ImagingTask> results;
		try {
			results = entityWrapper.query(new ImagingTask());
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
		return results;
	}
	
	public void updateInDb(ImagingTask task)
	{
		LOG.debug("Updating ImagingTask to db, task:" + task.getId());
		// convert task into JSON
		task.serializeTaskToJSON();
		
		EntityWrapper<ImagingTask> entityWrapper =
			EntityWrapper.get(ImagingTask.class);

		try {
			entityWrapper.merge(task);
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
	}
	
	private static ImagingTask getUniqueTask( EntityWrapper<ImagingTask> db, String taskId ) throws Exception {
	    @SuppressWarnings( "unchecked" )
	    final ImagingTask result = ( ImagingTask ) db
	        .createCriteria( ImagingTask.class ).add( Restrictions.eq( "id", taskId ) )
	        .setCacheable( true )
	        .uniqueResult();
	    if ( result == null ) {
	      throw new NoSuchElementException( "Can not find task " + taskId );
	    }
	    return result;
	  }
	
	public void removeFromDb(ImagingTask task)
	{
		LOG.debug("Removing ImagingTask from db, task:" + task.getId());
		
		EntityWrapper<ImagingTask> entityWrapper =
			EntityWrapper.get(ImagingTask.class);

		try {
			final ImagingTask taskToDelete = getUniqueTask(entityWrapper, task.getId());
			entityWrapper.delete(taskToDelete);
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
	}

	public void addToDb(ImagingTask task)
	{
		LOG.debug("Adding ImagingTask to db, task:" + task.getId());
		// convert task into JSON
		task.serializeTaskToJSON();

		EntityWrapper<ImagingTask> entityWrapper =
			EntityWrapper.get(ImagingTask.class);

		try {
			entityWrapper.add(task);
			entityWrapper.commit();
		} catch (Exception ex) {
			LOG.error(ex.getMessage());
			LOG.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}		
	}
}
