package com.eucalyptus.upgrade;

import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.Parameterized.Parameters;

import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;

public class TestHarness
{
	 public static Integer                            LINE_BYTES      = 0;
	  static {
	    String defaultLevel = System.getProperty( "verbose" );
	    String columns = System.getenv( "COLUMNS" );
	    System.setProperty( "test.output.console", defaultLevel!=null?"FATAL":"WARN" );
	    System.setProperty( "test.output.terminal", defaultLevel!=null?defaultLevel:"FATAL" );
	    if ( columns != null && !"".equals( columns ) ) {
	      LINE_BYTES = Integer.parseInt( System.getenv( "COLUMNS" ) );
	      System.setProperty( "test.output.terminal", defaultLevel!=null?defaultLevel:"INFO" );
	    } else {
	      LINE_BYTES = 85;
	      System.setProperty( "test.output.console", "INFO" );
	    }
	  }
	  
	  public static int                                count           = 1;
	  private static Logger                            LOG             = Logger.getLogger( TestHarness.class );
	  private static List<Class<? extends Annotation>> testAnnotations = Lists.newArrayList( Before.class, After.class, Test.class, Parameters.class );
	  
	  @SuppressWarnings( { "static-access", "unchecked" } )
	  public static void main( String[] args ) throws Throwable {
		System.out.println( Arrays.asList(args));
	    final Options opts = getCliOptions( );
	    final GnuParser cliParser = new GnuParser( );
	    final CommandLine cmd = cliParser.parse( opts, args );
	    
	    boolean anyFail = false;
	    try {
	      anyFail = Iterables.all( Lists.newArrayList( Opts.values( ) ), new Predicate<Opts>( ) {
	        public boolean apply( Opts o ) {
	          return !o.run( cmd, opts );
	        }
	      } );
	    } catch ( Exception e ) {
	      LOG.fatal( e, e );
	      System.exit( 1 );
	    }
	    System.exit( anyFail ? 1 : 0 );
	  }
	  
	  enum Opts {
	    help {
	      @Override
	      public boolean run( CommandLine cmd, Options opts ) {
	        if ( cmd.hasOption( Opts.help.toString( ) ) || !cmd.getArgList( ).isEmpty( ) || cmd.getOptionValues( "test" ) == null ) {
	          printHelp( opts );
	          System.exit( 0 );
	        }
	        return true;
	      }
	    },
	    typica {
	        @Override
	        public boolean run( CommandLine cmd, Options opts ) {
	          if ( cmd.hasOption( this.name( ) ) ) {
				try {
					StandalonePersistence.setupSystemProperties();
					StandalonePersistence.setupConfigurations();
					StandalonePersistence.setupInitProviders();
					StandalonePersistence.runDiscovery();
					StandalonePersistence.setupProviders();
					StandalonePersistence.setupNewDatabase();
				} catch (Exception e) {
					throw new RuntimeException("Standalone persistenec setup failed", e);
				}

	            List<Class> testList = getTestList( cmd, opts );
	            return TestHarness.runTests( testList );
	          } else {
	            return true;
	          }
	        }
	      };

	    public abstract boolean run( CommandLine cmd, Options opts );
	  }
	  
	  @SuppressWarnings( "static-access" )
	  private static Options getCliOptions( ) {
	    final Options opts = new Options( );
	    opts.addOption( OptionBuilder.withLongOpt( "test" ).hasArgs( ).withDescription( "Run the specified test; this option can appear multiple times. Parameters can be set as key=value pairs. e.g. ConcurrencyTest:threads=16" ).withArgName( "TEST_CLASS[:PARAM=VALUE]*" ).create( "t" ) );
	    opts.addOption( OptionBuilder.withLongOpt( Opts.help.name( ) ).withDescription( "Show this help information." ).create( "h" ) );
	    return opts;
	  }
	  
	  @SuppressWarnings( { "unchecked", "static-access" } )
	  private static void printHelp( Options opts ) {
	    try {
	      PrintWriter out = new PrintWriter( System.out );
	      HelpFormatter help = new HelpFormatter( );
	      help.setArgName( "TESTS" );
	      help.printHelp( out, LINE_BYTES, "java -jar test.jar", "Options controlling the test harness.", opts, 2, 4, "", true );
	      out.flush( );
	      Multimap<Class, Method> testMethods = getTestMethods( );
	      help = new HelpFormatter( );
	      help.setLongOptPrefix( "" );
	      help.setOptPrefix( "" );
	      help.setSyntaxPrefix( "" );
	      help.setLeftPadding( 0 );
	      Options testOptions = new Options( );
	      for ( Class c : testMethods.keySet( ) ) {
	        testOptions.addOption( OptionBuilder.withDescription( getDescription( c ) ).withLongOpt( c.getSimpleName( ) ).create( ) );
	        for ( Method m : testMethods.get( c ) ) {
	          testOptions.addOption( OptionBuilder.withDescription( getDescription( m ) ).withLongOpt( c.getSimpleName( ) + "." + m.getName( ) ).create( ) );
	        }
	      }
	      help.printHelp( out, LINE_BYTES, " ", "Tests:", testOptions, 0, 2, "", false );
	      out.flush( );
	    } catch ( Exception e ) {
	      LOG.error( e, e );
	      System.exit( 1 );
	    }
	  }
	  
