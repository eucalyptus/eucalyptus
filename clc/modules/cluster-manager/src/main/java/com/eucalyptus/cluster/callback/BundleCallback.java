package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.MessageCallback;
import edu.ucsb.eucalyptus.msgs.BundleInstanceResponseType;
import edu.ucsb.eucalyptus.msgs.BundleInstanceType;

public class BundleCallback extends MessageCallback<BundleInstanceType,BundleInstanceResponseType> {

  private static Logger LOG = Logger.getLogger( BundleCallback.class );
  public BundleCallback( BundleInstanceType request ) {
    this.setRequest( request );
  }
  

  @Override
  public void initialize( BundleInstanceType msg ) {}

  @Override
  public void fire( BundleInstanceResponseType reply ) {
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
        EventRecord.here( BundleCallback.class, EventType.BUNDLE_STARTED, this.getRequest( ).getUserErn( ), vm.getBundleTask( ).getBundleId( ), vm.getInstanceId( ) ).info( );
      } catch ( NoSuchElementException e1 ) {
      }
    }

  }

  @Override
  public void fireException( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
  }

}
