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
    }
  }

  public abstract void processClass( Class klass );
  public abstract void close( );
  private static List<BindingGenerator> preBindGenerators = Lists.newArrayList( (BindingGenerator) new InternalSoapBindingGenerator( )/*, new JsonDescriptorGenerator()*/ ); 
  private static List<BindingGenerator> postBindGenerators = Lists.newArrayList( ); 
  public static List<BindingGenerator> getPreGenerators( ) {
    return preBindGenerators;
  }
//  public static List<BindingGenerator> getPostGenerators( ) {
//    return postBindGenerators;
//  }
}
