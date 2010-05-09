package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import com.eucalyptus.records.EventType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BundleInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.BundleInstanceType;
import com.eucalyptus.records.EventRecord;

public class BundleCallback extends QueuedEventCallback<BundleInstanceType,BundleInstanceResponseType> {

  private static Logger LOG = Logger.getLogger( BundleCallback.class );
  public BundleCallback( BundleInstanceType request ) {
    this.setRequest( request );
  }
  

  @Override
  public void prepare( BundleInstanceType msg ) throws Exception {}

  @Override
  public void verify( BaseMessage response ) throws Exception {
    BundleInstanceResponseType reply = (BundleInstanceResponseType) response;
    if ( !reply.get_return( ) ) {
      LOG.info( "Attempt to bundle instance " + this.getRequest( ).getInstanceId( ) + " has failed." );
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( this.getRequest().getInstanceId( ) );
        vm.resetBundleTask( );
      } catch ( NoSuchElementException e1 ) {
      }
    } else {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookup( this.getRequest().getInstanceId( ) );
        vm.clearPendingBundleTask( );
        EventRecord.here( BundleCallback.class, EventType.BUNDLE_STARTED, this.getRequest( ).getUserId( ), vm.getBundleTask( ).getBundleId( ), vm.getInstanceId( ) ).info( );
      } catch ( NoSuchElementException e1 ) {
      }
    }

  }

  @Override
  public void fail( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
  }

}
