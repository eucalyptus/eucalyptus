package com.eucalyptus.ws.binding;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.ws.BindingException;

public class BindingManager {

  private static Logger               LOG        = Logger.getLogger( BindingManager.class );
  private static Map<String, Binding> bindingMap = new HashMap<String, Binding>( );

  public static String sanitizeNamespace( String namespace ) {
    return namespace.replaceAll( "(http://)|(/$)", "" ).replaceAll( "[./-]", "_" );
  }

  public static Binding getBinding( final String bindingName ) throws BindingException {
    if ( BindingManager.bindingMap.containsKey( bindingName ) ) { return BindingManager.bindingMap.get( bindingName ); }
    final Binding newBinding = new Binding( bindingName );
    BindingManager.bindingMap.put( bindingName, newBinding );
    return newBinding;
  }

}
