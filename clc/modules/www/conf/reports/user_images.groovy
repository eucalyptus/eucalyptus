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
  Integer imageCount = 0;
  Integer imageKernel = 0;
  Integer imageMachine = 0;
  Integer imageRamdisk = 0;
}

