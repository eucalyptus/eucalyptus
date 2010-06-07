import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.BaseRecord;
import groovy.sql.Sql;

EntityWrapper db = EntityWrapper.get( BaseRecord.class );
Sql sql = new Sql( db.getSession( ).connection( ) )
def groups = [:]
def accountedFor = new TreeSet()
List userResults = new ArrayList()
Users.listAllUsers().each{ User user ->
  def u = new UserVmData() {{
      userName = user.getName() 
    }
  };
  def query = "SELECT\n" + 
    "  crt.record_correlation_id as instance_id, \n" + 
    "  crt.record_user_id as user_id, \n" + 
    "  UNIX_TIMESTAMP(del.record_timestamp)*1000 as terminate_time, UNIX_TIMESTAMP(crt.record_timestamp)*1000 as start_time,\n" + 
    "  TRIM(REPLACE(crt.record_extra,':type:',' ')) as instance_type\n" + 
    "FROM eucalyptus_records.records_logs as del, eucalyptus_records.records_logs as crt \n" + 
    "WHERE  \n" + 
    "  crt.record_user_id LIKE '${u.userName}' AND crt.record_extra LIKE '%type%' \n" + 
    "  AND crt.record_type LIKE 'VM_STATE' \n" + 
    "  AND UNIX_TIMESTAMP(crt.record_timestamp)*1000 < ${notAfter} \n" +
    "  AND UNIX_TIMESTAMP(del.record_timestamp)*1000 > ${notBefore} \n" +
    "  AND crt.record_correlation_id=del.record_correlation_id AND del.record_class LIKE 'VM'\n" + 
    "ORDER BY terminate_time DESC;"
  sql.rows( query ).each{
    if( accountedFor.add( it.instance_id ) ) {
      def type = it.instance_type.replaceAll("\\.","")
      Long startTime = ( it.start_time > notBefore ) ? it.start_time : notBefore;
      Long endTime = ( it.terminate_time < notAfter ) ? it.terminate_time : notAfter;
      if( it.start_time > notBefore ) {
        u[type]++
      }
      def time = (endTime - startTime)/(1000.0*60.0*60.0)
      u[type+"Time"] += time
    }
  }
  for( Group group : Groups.lookupUserGroups( user ) ) {
    def g = new GroupVmData() ;
    g.groupName = group.getName();
    
    if( groups.containsKey( group.getName() ) ) {
      g = groups.get( group.getName() );
    } else {
      groups.put( group.getName(), g );
      groupResults.add( g );
    }
    g.metaClass.properties.findAll{ !it.name.startsWith("group") && it.name!="metaClass"&&it.name!="class" }.each {
      g[it.name]+=u[it.name]
    }
  }
  results.add( u )
}
results.each{ println it.dump() }
db?.commit()
def class GroupVmData {
  String groupName;
  Integer vmCount = 0;
  Integer m1small = 0;
  Integer c1medium = 0;
  Integer m1large = 0;
  Integer m1xlarge = 0;
  Integer c1xlarge = 0;
  Integer m1smallTime = 0;
  Integer c1mediumTime = 0;
  Integer m1largeTime = 0;
  Integer m1xlargeTime = 0;
  Integer c1xlargeTime = 0;
}
def class UserVmData {
  String userName;
  Integer vmCount = 0;
  Integer m1small = 0;
  Integer c1medium = 0;
  Integer m1large = 0;
  Integer m1xlarge = 0;
  Integer c1xlarge = 0;
  Integer m1smallTime = 0;
  Integer c1mediumTime = 0;
  Integer m1largeTime = 0;
  Integer m1xlargeTime = 0;
  Integer c1xlargeTime = 0;
}

