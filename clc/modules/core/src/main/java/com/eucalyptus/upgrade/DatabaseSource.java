package com.eucalyptus.upgrade;

import groovy.sql.Sql;
import java.sql.SQLException;

public interface DatabaseSource {
  
  public abstract Sql getSqlSession( String persistenceContext ) throws SQLException;
  
}