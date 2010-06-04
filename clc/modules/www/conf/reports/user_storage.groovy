import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.blockstorage.Volume;

EntityWrapper db = EntityWrapper.get( Volume.class );
Users.listAllUsers().each{ User user ->
  def u = new UserStorageData() {{
      userName = user.getName() 
    }
  };
  Date bc = new Date(notBefore)
  Date ad = new Date(notAfter)
  db.query( Volume.ownedBy( user.getName() ) ).each{ Volume volume ->
    u.volumeCount++
    u.volumeGigabytes+=volume.getSize();
    Date start = volume.getBirthDay();
    if( bc.isAfter( volume.getBirthday() ) ) {
      start = bc;
    }
  }
  results.add( u )
}
db?.commit()
def class UserStorageData {
  String userName;
  Integer volumeCount = 0;
  Integer volumeGigabytes = 0;
  Integer volumeTime = 0;
  Integer snapshotCount = 0;
}

