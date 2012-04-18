package com.eucalyptus.util.async;

import com.eucalyptus.util.Callback;


public abstract class UnconditionalCallback implements Callback {
  
  @Override
  public void fire( Object o ) {
    this.fire( );
  }
  
  public abstract void fire( );
}
