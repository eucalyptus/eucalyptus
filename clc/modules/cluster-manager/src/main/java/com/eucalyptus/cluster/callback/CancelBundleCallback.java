package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.vm.CancelBundleTaskResponseType;
import com.eucalyptus.vm.CancelBundleTaskType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.MessageCallback;

public class CancelBundleCallback extends MessageCallback<CancelBundleTaskType,CancelBundleTaskResponseType> {

  private static Logger LOG = Logger.getLogger( CancelBundleCallback.class );
  public CancelBundleCallback( CancelBundleTaskType request ) {
    super( request );
  }
  

  @Override
  public void initialize( CancelBundleTaskType msg ) {}

  @Override
  public void fire( CancelBundleTaskResponseType reply ) {
    if ( !reply.get_return( ) ) {
      LOG.info( "Attempt to CancelBundleTask for instance " + this.getRequest( ).getBundleId( ) + " has failed." );
    } else {
      try {
        VmInstance vm = VmInstances.lookupByBundleId( this.getRequest().getBundleId( ) );
        EventRecord.here( CancelBundleCallback.class, EventType.BUNDLE_CANCELLED, this.getRequest( ).toSimpleString( ), vm.getBundleTask( ).getBundleId( ), vm.getInstanceId( ) ).info( );
        vm.resetBundleTask( );
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
