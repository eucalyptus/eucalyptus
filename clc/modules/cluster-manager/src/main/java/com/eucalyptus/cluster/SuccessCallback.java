package com.eucalyptus.cluster;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface SuccessCallback<T extends BaseMessage> extends Callback {
  public void apply( T t );
  @SuppressWarnings( "unchecked" )
  public static SuccessCallback NOOP = new SuccessCallback() {
    @Override
    public void apply( BaseMessage t ) {}
  };
}
