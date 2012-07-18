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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.BaseRecord;
import groovy.sql.*;
import org.apache.log4j.*;

/**
 * Establishes a thread which periodically runs the pruner.
 */
class PrunerThread
	implements Runnable
{

	private static Integer INTERVAL_NUM_SECS = 3600

	private static Logger  LOG = Logger.getLogger( PrunerThread.class )

	/**
	 * Run a pruner once.
	 */
	public void run()
	{
		EntityWrapper db = EntityWrapper.get( BaseRecord.class );
		Sql sql
		try {
			/* It's necessary to have a new DB conn every prune, to avoid timeout/closure */
			sql = new Sql( db.getSession( ).connection( ) )
			new Pruner( sql ).prune()
		} finally {
			db?.commit()
		}
	}


	/**
	 * Start a single thread which prunes the database at periodic intervals.
	 */
	public static void startThreadIfNotRunning()
	{
		if( java.lang.System.getProperties( ).setProperty("euca.periodic.filter", "running" ) == null ) {
			LOG.info("Established Pruner thread")
			Executors.newSingleThreadScheduledExecutor( ).scheduleAtFixedRate( new PrunerThread(), 0l, INTERVAL_NUM_SECS, TimeUnit.SECONDS );
		}
	}

}
