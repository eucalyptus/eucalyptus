import java.sql.Timestamp;
import groovy.sql.*;
import org.apache.log4j.*;

/**
 * This class prunes superfluous data from the records_logs table.
 *
 * Its primary method (prune) is invoked every time an instance report is
 * generated. It can also be invoked from the command line.
 *
 * @author twerges
 */
class Pruner
{
	private static Integer QUERY_LIMIT = 1000000  //HACK to avoid MySQL memory leak; @see pruneRepeatedly
	private static Logger  LOG = Logger.getLogger( Pruner.class )

	private final Sql sql


	public Pruner(Sql sql)
	{
		this.sql = sql
	}
	


	/**
	 * Prunes redundant data from the database, by deleting all data except the
	 * earliest and latest n rows for each instance.
	 *
	 * It's necessary to delete superfluous data because of a bug in the code that re-adds
	 * instance data to the log table every 20 seconds. That bug creates a vast amount of
	 * superfluous log data for long-running instances, which slows down report generation.
	 *
	 * This function can be run repeatedly, and will incrementally re-prune the
	 * records_logs table each time.
	 *
	 * This function uses an algoritm which requires only one full table scan and does
	 * not require mysql to sort the results.
	 *
	 * @param redundantRowsDeleteThreshold How many redundant rows an instance must have
	 *   to be pruned.
	 * @param targetRowsNum The number of rows to retain after pruning for each instance,
	 *   at the beginning and at the end (n rows at the beginning, and n at the end).
	 */
	public void prune(Integer redundantRowsDeleteThreshold=100, Integer targetRowsNum=80,
		 			  Long onlyAfterTimestamp=0, Long onlyBeforeTimestamp=9999999999) {

		assert redundantRowsDeleteThreshold != null
		assert targetRowsNum != null
		assert onlyAfterTimestamp != null
		assert onlyBeforeTimestamp != null
		assert onlyAfterTimestamp < onlyBeforeTimestamp
		assert redundantRowsDeleteThreshold > 0
		assert targetRowsNum > 0

		LOG.info("Begin prune")

		/* Find earliest and latest Nth rows, according to the following algorithm.
		 * Establish two lists for each instance: one for the earliest nth rows, and one
		 * for the latest nth rows. Whenever we encounter a row for an instance which
		 * is earlier than the latest row in the early list, displace the earliest
		 * row in the early list and add the current row. Likewise (but in reverse)
		 * with the latest rows. At the end of this procedure, we will have two
		 * lists for each instance that contain the earliest and latest n rows for
		 * that instance. Then, delete all rows for the instance with a timestamp
		 * greater than the latest timestamp in the early list, and less than the
		 * earliest timestamp in the late list. What remains will be the earlist n
		 * and latest n rows for an instance; everything in between will be deleted.
		 *
		 * This algorithm was chosen for the following reasons: 1) It requires only
		 * one full table scan of the data; 2) it does not require sorting the data,
		 * which would cause MySQL to do a slow disk sort; 3) it uses a minmum of heap
		 * space; 4) it can be run incrementally without storing any data between
		 * invocations such as last pruning timestamp
		 */

		/* Find earliest and latest n rows */
		String query = """
		SELECT record_correlation_id, UNIX_TIMESTAMP(record_timestamp) as ts
		FROM records_logs
		WHERE record_class = 'VM'
		AND UNIX_TIMESTAMP(record_timestamp) > ?
		AND UNIX_TIMESTAMP(record_timestamp) < ?
		"""	

		/* HACK to avoid memory leak from MySQL driver; add LIMIT clause to SQL */
		if (QUERY_LIMIT != null && QUERY_LIMIT > 0) { query = query + " LIMIT " + QUERY_LIMIT }

		def instanceInfoMap = [:]

		this.sql.eachRow( query, [onlyAfterTimestamp,onlyBeforeTimestamp] ) {

			if (! instanceInfoMap.containsKey(it.record_correlation_id)) {
				instanceInfoMap[it.record_correlation_id]=new InstanceInfo()
				LOG.debug("Found new instance:" + it.record_correlation_id)
			}
			InstanceInfo info = instanceInfoMap[it.record_correlation_id]

			//latestEarlyTs is only set when earlyList has reached n rows
			if (info.latestEarlyTs==null) {
				info.earlyList.add(it.ts)
				if (info.earlyList.size() >= targetRowsNum) {
					info.earlyList.sort()
					info.latestEarlyTs = info.earlyList.last()
				}
			} else if (it.ts <= info.latestEarlyTs) {
				Long ts = it.ts
				info.earlyList.add(ts)
				info.earlyList.sort()
				info.earlyList.remove(info.earlyList.last())
				info.latestEarlyTs = info.earlyList.last()
			}

			//earliestLateTs is only set when lateList has reached n rows
			if (info.earliestLateTs==null) {
				info.lateList.add(it.ts)
				if (info.lateList.size() >= targetRowsNum) {
					info.lateList.sort()
					info.earliestLateTs = info.lateList.first()
				}
			} else if (it.ts >= info.earliestLateTs) {
				Long ts = it.ts
				info.lateList.add(ts)
				info.lateList.sort()
				info.lateList.remove(info.lateList.first())
				info.earliestLateTs = info.lateList.first()
			}

			info.rowCnt = info.rowCnt+1

		}


		/* Delete data for each instance which is later than the latest early timestamp,
		 * but earlier than the earliest late timestamp.
		 */
		query = """
		DELETE FROM records_logs
		WHERE record_correlation_id = ?
		AND record_class = 'VM'
		AND UNIX_TIMESTAMP(record_timestamp) > ?
		AND UNIX_TIMESTAMP(record_timestamp) < ?
		"""
		LOG.debug("Begin deleting")
		Integer redundantRowsCnt
		instanceInfoMap.each { key, value ->
			redundantRowsCnt = value.rowCnt-(targetRowsNum*2)
			LOG.debug("INSTANCE id:${key} rowsAboveThreshold:${redundantRowsCnt}")
			if (value.rowCnt-(targetRowsNum*2) > redundantRowsDeleteThreshold) {
				this.sql.executeUpdate(query, [key, value.latestEarlyTs, value.earliestLateTs])
				LOG.debug(String.format("DELETE id:%s %d-%d",
										key, value.latestEarlyTs, value.earliestLateTs))
			}
		}

		LOG.info("End prune")
	}   //end: prune method


