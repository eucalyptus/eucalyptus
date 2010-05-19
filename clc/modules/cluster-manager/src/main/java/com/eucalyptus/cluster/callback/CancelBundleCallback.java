package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.records.EventType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.CancelBundleTaskResponseType;
import edu.ucsb.eucalyptus.msgs.CancelBundleTaskType;
import com.eucalyptus.records.EventRecord;

public class CancelBundleCallback extends QueuedEventCallback<CancelBundleTaskType,CancelBundleTaskResponseType> {

  private static Logger LOG = Logger.getLogger( CancelBundleCallback.class );
  public CancelBundleCallback( CancelBundleTaskType request ) {
    this.setRequest( request );
  }
  

  @Override
  public void prepare( CancelBundleTaskType msg ) throws Exception {}

  @Override
  public void verify( BaseMessage response ) throws Exception {
    CancelBundleTaskResponseType reply = (CancelBundleTaskResponseType) response;
    if ( !reply.get_return( ) ) {
      LOG.info( "Attempt to CancelBundleTask for instance " + this.getRequest( ).getBundleId( ) + " has failed." );
    } else {
      try {
        VmInstance vm = VmInstances.getInstance( ).lookupByBundleId( this.getRequest().getBundleId( ) );
        EventRecord.here( CancelBundleCallback.class, EventType.BUNDLE_CANCELLED, this.getRequest( ).getUserId( ), vm.getBundleTask( ).getBundleId( ), vm.getInstanceId( ) ).info( );
        vm.resetBundleTask( );
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
