package com.eucalyptus.records;


public class RecordFilter {
  private static RecordFilter singleton = new RecordFilter();
  private RecordFilter( ) {}
  public static RecordFilter getInstance() {
    return singleton;
  }
  public boolean accepts( Record rec ) {
    final EventType check = rec.getType( );
    return !EventClass.ORPHAN.equals( rec.getEventClass( ) );
  }
}
