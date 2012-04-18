import groovy.sql.*;
import java.sql.Timestamp;

/* This script transfers data from an old log table to a new one, then deletes
 * the transferred data from the old log table. This script does not transfer
 * any superfluous data from the old log table, allowing it to be destroyed
 * during the subsequent deletion.
 *
 * Transferring data requires a conversion, because the two log tables are quite different.
 * The old log table (records_logs) has multiple rows per instance, with a separate row
 * for each attribute. The new log table (instance_log) has only one row per instance,
 * with a column for each attribute. This script reads through all the rows from the old
 * table, storing each attribute encountered in RAM, then merging them at the end into a
 * single row with multiple columns.
 *
 * This script can be run repeatedly as new data accumulates in the old table. It
 * will transfer the newly added data to the new table. It will also fill in any
 * missing columns of instances which have already been transferred. For example, it
 * will fill in a null column in the new tables for terminate_time for an instance
 * which has already been transferred but had not terminated yet when the last transfer
 * was performed.
 * -tw
 *
 * POTENTIAL BUG: we must lock the records_logs table to prevent data from being
 * added to it during the conversion.
 */
if (args.length<5) {
	println("Usage: transfer.groovy dbName username password host port [debug]");
	System.exit(-1)
}
Sql sql = Sql.newInstance("jdbc:mysql://${args[3]}:${args[4]}/${args[0]}",
							args[1],args[2],'com.mysql.jdbc.Driver')
Boolean debuggingOutput = (args.length > 5 && args[5]=="debug")

lastTimestamp = new Timestamp(0L);


/* Perform a LEFT OUTER JOIN on the old and new tables. Interpret NULL ids from the
 * new table as a row which exists only in the old table. Interpret NULL columns in
 * the new table as columns missing from it.
 */
def query = """
SELECT rl.record_correlation_id,
	rl.record_extra as extra,
	rl.record_timestamp, 
	il.instance_id,
	rl.record_user_id as user_id,
	il.instance_type, il.instance_cluster,
	il.instance_platform, il.instance_image,
	il.started_time, il.terminated_time
FROM records_logs rl
LEFT OUTER JOIN instance_log il
ON rl.record_correlation_id = il.instance_id
WHERE rl.record_class = 'VM'
AND rl.record_type = 'VM_STATE'
"""


/* Gather all data to be inserted in the new table, and store it in RAM. We'll perform
 * the database insertion later. This way we can avoid inserting superfluous data.
 */
def class InstanceData {
	String userId
	String instanceType = null
	String cluster = null
	String platform = null
	String image = null
	Timestamp startTime = null
	Timestamp terminateTime = null
	Boolean isNewRow  //Indicates that there is no row yet for this instance in the new table, and an INSERT must be done
}
def instances = [:]
sql.eachRow( query ) {

	if (it.instance_id==null && !instances.containsKey(it.record_correlation_id)) {
		InstanceData newInstance = new InstanceData();
		newInstance.userId = it.user_id
		newInstance.isNewRow = true
		instances[it.record_correlation_id]=newInstance 
	}

	InstanceData instanceData = null;
	if (instances.containsKey(it.record_correlation_id)) {
		instanceData = instances[it.record_correlation_id]
	} else {
		instanceData = new InstanceData()
		instanceData.userId = it.user_id
		instances[it.record_correlation_id] = instanceData
	}


	//The format of the "extra" col in the db is ":attrName:attrValue"
	fields = it.extra.split(":")
	if (fields.size()==3) {

		if (it.instance_type == null && fields[1]=="type") {
			instanceData.instanceType = fields[2]
		} else if (it.instance_cluster == null && fields[1]=="cluster") {
			instanceData.cluster = fields[2]
		} else if (it.instance_platform == null && fields[1]=="platform") {
			instanceData.platform = fields[2]
		} else if (it.instance_image == null && fields[1]=="image") {
			instanceData.image = fields[2]
		} else if (it.terminated_time == null && fields[1]=="state" && fields[2]=="TERMINATED") {
			instanceData.terminateTime = it.record_timestamp
		} else if (it.started_time == null && fields[1]=="started" && instanceData.startTime==null) {
			instanceData.startTime = it.record_timestamp
		}
	}

	if (it.record_timestamp!=null) {
		lastTimestamp = (lastTimestamp < it.record_timestamp) ? it.record_timestamp : lastTimestamp;
	}
}


/* Insert and update data in the new table, based upon what we gathered from
 * querying the old table above.
 */
instances.each {

	/* Insert a new row for every instance found in the old table but not in the new
	 */
	if (it.value.isNewRow) {
		sql.executeUpdate("INSERT INTO instance_log " +
						"(instance_id, user_id) " +
						"VALUES (?,?)",
						[it.key, it.value.userId])
		if (debuggingOutput) {
			println("INSERT INTO instance_log " +
						"(instance_id, user_id) " +
						"VALUES (${it.key},${it.value.userId})")
						
		}
	}


	/* Update the new table with every attribute for every instance we found in
	 * the old table.
	 * NOTE: These must be done as separate UPDATEs, one for each column. Using
	 *  one big UPDATE by constructing a String will prevent groovy from using
	 *  a prepared statement.
	 */
	if (it.value.instanceType != null) {
		if (debuggingOutput) {
			printf("UPDATE instance_log SET instance_type=%s WHERE instance_id=%s\n",
					it.value.instanceType, it.key);
		}
		sql.executeUpdate("UPDATE instance_log SET instance_type=? WHERE instance_id=?",
							[it.value.instanceType, it.key])
	}
	if (it.value.cluster != null) {
		if (debuggingOutput) {
			printf("UPDATE instance_log SET instance_cluster=%s WHERE instance_id=%s\n",
					it.value.cluster, it.key);
		}
		sql.executeUpdate("UPDATE instance_log SET instance_cluster=? WHERE instance_id=?",
							[it.value.cluster, it.key])
	}
	if (it.value.platform != null) {
		if (debuggingOutput) {
			printf("UPDATE instance_log SET instance_platform=%s WHERE instance_id=%s\n",
					it.value.platform, it.key);
		}
		sql.executeUpdate("UPDATE instance_log SET instance_platform=? WHERE instance_id=?",
							[it.value.platform, it.key])
	}
	if (it.value.image != null) {
		if (debuggingOutput) {
			printf("UPDATE instance_log SET instance_image=%s WHERE instance_id=%s\n",
					it.value.image, it.key);
		}
		sql.executeUpdate("UPDATE instance_log SET instance_image=? WHERE instance_id=?",
							[it.value.image, it.key])
	}
	if (it.value.startTime != null) {
		if (debuggingOutput) {
			printf("UPDATE instance_log SET started_time=%s WHERE instance_id=%s\n",
					it.value.startTime, it.key);
		}
		sql.executeUpdate("UPDATE instance_log SET started_time=? WHERE instance_id=?",
							[it.value.startTime, it.key])
	}
	if (it.value.terminateTime != null) {
		if (debuggingOutput) {
			println "TERMINATE TIME:${it.value.terminateTime}"
			printf("UPDATE instance_log SET terminated_time=%s WHERE instance_id=%s\n",
					it.value.terminateTime, it.key);
		}
		sql.executeUpdate("UPDATE instance_log SET terminated_time=? WHERE instance_id=?",
							[it.value.terminateTime, it.key])
	}
}

/* DELETE all data which has been transferred to the new table.
 */
//sql.execute("DELETE FROM records_logs WHERE record_timestamp < ${lastTimestamp} AND record_class='VM' AND record_type='VM_STATE'");

