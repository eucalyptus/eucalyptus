package com.eucalyptus.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Ats;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

public class TypeMappers {
  private enum CompareClasses implements Comparator<Class> {
    INSTANCE;
    
    @Override
    public int compare( Class o1, Class o2 ) {
      if ( o1 == null && o2 == null ) {
        return 0;
      } else if ( o1 != null && o2 != null ) {
        return ( "" + o1.toString( ) ).compareTo( "" + o2.toString( ) );
      } else {
        return ( o1 != null
          ? 1
          : -1 );
      }
    }
    
  }
  
  private static Logger                          LOG          = Logger.getLogger( TypeMappers.class );
  private static SortedSetMultimap<Class, Class> knownMappers = TreeMultimap.create( CompareClasses.INSTANCE, CompareClasses.INSTANCE );
  private static Map<String, Function>           mappers      = Maps.newHashMap( );
  
  public static <A, B> B transform( A from, Class<B> to ) {
    Class target = from.getClass( );
    for ( Class p : Classes.ancestors( from ) ) {
      if ( knownMappers.containsKey( p ) && !knownMappers.get( p ).isEmpty( ) ) {
        target = p;
        break;
      }
    }
    Function func = lookup( target, to );
    return ( B ) func.apply( from );
  }
  
  public static <A, B> Function<A, B> lookup( Class<A> a, Class<B> b ) {
    assertThat( knownMappers.keySet( ), hasItem( a ) );
    assertThat( knownMappers.get( a ), hasItem( b ) );
    String key = Joiner.on( "=>" ).join( a, b );
    return mappers.get( key );
  }
  
  public static class TypeMapperDiscovery extends ServiceJarDiscovery {
    
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( Ats.from( candidate ).has( TypeMapper.class ) && Function.class.isAssignableFrom( candidate ) ) {
        TypeMapper mapper = Ats.from( candidate ).get( TypeMapper.class );
        Class[] types = mapper.value( );
        List<Class> generics = Lists.newArrayList( );
        try {
          generics.addAll( Classes.genericsToClasses( Classes.newInstance( candidate ) ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
        if ( generics.size( ) != 2 ) {
          LOG.error( candidate + " looks like it is a @TypeMapper but needs generics: "
                     + generics );
          return false;
        } else {
          try {
            registerMapper( generics.get( 0 ), generics.get( 1 ), ( Function ) Classes.newInstance( candidate ) );
            return true;
          } catch ( Exception ex1 ) {
            LOG.error( ex1, ex1 );
          }
        }
      }
      return false;
    }
    
    @Override
    public Double getPriority( ) {
      return 0.3d;
    }
    
  }
  
  private static void registerMapper( Class from, Class to, Function mapper ) {
    EventRecord.here( TypeMapperDiscovery.class, EventType.BOOTSTRAP_INIT_DISCOVERY, "mapper", from.getCanonicalName( ), to.getCanonicalName( ),
                      mapper.getClass( ).getCanonicalName( ) ).info( );
    String key = Joiner.on( "=>" ).join( from, to );
    assertThat( knownMappers.get( from ), not( hasItem( to ) ) );
    assertThat( mappers, not( hasKey( key ) ) );
    knownMappers.put( from, to );
    mappers.put( key, mapper );
    
  }
}
