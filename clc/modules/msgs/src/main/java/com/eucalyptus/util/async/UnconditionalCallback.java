package com.eucalyptus.util.async;


public abstract class UnconditionalCallback implements Callback {
  
  @Override
  public void fire( Object o ) {
    this.fire( );
  }
  
  public abstract void fire( );
}
