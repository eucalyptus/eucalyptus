import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ImageInfo;

EntityWrapper db;
Users.listAllUsers().each{ User user ->
  def u = new UserReportInfo() {{
      userName = user.getName() 
      imageCount = 0
    }
  };
  (db = new EntityWrapper<ImageInfo>( )).query( ImageInfo.byOwnerId( user.getName() ) ).each{ ImageInfo image ->
    u.imageCount++
  }
  db?.commit()
  results.add( u )
}
results.each{  println "HELLOOOO ${it.dump()}" }
def class UserReportInfo {
  String userName;
  Integer imageCount = 0;
}

