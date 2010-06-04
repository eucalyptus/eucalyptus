import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.address.Address;

EntityWrapper db = EntityWrapper.get( Address.class );
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
  results.add( u )
}
db?.commit()
println results
def class UserAddressData {
  String userName;
  Integer allocCount = 0;
  Integer assignCount = 0;
  Integer systemCount = 0;
  Integer allocTime = 0;
  Integer assignTime = 0;
  Integer systemTime = 0;
}

