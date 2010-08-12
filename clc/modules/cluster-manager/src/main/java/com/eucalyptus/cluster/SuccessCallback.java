package com.eucalyptus.cluster;

import org.apache.log4j.Logger;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface SuccessCallback<T extends BaseMessage> extends Callback {
  static Logger LOG = Logger.getLogger( SuccessCallback.class );
  public void apply( T t );
  @SuppressWarnings( "unchecked" )
  public static SuccessCallback NOOP = new SuccessCallback() {
    @Override
    public void apply( BaseMessage t ) {
      LOG.trace( "NOOP: " + t.toString( ) );
    }
  };
}
