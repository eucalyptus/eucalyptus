package com.eucalyptus.records;

public interface Record {
  
  public abstract Record info( );
  
  public abstract Record error( );
  
  public abstract Record trace( );
  
  public abstract Record debug( );
  
  public abstract Record warn( );
  
  public abstract Record next( );
  
  public abstract Record append( Object... obj );
  
  public abstract String toString( );
  
  public abstract int hashCode( );

  public abstract EventType getType( );

  public abstract EventClass getEventClass( );
  
}