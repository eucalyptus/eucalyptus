package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;

import java.util.Date;

/**
 * User: decker
 * Date: Nov 12, 2008
 * Time: 1:52:53 PM
 */
public class VmConsoleOutput extends GetConsoleOutputType implements Cloneable {

  private static Logger LOG = Logger.getLogger( VmConsoleOutput.class );

  private boolean isAdmin = false;
  private boolean result = false;

  public VmConsoleOutput( GetConsoleOutputType msg )
  {
    this.setCorrelationId( msg.getCorrelationId() );
    this.setEffectiveUserId( msg.getEffectiveUserId() );
    this.setUserId( msg.getUserId() );
    this.setInstanceId( msg.getInstanceId() );
  }

  public void transform() throws EucalyptusInvalidRequestException
  {
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    UserInfo user = null;
    try
    {
      user = db.getUnique( new UserInfo( this.getUserId() ) );
      this.isAdmin = user.isAdministrator();
    }
    catch ( EucalyptusCloudException e )
    {
      db.rollback();
      throw new EucalyptusInvalidRequestException( e );
    }
    db.commit();
  }

  public void apply() throws EucalyptusCloudException
  {
    return;
  }

  public void rollback()
  {

  }

  public GetConsoleOutputResponseType getResult()
  {
    GetConsoleOutputResponseType reply = ( GetConsoleOutputResponseType ) this.getReply();
    reply.setOutput( "" );
    reply.setTimestamp( new Date() );
    return reply;
  }

}
