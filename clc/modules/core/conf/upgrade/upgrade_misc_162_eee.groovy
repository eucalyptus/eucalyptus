import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import groovy.sql.Sql;
import com.eucalyptus.upgrade.AbstractUpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.Groups;

class upgrade_misc_162_eee extends AbstractUpgradeScript {
  static final String FROM_VERSION = "1.6.2";
  static final String TO_VERSION = "eee-2.0.0";
  
  public upgrade_misc_162_eee() {
	  super(4);
  }
  
  @Override
  public Boolean accepts( String from, String to ) {
    if(FROM_VERSION.equals(from) && TO_VERSION.equals(to))
      return true;
    return false;
  }
  
  @Override
  public void upgrade(File oldEucaHome, File newEucaHome) {
    EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>("eucalyptus_general");
    try {
      for( ImageInfo img : db.query( new ImageInfo() ) ) {
        img.grantPermission( Users.lookupUser( img.getImageOwnerId( ) ) );
        img.grantPermission( Groups.lookupGroup( "all" ) );
      }
      db.commit( );
    } catch( Throwable e ) {
      e.printStackTrace();
      db.rollback( );
    }
  }
}
