package com.eucalyptus.component;

import java.net.URI;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public class ComponentFactory {
  private static Logger LOG = Logger.getLogger( ComponentFactory.class );
  
  public static class Child extends Component {
    private Component  parent;
    private String     childName;
    private Dispatcher dispatcher;
    
    private Child( Component parent, String childName, Dispatcher dispatcher ) {
      super( parent.getName( ) );
      this.childName = childName;
      this.dispatcher = dispatcher;
    }
    
    public String getName( ) {
      return this.parent.getName( ) + "@" + this.childName;
    }
    
    @Override
    public Component getChild( String childName ) {
      return this.parent.getChild( childName );
    }
    
    public final Dispatcher getDispatcher( ) {
      return this.dispatcher;
    }

    @Override
    public int compareTo( Component that ) {
      return this.getName( ).compareTo( that.getName( ) );
    }
    
  }
  
  public static class Parent extends Component {
    private ComponentFactory.Child   child;
    private final Map<String, Child> children = Maps.newConcurrentHashMap( );
    
    public Parent( String name ) {
      super( name );
    }

    public Parent( String name, URI configuration ) {
      super( name, configuration );
    }
    
    public Component getChild( String hostName ) {
      Exceptions.ifNullArgument( hostName );
      String key = getChildKey( hostName );
      if ( this.children.containsKey( key ) ) {
        return this.children.get( key );
      } else {
        Dispatcher dispatcher = DispatcherFactory.build( this, hostName );
        Child c = new Child( this, hostName, dispatcher );
        LOG.info( EventRecord.caller( Component.class, EventType.COMPONENT_CHILD, this.getName( ), c.getName( ), c.getLifecycle( ).getUri( ).toString( ) ) );
        return this.children.put( c.getName( ), c );
      }
    }
    
    public Dispatcher getDispatcher( ) {
      if ( this.isSingleton( ) ) {
        return this.child.getDispatcher( );
      } else {
        throw Exceptions.fatal( "It is never OK to try to get the dispatcher from the parent component." );
      }
    }
    
    @Override
    public int compareTo( Component that ) {
      return this.getName( ).compareTo( that.getName( ) );
    }
    
  }
  
}