	/**
	 * HACK to handle memory leak in mysql/groovy.  The MySQL JDBC driver will throw an
	 * OutOfMemory exception sometimes for ResultSets greater than 10M rows or so.
	 * This happens even when you execute the query by itself in a groovy script with no
	 * other code.
	 *
	 * To get around this problem, I added a "LIMIT" clause to the prune() method which
	 * limits resultsets to 4M rows.
	 *
	 * This method calls prune repeatedly, so the entire log will be pruned (4M rows at
	 * a time).
	 */
	public void pruneRepeatedly()
	{
		def res = sql.firstRow("SELECT count(*) AS cnt FROM records_logs")
		if (QUERY_LIMIT != null) {
			for (int i=0; i<res.cnt.intdiv(QUERY_LIMIT); i++) {
				LOG.info("Prune iteration ${i}")
				prune()  //prune 4M rows
			}
		}
		LOG.info("Prune iteration final")
		prune()  //prune remainder of rows less than 4M
	}


	/**
	 * Command-line invocation of the prune method
	 */
	public static void main(String[] args) {

		/* Read cmd-line args */
		CliBuilder cli = new CliBuilder(usage:"Pruner.groovy -p mysqlPassword [options]")
		cli.D(args:1, required:false, argName:"dbName", "Database Name (default eucalyptus_records)")
		cli.u(args:1, required:false, argName:"username", "username (default eucalyptus)")
		cli.p(args:1, required:true,  argName:"password", "password for mysql")
		cli.h(args:1, required:false, argName:"host", "host of mysqld (default localhost)")
		cli.P(args:1, required:false, argName:"port", "port or mysqld (default 8777)")
		cli.g(args:0, required:false, argName:"debug", "debugging output (default false)")
		cli.a(args:1, required:false, argName:"onlyAfterTimestamp", "only prunes after timestamp in secs (default 0)")
		cli.b(args:1, required:false, argName:"onlyBeforeTimestamp", "only prunes before timestamp in secs (default max timestamp)")
		cli.t(args:1, required:false, argName:"redundantRowsDeleteThreshold", "only prunes instances more than n redundant rows (default 100)")
		cli.r(args:1, required:false, argName:"targetRowsNum", "how many rows to preserve at the beginning and end for each instance (default 80)")
		cli.e(args:0, required:false, argName:"pruneRepeatedly", "repeatedly prunes one section at a time")
		def options = cli.parse(args)
		if (!options) System.exit(-1)

		/* Parse cmd-line args into appropriate types and provide default values */
		def optsMap = [:]
		optsMap['D']=options.D ? options.D : "eucalyptus_records"
		optsMap['u']=options.u ? options.u : "eucalyptus"
		optsMap['p']=options.p
		optsMap['g']=options.g
		optsMap['h']=options.h ? options.h : "localhost"
		optsMap['P']=options.P ? Long.parseLong(options.P) : 8777l
		optsMap['a']=options.a ? Long.parseLong(options.a) : 0l
		optsMap['b']=options.b ? Long.parseLong(options.b) : 9999999999l
		optsMap['t']=options.t ? Integer.parseInt(options.t) : 100
		optsMap['r']=options.r ? Integer.parseInt(options.r) : 80
		optsMap['e']=options.e

		if (optsMap['g']) {
			LOG.setLevel(Level.DEBUG)
		} else {
			LOG.setLevel(Level.OFF)
		}

		LOG.debug(String.format("Using db:%s user:%s host:%s port:%d debug:%s " +
								"after:%d before:%d threshold:%d target:%d i:%b", 
								optsMap.D, optsMap.u, optsMap.h, optsMap.P, optsMap.g,
								optsMap.a, optsMap.b, optsMap.t, optsMap.r, optsMap.e))


		/* Create a mysql connection, then prune */
		def connStr = "jdbc:mysql://${optsMap['h']}:${optsMap['P']}/${optsMap['D']}"
		Sql sql = Sql.newInstance(connStr, optsMap['u'],optsMap['p'],'com.mysql.jdbc.Driver')

		Pruner pruner = new Pruner(sql);
		if (optsMap['e']) {
			pruner.pruneRepeatedly()
		} else {
			pruner.prune(optsMap['t'], optsMap['r'], optsMap['a'], optsMap['b'])
		}

	}  //end: main method


}  //end: Pruner class


class InstanceInfo {
	List earlyList = []
	List lateList = []
	Long latestEarlyTs = null
	Long earliestLateTs = null
	Integer rowCnt=0
}
