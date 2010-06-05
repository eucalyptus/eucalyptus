import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.BaseRecord;
import groovy.sql.Sql;

EntityWrapper db = EntityWrapper.get( BaseRecord.class );
Sql sql = new Sql( db.getSession( ).connection( ) )
def groups = [:]
List userResults = new ArrayList()
Users.listAllUsers().each{ User user ->
  def u = new UserVmData() {{
      userName = user.getName() 
    }
  };
  def runQuery = "SELECT UNIX_TIMESTAMP(record_timestamp)*1000 as timestamp, record_extra as details FROM eucalyptus_records.records_logs " \
  +"WHERE record_class LIKE 'VM' AND record_extra LIKE 'user=${u.userName}:%:state=RUNNING:%' AND UNIX_TIMESTAMP(record_timestamp)*1000 < ${notAfter} " \
  +"GROUP BY record_extra ORDER BY min(record_timestamp) DESC;"
  def termQuery = "SELECT UNIX_TIMESTAMP(record_timestamp)*1000 as timestamp, record_extra as details FROM eucalyptus_records.records_logs " \
  +"WHERE record_class LIKE 'VM' AND record_extra LIKE \"user=${u.userName}:%:state=TERMINATED:%\" OR record_extra LIKE \"user=${u.userName}:%:state=TERMINATED:%\" AND UNIX_TIMESTAMP(record_timestamp)*1000 < ${notBefore} " \
  +"GROUP BY record_extra ORDER BY min(record_timestamp) DESC;"
  sql.rows( runQuery ).each{
      println it
        String[] details = it.details.split(":")
        Long timestamp = it.timestamp
        String instanceId = details[1].replaceAll(".*=","");
        String type = details[2].replaceAll(".*=","").replaceAll("\\.","");
        u[type]++
        println "Found RUN for ${instanceId} ${type} ${timestamp}"
      }
  sql.rows( termQuery ).each{
        String[] details = it.details.split(":")
        Long timestamp = it.timestamp
        String instanceId = details[1].replaceAll(".*=","");
        String type = details[2].replaceAll(".*=","");
        println "Found TERMINATE for ${instanceId} ${type} ${timestamp}"
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
    g.metaClass.properties.findAll{ !it.name.startsWith("user") && it.name!="metaClass"&&it.name!="class" }.each {
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

