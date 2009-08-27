package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

import java.util.*;

public class VmReboot extends RebootInstancesType implements Cloneable {

  private static Logger LOG = Logger.getLogger( VmReboot.class );

  private boolean isAdmin = false;
  private boolean result = false;

  public VmReboot( RebootInstancesType msg )
  {
    this.setCorrelationId( msg.getCorrelationId() );
    this.setEffectiveUserId( msg.getEffectiveUserId() );
    this.setUserId( msg.getUserId() );
    this.setInstancesSet( msg.getInstancesSet() );
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
    if ( this.getInstancesSet() == null )
      this.setInstancesSet( new ArrayList<String>() );
  }

  public void apply() throws EucalyptusCloudException
  {
    if ( this.getInstancesSet().isEmpty() ) return;
    Map<String, RebootInstancesType> rebootMap = new HashMap<String, RebootInstancesType>();
    for ( String vmId : this.getInstancesSet() )
    {
      VmInstance vm = null;
      try
      {
        vm = VmInstances.getInstance().lookup( vmId );
        if ( vm.getOwnerId().equals( this.getUserId() ) || this.isAdmin )
        {
          if ( rebootMap.get( vm.getPlacement() ) == null )
          {
            RebootInstancesType request = new RebootInstancesType();
            request.setUserId( this.isAdmin ? Component.eucalyptus.name() : this.getUserId() );
            rebootMap.put( vm.getPlacement(), request );
          }
          rebootMap.get( vm.getPlacement() ).getInstancesSet().add( vm.getInstanceId() );
        }
        else
        {
          this.result = false;
          return;
        }
      }
      catch ( NoSuchElementException e )
      {
        this.result = false;
        return;
      }
    }
//    for ( String cluster : rebootMap.keySet() )
//      Clusters.getInstance().lookup( cluster ).getMessageQueue().enqueue( rebootMap.get( cluster ) );
    this.result = true;
    return;
  }

  public void rollback()
  {

  }

  public RebootInstancesResponseType getResult()
  {
    RebootInstancesResponseType reply = ( RebootInstancesResponseType ) this.getReply();
    reply.set_return( this.result );
    return reply;
  }

}
