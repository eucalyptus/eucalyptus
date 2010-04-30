package com.eucalyptus.binding;

import java.util.List;
import com.google.common.collect.Lists;

public abstract class BindingGenerator {
  public static Class             DATA_TYPE;
  public static Class             MSG_TYPE;
  static {
    try {
      DATA_TYPE = Class.forName( "edu.ucsb.eucalyptus.msgs.EucalyptusData" );
      MSG_TYPE = Class.forName( "edu.ucsb.eucalyptus.msgs.BaseMessage" );
    } catch ( ClassNotFoundException e ) {
      e.printStackTrace( );
      System.exit( -1 );
    }
  }

  public abstract void processClass( Class klass );
  public abstract void close( );
  public static List<BindingGenerator> getGenerators( ) {
    return Lists.newArrayList( (BindingGenerator) new InternalSoapBindingGenerator( ) );
  }
}
