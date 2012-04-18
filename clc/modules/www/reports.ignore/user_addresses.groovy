import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.address.Address;
import groovy.sql.Sql;
import com.eucalyptus.records.BaseRecord;

EntityWrapper db = EntityWrapper.get( BaseRecord.class );
Sql sql = new Sql( db.getSession( ).connection( ) )
def groups = [:]
def accountedFor = new TreeSet()
List userResults = new ArrayList()
Users.listAllUsers().each{ User user ->
  def u = new UserAddressData() {{
      userName = user.getName() 
    }
  };
  def query = "SELECT\n" + 
    "  crt.record_correlation_id as addr_id, \n" + 
    "  crt.record_user_id as user_id, \n" + 
    "  UNIX_TIMESTAMP(del.record_timestamp)*1000 as dealloc_time, UNIX_TIMESTAMP(crt.record_timestamp)*1000 as alloc_time,\n" + 
    "  TRIM(REPLACE(crt.record_extra,':size:',' ')) as volume_size\n" + 
    "FROM eucalyptus_records.records_logs as del, eucalyptus_records.records_logs as crt \n" + 
    "WHERE  \n" + 
    "  crt.record_user_id LIKE '${u.userName}' AND crt.record_extra LIKE '%USER%' \n" + 
    "  AND crt.record_type LIKE 'ADDRESS_ASSIGN' \n" + 
    "  AND UNIX_TIMESTAMP(crt.record_timestamp)*1000 < ${notAfter} \n" +
    "  AND UNIX_TIMESTAMP(del.record_timestamp)*1000 > ${notBefore} \n" +
    "  AND crt.record_correlation_id=del.record_correlation_id AND ( del.record_type LIKE 'ADDRESS_ASSIGN' OR del.record_type LIKE 'ADDRESS_UNASSIGN' )\n" + 
    "ORDER BY dealloc_time DESC;"
  db.query( a ).each{ Address addr ->
    def volId = it.addr_id;
    u.volumeCount++
    Long startTime = ( it.alloc_time > notBefore ) ? it.alloc_time : notBefore;
    Long endTime = ( it.dealloc_time < notAfter ) ? it.dealloc_time : notAfter;
    Integer volSize = new Integer( it.volume_size );
    if( it.alloc_time > notBefore ) {
      u.volumeGigabytesAllocated += volSize
    }
    if( it.dealloc_time < notAfter ) {
      u.volumeGigabytesDeleted += volSize
    }
    def time = (volSize * (endTime - startTime) )
    u.volumeTimeSeconds += time
    time = time/(1000.0*60.0*60.0)
    u.volumeTime += Math.ceil( time )
    println "==> ${it.addr_id} ${time} ${new Date(it.alloc_time)} ${new Date(it.dealloc_time)}"
  }
  for( Group group : Groups.lookupUserGroups( user ) ) {
    def g = new GroupAddressData();
    g.groupName = group.getName();
    
    if( groups.containsKey( group.getName() ) ) {
      g = groups.get( group.getName() );
    } else {
      groups.put( group.getName(), g );
      groupResults.add( g );
    }
    g.metaClass.properties.findAll{ !it.name.startsWith("group") && it.name!="metaClass"&&it.name!="class"  }.each {
      g[it.name]+=u[it.name]
    }
  }
  results.add( u )
}
db?.commit()
println results
def class GroupAddressData {
  String groupName;
  Integer allocCount = 0;
  Integer assignCount = 0;
  Integer systemCount = 0;
  Integer allocTime = 0;
  Integer assignTime = 0;
  Integer systemTime = 0;
}
def class UserAddressData {
  String userName;
  Integer allocCount = 0;
  Integer assignCount = 0;
  Integer systemCount = 0;
  Integer allocTime = 0;
  Integer assignTime = 0;
  Integer systemTime = 0;
}

