package com.eucalyptus.event;


public abstract class Event {
  private boolean veto;
  private String cause;
  private Exception fail;
  
  public Event( ) {
    super( );
    this.veto = false;
  }

  public void veto( ) {
    this.veto = true;
  }

  public void veto( String cause ) {
    this.veto = true;
    this.cause = cause;
  }
  
  public boolean isVetoed( ) {
    return this.veto;
  }

  public String getCause( ) {
    return cause;
  }

  public Exception getFail( ) {
    return fail;
  }

  public void setFail( Exception fail ) {
    this.fail = fail;
  }
  
}
