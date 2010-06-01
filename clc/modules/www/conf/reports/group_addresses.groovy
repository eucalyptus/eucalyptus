import com.eucalyptus.auth.*;
import com.eucalyptus.auth.principal.*;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ImageInfo;

EntityWrapper db;
Users.listAllUsers().each{ User user ->
  def u = new UserReportInfo() {{
          groupName = user.getName() 
        }
      };
  (db = EntityWrapper.get( ImageInfo.class )).query( ImageInfo.byOwnerId( user.getName() ) ).each{ ImageInfo image ->
    u.imageCount++
    if("machine".equals( image.getImageType() ) ) {
      u.imageMachine++
    } else if("kernel".equals( image.getImageType() ) ) {
      u.imageKernel++
    } else if("machine".equals( image.getImageType() ) ) {
      u.imageRamdisk++
    }
  }
  db?.commit()
  results.add( u )
}
def class UserReportInfo {
  String groupName;
  Integer imageCount = 0;
  Integer imageKernel = 0;
  Integer imageMachine = 0;
  Integer imageRamdisk = 0;
  Integer volumes = 0;
  Integer snapshots;
  Integer networks;
  Integer tcpRules;
  Integer udpRules;
  Integer icmpRules;
}