	  @SuppressWarnings( "unchecked" )
	  private static String getDescription( Object o ) {
	    Class c = null;
	    Method m = null;
	    if ( o instanceof Class && ( ( c = ( Class ) o ).getAnnotation( TestDescription.class ) ) != null ) {
	      return ( ( TestDescription ) c.getAnnotation( TestDescription.class ) ).value( );
	    } else if ( o instanceof Method && ( ( m = ( Method ) o ).getAnnotation( TestDescription.class ) ) != null ) {
	      StringBuffer sb = new StringBuffer( );
	      for ( Class a : Lists.newArrayList( Before.class, After.class, Test.class, Ignore.class, Parameters.class ) ) {
	        if ( m.getAnnotation( a ) != null ) sb.append( "  @" ).append( String.format( "%-9.9s", a.getSimpleName( ) ) ).append( " " );
	      }
	      return sb.append(" ").append( ( ( TestDescription ) m.getAnnotation( TestDescription.class ) ).value( ) ).toString( );
	    }
	    return "";
	  }
	  
	  @SuppressWarnings( "unchecked" )
	  private static Multimap<Class, Method> getTestMethods( ) throws Exception {
	    final Multimap<Class, Method> testMethods = Multimaps.newArrayListMultimap();
	    List<Class> classList = Lists.newArrayList( );
	    for ( File f : new File( System.getProperty("euca.home") + "/usr/share/eucalyptus" ).listFiles( ) ) {
	      if ( f.getName( ).startsWith( "eucalyptus" ) && f.getName( ).endsWith( ".jar" ) && !f.getName( ).matches( ".*-ext-.*" ) ) {
	        try {
	          JarFile jar = new JarFile( f );
	          Enumeration<JarEntry> jarList = jar.entries( );
	          while ( jarList.hasMoreElements( ) ) {
	            JarEntry j = jarList.nextElement( );
	            if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
	              String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
	              try {
	                Class candidate = ClassLoader.getSystemClassLoader( ).loadClass( classGuess );
	    			for (final Method m : candidate.getDeclaredMethods()) {
	    				if (Iterables.any(testAnnotations,
	    						new Predicate<Class<? extends Annotation>>()
	    						{
	    							public boolean apply(Class<? extends Annotation> arg0)
	    							{
	    								return m.getAnnotation(arg0) != null;
	    							}
	    						})) {
	    					LOG.info( "Added test class: " + candidate.getCanonicalName() );
	    					testMethods.put(candidate, m);
	    				}
	    			}
	              } catch ( ClassNotFoundException e ) {}
	            }
	          }
	          jar.close( );
	        } catch ( Throwable e ) {
	          LOG.error( e.getMessage( ) );
	          continue;
	        }
	      }
	    }
	    return testMethods;
	  }
	  
	  public static boolean runTests( List<Class> tests ) {
	    JUnitCore core = new JUnitCore( );
//	    core.run( Cleanup.class );
//	    core = new JUnitCore( );
	    core.addListener( new TestListener( ) );
	    tests = Lists.newArrayList( tests );
//	    tests.add( Cleanup.class );
	    Result res = core.run( ( Class<?>[] ) tests.toArray( new Class[] {}) );
	    return res != null ? res.wasSuccessful( ) : false;
	  }
	  
	  @SuppressWarnings( "unchecked" )
	  private static List<Class> getTestList( final CommandLine cmd, final Options opts ) {
	    List<Class> testList = Lists.transform( Lists.newArrayList( cmd.getOptionValues( "test" ) ), new Function<String, Class>( ) {
	      public Class apply( String arg ) {
	        String[] argParts = arg.split(":");
	        String className = argParts[0];
	        Class targetClass = null;
	        try {
	          targetClass = Class.forName( className );
	        } catch ( Exception e ) {
	          try {
	            targetClass = Class.forName( className + "Test" );
	          } catch ( Exception e1 ) {
	          }
	        }
	        if( targetClass == null ) {
	          printHelp( opts );
	          System.exit( 1 );
	        } else {
	          for( int i = 1; i < argParts.length; i++ ) {
	            String property = argParts[i].replaceAll("=.*","");
	            String value = argParts[i].replaceAll(".*=","");
	            try {
	              targetClass.getDeclaredMethod( property, String.class ).invoke( null, value );
	            } catch ( Exception e ) {
	              LOG.fatal( e, e );
	              System.exit( 1 );
	            }
	          }
	        }
	        return targetClass;
	      }
	    } );
	    return testList;
	  }
}
