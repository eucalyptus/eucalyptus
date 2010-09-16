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


/* Incrementally prune any redundant data in the log tables.
 */
Pruner pruner = new Pruner( sql )
pruner.initialPrune()

def query = "SELECT MAX(UNIX_TIMESTAMP(record_timestamp)*1000) as terminate_time, " +
            " MIN(UNIX_TIMESTAMP(record_timestamp)*1000) as start_time, " +
            " record_user_id as user_id, " +
            " record_correlation_id as instance_id, " +
            " TRIM(REPLACE(record_extra,':type:',' ')) as instance_type " + 
            "FROM eucalyptus_records.records_logs " +
            "WHERE record_class LIKE 'VM' " +
            "AND record_type LIKE 'VM_STATE' " +
            "AND record_user_id LIKE '${u.userName}' " +
            "AND record_extra LIKE '%type%' " + 
            "GROUP BY instance_id " +
            "HAVING start_time < ${notAfter} " +
            "AND terminate_time > ${notBefore};"
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

