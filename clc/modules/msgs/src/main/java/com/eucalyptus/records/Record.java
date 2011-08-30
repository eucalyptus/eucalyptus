package com.eucalyptus.records;

public interface Record {
  
  public abstract Record info( );
  
  public abstract Record error( );
  
  public abstract Record trace( );
  
  public abstract Record debug( );
  
  public abstract Record extreme( );

  public abstract Record exhaust( );
  
  public abstract Record warn( );
  
  public abstract Record next( );
  
  public abstract Record append( Object... obj );
  
  public abstract Record withDetails( String key, String value );

  public abstract Record withDetails( String userName, String primaryKey, String key, String value );
  
  public abstract String toString( );
  
  public abstract int hashCode( );

  public abstract EventType getType( );

  public abstract EventClass getEventClass( );

}