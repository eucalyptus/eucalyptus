package com.eucalyptus.cluster;


public interface SuccessCallback<RESP> {
  public void apply( RESP response );
}
