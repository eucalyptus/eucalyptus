package com.eucalyptus.configurable;

import java.util.Map;
import org.apache.log4j.Logger;
import com.google.common.collect.Maps;

public abstract class PropertyTypeParser<T> {
  private static Logger LOG = Logger.getLogger( PropertyTypeParser.class );
  private static Map<Class,PropertyTypeParser> typeParsers = Maps.newHashMap( );
  private static PropertyTypeParser<Float> floatParser = new PropertyTypeParser<Float>( ) {
    @Override
    public Float parse( String property ) {
      return Float.parseFloat( property );
    }
  };
  private static PropertyTypeParser<Double> doubleParser = new PropertyTypeParser<Double>( ) {
    @Override
    public Double parse( String property ) {
      return Double.parseDouble( property );
    }
  };
  private static PropertyTypeParser<Integer> integerParser = new PropertyTypeParser<Integer>( ) {
    @Override
    public Integer parse( String property ) {
      return Integer.parseInt( property );
    }
  };
  private static PropertyTypeParser<Long> longParser = new PropertyTypeParser<Long>( ) {
    @Override
    public Long parse( String property ) {
      return Long.parseLong( property );
    }
  };
  private static PropertyTypeParser<Boolean> booleanParser = new PropertyTypeParser<Boolean>( ) {
    @Override
    public Boolean parse( String property ) {
      return Boolean.parseBoolean( property );
    }
  };
  private static PropertyTypeParser<String> stringParser = new PropertyTypeParser<String>( ) {
    @Override
    public String parse( String property ) {
      return property;
    }
  };
  static {
    typeParsers.put( Integer.class, integerParser );
    typeParsers.put( Long.class, longParser );
    typeParsers.put( Float.class, floatParser );
    typeParsers.put( Double.class, doubleParser );
    typeParsers.put( Boolean.class, booleanParser );
    typeParsers.put( String.class, stringParser );
  }

  public static void addTypeParser( Class c, PropertyTypeParser p ) {
    typeParsers.put( c, p );
  }
  
  public static PropertyTypeParser get( Class c ) {
    if( !typeParsers.containsKey( c ) || typeParsers.get( c ) == null ) {
      RuntimeException r = new RuntimeException("Invalid configurable type: " + c );
      LOG.fatal( r, r );
      throw r;
    } else {
      return typeParsers.get( c );      
    }
  }
  
  public abstract T parse( String property );
}
