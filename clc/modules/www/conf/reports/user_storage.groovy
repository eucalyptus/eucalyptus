import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.blockstorage.Volume;

EntityWrapper db = EntityWrapper.get( Volume.class );
Users.listAllUsers().each{ User user ->
  def u = new UserReportInfo() {{
      userName = user.getName() 
    }
  };
  db.query( Volume.ownedBy( user.getName() ) ).each{ Volume volume ->
    u.volumeCount++
    u.volumeGigabytes+=volume.getSize();
  }
  results.add( u )
}
db?.commit()
def class UserReportInfo {
  String userName;
  Integer volumeCount = 0;
  Integer volumeGigabytes = 0;
}

