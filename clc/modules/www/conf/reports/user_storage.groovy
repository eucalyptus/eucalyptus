import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.BaseRecord;
import com.eucalyptus.blockstorage.Volume;
import groovy.sql.Sql;

EntityWrapper db = EntityWrapper.get( BaseRecord.class );
Sql sql = new Sql( db.getSession( ).connection( ) )
def accountedFor = new TreeSet()
def groups = [:]
List userResults = new ArrayList()
Users.listAllUsers().each{ User user ->
  def u = new UserStorageData() {{
      userName = user.getName() 
    }
  };
  def query = "SELECT\n" + 
      "  crt.record_correlation_id as volume_id, \n" + 
      "  crt.record_user_id as user_id, \n" + 
      "  UNIX_TIMESTAMP(del.record_timestamp)*1000 as delete_time, UNIX_TIMESTAMP(crt.record_timestamp)*1000 as create_time,\n" + 
      "  TRIM(REPLACE(crt.record_extra,':size:',' ')) as volume_size\n" + 
      "FROM eucalyptus_records.records_logs as del, eucalyptus_records.records_logs as crt \n" + 
      "WHERE  \n" + 
      "  crt.record_user_id LIKE '${u.userName}' AND crt.record_extra LIKE '%size%' \n" + 
      "  AND crt.record_type LIKE 'VOLUME_CREATE' \n" + 
      "  AND UNIX_TIMESTAMP(crt.record_timestamp)*1000 < ${notAfter} \n" +
      "  AND UNIX_TIMESTAMP(del.record_timestamp)*1000 > ${notBefore} \n" +
      "  AND crt.record_correlation_id=del.record_correlation_id AND ( del.record_type LIKE 'VOLUME_DELETE' OR del.record_type LIKE 'VOLUME_CREATE' )\n" + 
      "ORDER BY delete_time DESC;"
  sql.rows( query ).each{
    if( accountedFor.add( it.volume_id ) ) {
      def volId = it.volume_id;
      u.volumeCount++
      Long startTime = ( it.create_time > notBefore ) ? it.create_time : notBefore;
      Long endTime = ( it.delete_time < notAfter ) ? it.delete_time : notAfter;
      Integer volSize = new Integer( it.volume_size );
      if( it.create_time > notBefore ) {
        u.volumeGigabytesAllocated += volSize
      }
      if( it.delete_time < notAfter ) {
        u.volumeGigabytesDeleted += volSize
      }
      def time = (volSize * (endTime - startTime) )
      u.volumeTimeSeconds += time
      time = time/(1000.0*60.0*60.0)
      u.volumeTime += Math.ceil( time )
      println "==> ${it.volume_id} ${it.volume_size} ${time} ${new Date(it.create_time)} ${new Date(it.delete_time)}"
      def snapshotQuery = "SELECT UNIX_TIMESTAMP(record_timestamp)*1000 as timestamp,record_correlation_id as snapshot_id FROM eucalyptus_records.records_logs WHERE record_class LIKE 'SNAPSHOT' AND record_type LIKE 'SNAPSHOT_CREATE' AND record_extra LIKE ':volume:%'";
      sql.rows( snapshotQuery ).each {
        if( it.timestamp < notAfter && it.timestamp > notBefore ) {
          u.volumeSnapshots++
        }
      }
    } else {
    }
  }
  for( Group group : Groups.lookupUserGroups( user ) ) {
    def g = new GroupStorageData() {{
            groupName = group.getName() 
          }
        };
    if( groups.containsKey( group.getName() ) ) {
      g = groups.get( group.getName() );
    } else {
      groups.put( group.getName(), g );
      groupResults.add( g );
    }
    g.metaClass.properties.findAll{ it.name.startsWith("volume") }.each {
      g[it.name]+=u[it.name]
    }
  }
  results.add( u )
}
results.each{ println it.dump() }
groupResults.each{ println it.dump() }
db?.commit()
def class GroupStorageData {
  String groupName;
  Integer volumeCount = 0;
  Integer volumeExtant = 0;
  Integer volumeGigabytesAllocated = 0;
  Integer volumeGigabytesDeleted = 0;
  Integer volumeSnapshots = 0;
  Integer volumeTime = 0;
  Integer volumeTimeSeconds = 0;
}
def class UserStorageData {
  String userName;
  Integer volumeCount = 0;
  Integer volumeExtant = 0;
  Integer volumeGigabytesAllocated = 0;
  Integer volumeGigabytesDeleted = 0;
  Integer volumeSnapshots = 0;
  Integer volumeTime = 0;
  Integer volumeTimeSeconds = 0;
}

