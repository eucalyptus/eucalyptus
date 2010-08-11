package com.eucalyptus.bootstrap;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.transitions.LoadConfigs;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Lifecycles;
import com.eucalyptus.component.Resource;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Committor;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.Transition;
import com.google.common.collect.Lists;

public class Bootstrap {
  private static Logger LOG = Logger.getLogger( Bootstrap.class );
  
  public enum Stage {
    SystemInit,
    PrivilegedConfiguration,
    UnprivilegedConfiguration,
    SystemCredentialsInit, /* <-- this means system credentials, not user. */
    RemoteConfiguration,
    DatabaseInit,
    PersistenceContextInit,
    DeferredClassInit,
    RemoteServicesInit,
    UserCredentialsInit,
    CloudServiceInit,
    Verification,
    Anonymous,
    Final;
    public static List<Stage> list( ) {
      return Arrays.asList( Stage.values( ) );
    }
    
    public <A> Transition<A, Bootstrap.Stage> to( final Bootstrap.Stage s, final Committor<A> c ) throws Exception {
      return ( Transition<A, Bootstrap.Stage> ) Transition.anonymous( this, s, c );
    }
    
    private List<Bootstrapper> bootstrappers         = Lists.newArrayList( );
    private List<Resource>     resources             = Lists.newArrayList( );
    
    public <A, B extends Comparable<? extends Comparable<?>>> Transition<A,Comparable<? extends Comparable<?>>> to( B s, Committor<A> c ) throws Exception {
      return ( Transition<A,Comparable<? extends Comparable<?>>> ) Transition.anonymous( this, s, c );
    }
    
    public List<Bootstrapper> getBootstrappers( ) {
      return this.bootstrappers;
    }
    
    public void addBootstrapper( Bootstrapper b ) {
      if ( this.bootstrappers.contains( b ) ) {
        throw BootstrapException.throwFatal( "Duplicate bootstrapper registration: " + b.getClass( ).toString( ) );
      } else {
        this.bootstrappers.add( b );
      }
    }
    
    public void printAgenda( ) {
      EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_AGENDA, this.name( ), Bootstrap.loading ? "LOAD" : "START" ).info( );
      for ( Bootstrapper b : this.bootstrappers ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_AGENDA, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
      }
    }
    
