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
