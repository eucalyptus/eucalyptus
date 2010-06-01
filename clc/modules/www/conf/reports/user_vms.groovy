import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ImageInfo;

EntityWrapper db = EntityWrapper.get( ImageInfo.class );
Users.listAllUsers().each{ User user ->
  def u = new UserReportInfo() {{
      userName = user.getName() 
    }
  };
  db.query( ImageInfo.byOwnerId( user.getName() ) ).each{ ImageInfo image ->
    u.imageCount++
    if("machine".equals( image.getImageType() ) ) {
      u.imageMachine++
    } else if("kernel".equals( image.getImageType() ) ) {
      u.imageKernel++
    } else if("machine".equals( image.getImageType() ) ) {
      u.imageRamdisk++
    }
  }
  results.add( u )
}
db?.commit()
def class UserReportInfo {
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

