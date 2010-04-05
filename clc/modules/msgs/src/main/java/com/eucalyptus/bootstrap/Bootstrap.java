package com.eucalyptus.bootstrap;

import static com.eucalyptus.system.Ats.From;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;
import com.eucalyptus.bootstrap.transitions.LoadConfigs;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Lifecycles;
import com.eucalyptus.component.Resource;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Committor;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.Transition;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EventRecord;

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
    private List<Resource>     temporaryResourceList = Lists.newArrayList( );
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
      LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_AGENDA, this.name( ), Bootstrap.loading ? "LOAD" : "START" ) );
      for ( Bootstrapper b : this.bootstrappers ) {
        LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_AGENDA, this.name( ), b.getClass( ).getCanonicalName( ) ) );
      }
    }
    
    private void updateBootstrapDependencies( ) {
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
    
    public void start( ) {
      this.updateBootstrapDependencies( );
      this.printAgenda( );
      for ( Bootstrapper b : this.bootstrappers ) {
        try {
          LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_START, this.name( ), b.getClass( ).getCanonicalName( ) ) );
          boolean result = b.start( );
          if ( !result ) {
            throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " returned 'false' from start( ): terminating bootstrap." );
          }
        } catch ( Throwable e ) {
          LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ERROR, this.name( ), b.getClass( ).getCanonicalName( ) ) );
          throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " threw an error in start( ): " + e.getMessage( ), e );
        }
      }
    }
    
    public void load( ) {
      this.updateBootstrapDependencies( );
      this.printAgenda( );
      for ( Bootstrapper b : this.bootstrappers ) {
        try {
          LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_LOAD, this.name( ), b.getClass( ).getCanonicalName( ) ) );
          boolean result = b.load( this );
          if ( !result ) {
            throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " returned 'false' from load( ): terminating bootstrap." );
          }
        } catch ( Throwable e ) {
          LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_ERROR, this.name( ), b.getClass( ).getCanonicalName( ) ) );
          throw BootstrapException.throwFatal( b.getClass( ).getSimpleName( ) + " threw an error in load( ): " + e.getMessage( ), e );
        }
      }
    }
    
    public String describe( ) {
      StringBuffer buf = new StringBuffer( );
      for ( Bootstrapper b : this.bootstrappers ) {
        buf.append( EventRecord.caller( Component.class, EventType.COMPONENT_INFO, this.name( ), b.getClass( ).getSimpleName( ) ) ).append( "\n" );
      }
      return buf.toString( );
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
      Bootstrap.Stage stage = From( bootstrap ).get( RunDuring.class ).value( );
      Provides p = From( bootstrap ).get( Provides.class );
      comp = ( p.value( ) == null ? old : p.value( ) );//TODO: remap orphan bootstrapper to 'any'
      if ( !comp.isDummy( ) && !comp.isEnabled( ) && Components.contains( comp ) ) { //report skipping a bootstrapper for an enabled component
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "Provides", comp.name( ),
                          "Component." + comp.name( ) + ".isEnabled", comp.isEnabled( ).toString( ) ).info( );
      } else if ( !bootstrap.checkLocal( ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "DependsLocal", comp.name( ),
                          "Component." + comp.name( ) + ".isLocal", comp.isLocal( ).toString( ) ).info( );
      } else if ( !bootstrap.checkRemote( ) ) {
        EventRecord.here( Bootstrap.class, EventType.BOOTSTRAPPER_SKIPPED, currentStage.name( ), bc, "DependsRemote", comp.name( ),
                          "Component." + comp.name( ) + ".isLocal", comp.isLocal( ).toString( ) ).info( );
      } else if ( !Components.contains( comp ) ) {
        throw BootstrapException.throwFatal( "Bootstrap class provides a component for which registration failed (" + comp.name( ) + "): " + bc );
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
      LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_COMPLETE, currentStage.toString( ) ) );
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
        LOG.info( EventRecord.here( Bootstrap.class, EventType.BOOTSTRAP_STAGE_SKIPPED, currentStage.name( ) ) );
        continue;
      } else {
        return currentStage;
      }
    }
    return currentStage;
  }
  
  public static Boolean isFinished( ) {
    return Bootstrap.Stage.Final.equals( Bootstrap.getCurrentStage( ) );
  }
  
  public static void initialize( ) throws Throwable {
    LOG.info( LogUtil.header( "Initializing component resources." ) );
    Transition.anonymous( LoadConfigs.class ).transition( Stage.list( ) );
    
    LOG.info( LogUtil.header( "Initializing singleton component services." ) );
    Lifecycles.State.DISABLED.to( Lifecycles.State.PRIMORDIAL, new Committor<com.eucalyptus.bootstrap.Component>( ) {
      @Override
      public void commit( com.eucalyptus.bootstrap.Component c ) throws Exception {
        if ( c.isDummy( ) ) {
          Components.create( c.name( ) );
        }
      }
    } ).transition( Components.delegate.list( ) );
    
    LOG.info( LogUtil.header( "Initializing component service configurations." ) );
    Lifecycles.State.PRIMORDIAL.to( Lifecycles.State.INITIALIZED, new Committor<Component>( ) {
      @Override
      public void commit( Component object ) throws Exception {
        Resource rsc = object.getConfiguration( ).getResource( );
        if( rsc != null ) {
          LOG.info( "Stage " + currentStage!=null?Bootstrap.getCurrentStage( ).name( ):"null" + " loaded component resources for: " + object + " @ " + rsc.getOrigin( ) );
          for ( ConfigResource cfg : rsc.getConfigurations( ) ) {
            LOG.info( "-> " + cfg.getUrl( ) );
          }          
        }
      }
    } ).transition( Components.list( ) );
    
    LOG.info( LogUtil.header( "Initializing discoverable bootstrap resources." ) );
    Bootstrap.doDiscovery( );
    
    LOG.info( LogUtil.header( "Preparing to initialize the system." ) );
    for ( Component c : Components.list( ) ) {
      LOG.info( c.describe( ) );
    }
    
    LOG.info( LogUtil.header( "Initializing bootstrappers." ) );
    Bootstrap.initBootstrappers( );
    
    LOG.info( LogUtil.header( "Initialized system: ready to start bootstrap." ) );
    for ( Component c : Components.list( ) ) {
      LOG.info( String.format( "Component.%-15.15s enabled=%-5.5s local=%-5.5s", c.getName( ), c.getLifecycle( ).isEnabled( ), c.getLifecycle( ).isLocal( ) ) );
      for ( Bootstrapper b : c.getConfiguration( ).getBootstrappers( ) ) {
        LOG.info( String.format( "- Component.%-13.13s bootstrapper=%s", c.name( ), b.getClass( ).getCanonicalName( ) ) );
      }
    }
    Lifecycles.State.INITIALIZED.to( Lifecycles.State.LOADED ).transition( Components.list( ) );
  }
  
}
