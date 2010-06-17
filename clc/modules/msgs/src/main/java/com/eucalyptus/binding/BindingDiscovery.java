package com.eucalyptus.binding;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class BindingDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( BindingDiscovery.class );

  public BindingDiscovery( ) {}

  @Override
  public Double getPriority( ) {
    return 0.9;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    Field f;
    String bindingList;
    try {
      f = candidate.getDeclaredField( "JiBX_bindingList" );
      bindingList = ( String ) f.get( null );
    } catch ( Exception e ) {
      return false;
    }
    List<String> bindings = Lists.transform( Arrays.asList( bindingList.split( "\\|" ) ), new Function<String,String>() {
      @Override
      public String apply( String arg0 ) {
        return BindingManager.sanitizeNamespace( arg0.replaceAll(".*JiBX_","").replaceAll("Factory","") );
      }        
    });
    boolean seeded = false;
    for( String binding : bindings ) {
      if( binding.length( ) > 2 ) {
        try {
          seeded |= BindingManager.seedBinding( binding, candidate );
        } catch ( Exception e ) {
        }
      }
    }
    return seeded;
  }
  
}
