import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ImageInfo;

println "HELLOOOO"
EntityWrapper db;
Users.listAllUsers().each{ User user ->
  println "HELLOOOO ${user.name}"
  def u = new UserReportInfo() {{
      userName = user.getName() 
      imageCount = 0
    }
  };
  (db = EntityWrapper.get( ImageInfo.class )).query( ImageInfo.byOwnerId( user.getName() ) ).each{ ImageInfo image ->
    u.imageCount++
  }
  db?.commit()
  results.add( u )
}
def class UserReportInfo {
  String userName;
  Integer imageCount = 0;
}

