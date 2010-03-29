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
  public boolean processsClass( Class candidate ) throws Throwable {
    try {
      Field f = candidate.getDeclaredField( "JiBX_bindingList" );
      String bindingList = ( String ) f.get( null );
      List<String> bindings = Lists.transform( Arrays.asList( bindingList.split( "\\|" ) ), new Function<String,String>() {
        @Override
        public String apply( String arg0 ) {
          return BindingManager.sanitizeNamespace( arg0.replaceAll(".*JiBX_","").replaceAll("Factory","") );
        }        
      });
      for( String binding : bindings ) {
        if( binding.length( ) > 2 ) {
          BindingManager.seedBinding( binding, candidate );
        }
      }
      return true;
    } catch ( Throwable t ) {
    }
    return false;
  }
  
}
