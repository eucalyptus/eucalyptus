package com.eucalyptus.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import com.eucalyptus.system.Ats;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Composites {
  private static Logger LOG = Logger.getLogger( Composites.class );
  private static ConcurrentMap<Class, CompositeHelper> subTypeCache = Maps.newConcurrentHashMap( );
  
  private static <T> CompositeHelper<T> build( Class<T> destType ) {
    List<Class> sourceTypes = Lists.newArrayList( );
    if ( !Ats.from( destType ).has( Composite.class ) ) {
      sourceTypes.add( destType );
    } else {
      sourceTypes = Arrays.asList( Ats.from( destType ).get( Composite.class ).value( ) );
    }
    CompositeHelper<T> helper = null;
    if ( !subTypeCache.containsKey( destType ) ) {
      helper = new CompositeHelper<T>( destType, sourceTypes );
      subTypeCache.putIfAbsent( destType, helper );
    } else {
      helper = subTypeCache.get( destType );
    }
    return helper;
  }  

  public static <A,B> B updateNulls( A source, B dest ) {
    return ( B ) CompositeHelper.updateNulls( source, dest );
  }

  public static <A,B> B update( A source, B dest ) {
    return ( B ) CompositeHelper.update( source, dest );
  }

  public static <T> T composeNew( Class<T> destType, Object... sources ) {
    T dest;
    try {
      dest = destType.newInstance( );
      return compose( dest, sources );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new RuntimeException( "Composition for " + destType.getCanonicalName( ) + " failed because of " + e.getMessage( ), e );
    }
  }
  
  public static <T> T compose( T dest, Object... sources ) {
    CompositeHelper<T> helper = Composites.build( (Class<T>)dest.getClass( ) );
    return helper.compose( dest, sources );
  }

  public static <A,B> B updateNew( A source, Class<B> destType ) {
    try {
      return ( B ) CompositeHelper.update( source, destType.newInstance( ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new RuntimeException( "Failed to update composable object because of: " + e.getMessage( ), e );
    }    
  }
  public static <T> List<Object> projectNew( T source, Class... destTypes ) {
    List<Object> dests = Lists.transform( Arrays.asList( destTypes ), new Function<Class,Object>( ) {
      @Override
      public Object apply( Class arg0 ) {
        try {
          return arg0.newInstance( );
        } catch ( Exception e ) {
          throw new RuntimeException( e );
        }
      }});
    return project( source, dests );
  }

  public static <T> List<Object> projectNew( T source ) {
    try {
      List<Object> dests = Lists.transform( subTypeCache.get( source ).getSourceTypes( ), new Function<Class,Object>() {
        @Override
        public Object apply( Class arg0 ) {
          try {
            return arg0.newInstance( );
          } catch ( Exception e ) {
            throw new RuntimeException( e );
          }
        }} );
      return project( source, dests );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new RuntimeException( "Projection for " + source.getClass( ).getCanonicalName( ) + " failed because of " + e.getMessage( ), e );
    }
  }
  
  public static <T> List<Object> project( T source, Object... dests ) {
    CompositeHelper<T> helper = ( CompositeHelper<T> ) Composites.build( source.getClass( ) );
    return helper.project( source, dests );
  }

}