    public void updateBootstrapDependencies( ) {
      for ( Bootstrapper b : Lists.newArrayList( this.bootstrappers ) ) {
        if ( !b.checkLocal( ) ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, "stage:" + Bootstrap.getCurrentStage( ), this.getClass( ).getSimpleName( ),
                            "Depends.local=" + b.toString( ), "Component." + b.toString( ) + "=remote" ).info( );
          this.bootstrappers.remove( b );
        } else if ( !b.checkRemote( ) ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, "stage:" + Bootstrap.getCurrentStage( ), this.getClass( ).getSimpleName( ),
                            "Depends.remote=" + b.toString( ), "Component." + b.toString( ) + "=local" ).info( );
          this.bootstrappers.remove( b );
        }
      }
    }
    
    public void load( ) {
      this.updateBootstrapDependencies( );
      this.printAgenda( );
      for ( Bootstrapper b : this.bootstrappers ) {
        try {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_LOAD, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          boolean result = b.load( this );
          if ( !result ) {
            throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " returned 'false' from load( ): terminating bootstrap." );
          }
        } catch ( Throwable e ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ERROR, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " threw an error in load( ): " + e.getMessage( ), e );
        }
      }
    }

    public void start( ) {
      this.updateBootstrapDependencies( );
      this.printAgenda( );
      for ( Bootstrapper b : this.bootstrappers ) {
        try {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_START, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          boolean result = b.start( );
          if ( !result ) {
            throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " returned 'false' from start( ): terminating bootstrap." );
          }
        } catch ( Throwable e ) {
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ERROR, this.name( ), b.getClass( ).getCanonicalName( ) ).info( );
          throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " threw an error in start( ): " + e.getMessage( ), e );
        }
      }
    }
        
    public String describe( ) {
      StringBuffer buf = new StringBuffer( this.name( ) ).append( " " );
      for ( Bootstrapper b : this.bootstrappers ) {
        buf.append( b.getClass( ).getSimpleName( ) ).append( " " );
      }
      return buf.append( "\n" ).toString( );
    }
    
    public String getResourceName( ) {
      return String.format( "com.eucalyptus.%sProvider", this.name( ).replaceAll( "Init\\Z", "" ) );
    }
    
    public List<Resource> getResources( ) {
      return this.resources;
    }
    
  }
  
  private static Boolean loading      = false;
  private static Boolean starting     = false;
  private static Boolean finished     = false;
  private static Stage   currentStage = Stage.SystemInit;
  
  public static Stage getCurrentStage( ) {
    return currentStage;
  }
  
  private static void doDiscovery( ) {
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( com.eucalyptus.bootstrap.Component.eucalyptus.name( ) ) && f.getName( ).endsWith( ".jar" )
           && !f.getName( ).matches( ".*-ext-.*" ) ) {
        LOG.debug( "Found eucalyptus component jar: " + f.getName( ) );
        try {
          ServiceJarDiscovery.processFile( f );
        } catch ( Throwable e ) {
          LOG.error( e.getMessage( ) );
          continue;
        }
      }
    }
    ServiceJarDiscovery.runDiscovery( );
  }
  
  @SuppressWarnings( "deprecation" )
  public static void initBootstrappers( ) {
    for ( Bootstrapper bootstrap : BootstrapperDiscovery.getBootstrappers( ) ) {//these have all been checked at discovery time
      com.eucalyptus.bootstrap.Component comp, old = com.eucalyptus.bootstrap.Component.any;
      String bc = bootstrap.getClass( ).getCanonicalName( );
      Bootstrap.Stage stage = Ats.from( bootstrap ).get( RunDuring.class ).value( );
      Provides p = Ats.from( bootstrap ).get( Provides.class );
      comp = ( p.value( ) == null ? old : p.value( ) );//TODO: remap orphan bootstrapper to 'any'
      if ( Components.delegate.any.equals( comp ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, currentStage.name( ), bc, "Provides", comp.name( ),
                          "Component." + comp.name( ) + ".isEnabled", "true" ).info( );
        stage.addBootstrapper( bootstrap );
      } else if ( !comp.isCloudLocal( ) && !comp.isEnabled( ) && Components.contains( comp ) ) { //report skipping a bootstrapper for an enabled component
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "Provides", comp.name( ),
                          "Component." + comp.name( ) + ".isEnabled", comp.isEnabled( ).toString( ) ).info( );
      } else if ( !bootstrap.checkLocal( ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "DependsLocal", comp.name( ),
                          "Component." + comp.name( ) + ".isLocal", comp.isLocal( ).toString( ) ).info( );
      } else if ( !bootstrap.checkRemote( ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "DependsRemote", comp.name( ),
                          "Component." + comp.name( ) + ".isLocal", comp.isLocal( ).toString( ) ).info( );
      } else if ( !Components.contains( comp ) ) {
        Exceptions.eat( "Bootstrap class provides a component for which registration failed: " + bc + " provides " + comp.name( ) );
        //        throw BootstrapException.throwFatal
        try {
          Component realComponent = Components.create( comp.name( ), null );
          EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, currentStage.name( ), bc, "Provides", comp.name( ),
                            "Component." + comp.name( ) + ".isEnabled", comp.isEnabled( ).toString( ) ).info( );
          realComponent.getConfiguration( ).addBootstrapper( bootstrap );
          stage.addBootstrapper( bootstrap );          
        } catch ( ServiceRegistrationException ex ) {
          LOG.error( ex , ex );
        }
      } else {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ADDED, currentStage.name( ), bc, "Provides", comp.name( ),
                          "Component." + comp.name( ) + ".isEnabled", comp.isEnabled( ).toString( ) ).info( );
        Component realComponent = Components.lookup( comp );
        realComponent.getConfiguration( ).addBootstrapper( bootstrap );
        stage.addBootstrapper( bootstrap );
      }
    }
  }
  
  public static Stage transition( ) {
    if ( currentStage == Stage.SystemInit && !loading && !starting && !finished ) {
      loading = true;
      starting = false;
      finished = false;
    } else if ( currentStage != null ) {
      EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_COMPLETE, currentStage.toString( ) ).info( );
      if ( Stage.Final.equals( currentStage ) ) {
        currentStage = null;
        if ( loading && !starting && !finished ) {
          loading = true;
          starting = true;
          finished = false;
        } else if ( loading && starting && !finished ) {
          loading = true;
          starting = true;
          finished = true;
        }
        return currentStage;
      }
    }
    int currOrdinal = currentStage != null ? currentStage.ordinal( ) : -1;
    for ( int i = currOrdinal + 1; i <= Stage.Final.ordinal( ); i++ ) {
      currentStage = Stage.values( )[i];
      if ( currentStage.bootstrappers.isEmpty( ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_SKIPPED, currentStage.name( ) ).info( );
        continue;
      } else {
        return currentStage;
      }
    }
    return currentStage;
  }
  
  public static Boolean isFinished( ) {
    return finished;
  }

  public static void describeComponents( String message ) {
    LOG.info( LogUtil.header( message ) );
    
  }
  
  public static void initialize( ) throws Throwable {
    LOG.info( LogUtil.header( "Initializing component resources." ) );
    Transition.anonymous( LoadConfigs.class ).transition( Stage.list( ) );
    for ( Component c : Components.list( ) ) {
      LOG.info( c.toString( ) );
    }
        
    LOG.info( LogUtil.header( "Initializing discoverable bootstrap resources." ) );
    Bootstrap.doDiscovery( );

    LOG.info( LogUtil.header( "Initializing local singleton component services." ) );
    Lifecycles.State.PRIMORDIAL.to( Lifecycles.State.INITIALIZED, new Committor<Component>( ) {
      @Override
      public void commit( Component comp ) throws Exception {
        if( ( comp.getPeer( ).isEnabled( ) && comp.getPeer( ).isAlwaysLocal( ) ) || ( Components.delegate.eucalyptus.isLocal( ) && comp.getPeer( ).isCloudLocal( ) ) ){
          comp.buildService( );
        }
      }
    } ).transition( Components.list( ) );

    Bootstrap.describeComponents( "Preparing to initialize the system." );

    LOG.info( LogUtil.header( "Initializing bootstrappers." ) );
    Bootstrap.initBootstrappers( );
    
    Bootstrap.describeComponents( "Initialized system: ready to start bootstrap." );
  }
  
}
