import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.address.Address;

EntityWrapper db = EntityWrapper.get( Address.class );
Users.listAllUsers().each{ User user ->
  def u = new UserReportInfo() {{
      userName = user.getName() 
    }
  };
  Address a = new Address(  )
  a.setOwnerId( user.getName() )
  db.query(  ).each{ Address addr ->
    u.addrCount++
    if( addr.isSystemOwned() ) {
      u.systemCount++
    }
  }
  results.add( u )
}
db?.commit()
def class UserReportInfo {
  String userName;
  Integer addrCount = 0;
  Integer systemCount = 0;
  Integer allocTime = 0;
  Integer assignTime = 0;
}

