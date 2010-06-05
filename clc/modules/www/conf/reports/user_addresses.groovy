import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.address.Address;

EntityWrapper db = EntityWrapper.get( Address.class );
def groups = [:]
List userResults = new ArrayList()
Users.listAllUsers().each{ User user ->
  def u = new UserAddressData() {{
      userName = user.getName() 
    }
  };
  Address a = new Address(  )
  a.setUserId( user.getName() )
  db.query( a ).each{ Address addr ->
    u.allocCount++
    if( addr.isSystemOwned() ) {
      u.systemCount++
    }
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

