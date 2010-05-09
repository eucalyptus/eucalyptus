package com.eucalyptus.util;

import org.apache.log4j.Logger;

public class CompositeHelper<T> {
  private static Logger LOG = Logger.getLogger( CompositeHelper.class ); 
  private Class<T> destType;
  List<Class> sourceTypes;
  def vars = [:]
  public CompositeHelper( Class<T> destType, List<Class> sources ) {
    this.destType = destType;
    this.sourceTypes = sources;
    this.vars = makeProps( destType );
    def check=vars.clone()
    sources.each{ src -> 
      src.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each {  f ->
        check.remove(f.name)
      }
    }
    check.each{ k,v -> LOG.debug( "WARNING: the field ${destType.class.name}.${k} will not be set since it is not defined in any of ${args}" ); }    
  }
  
  def makeProps( arg ) {
    def type = arg;
    def props = [:]
    if( !arg.respondsTo( "getDelegate", null ).isEmpty() ) {
      type = type.getDelegate();
    }
    type.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each{ 
      props[it.name]=it
    }
    return props;
  }
  
  public T compose( T dest, Object... args ) {
    def destProps = vars.clone();
    args.each{ src ->
      def srcProps = makeProps( src );
      srcProps.each {
        if( destProps.containsKey( it.name ) ) {
          LOG.debug("${src.class.simpleName}.${it.name} as ${dest.class.simpleName}.${it.name}=${src[it.name]}");
          dest[it.name]=src[it.name];
          destProps.remove(it.name);
        } else {
          LOG.trace("WARNING: Ignoring ${src.class.name}.${it.name} as it is not in the destination type.");
        }
      }
    }
    destProps.each { LOG.debug("${dest.class.simpleName}.${it.name} = ${dest[it.name]}"); }
    return dest;
  }
  
  public List<Object> project( T source, Object... args ) {
    def srcProps = makeProps( source );
    args.each{ dest ->
      def destProps = makeProps( dest ).collect{ p -> p.name }
      srcProps.findAll{ destProps.contains(it.name)&&source[it.name]!=null }.each { sourceField -> 
        LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
        dest[sourceField.name]=source[sourceField.name];
      }
    }
    return Arrays.asList(args);
  }
  
  public static Object update( Object source, Object dest ) {
    def destProps = makeProps( dest ).collect{ p -> p.name };
    def srcProps = makeProps( source );
    srcProps.findAll{ destProps.contains(it.name)&&source[it.name]!=null }.each{ sourceField ->
      LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
      dest[sourceField.name]=source[sourceField.name];
    }
    return dest;
  }
  
  public static Object updateNulls( Object source, Object dest ) {
    def destProps = makeProps( dest ).collect{ p -> p.name };
    def srcProps = makeProps( source );
    srcProps.findAll{ destProps.contains(it.name) }.each{ sourceField ->
      LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
      dest[sourceField.name]=source[sourceField.name];
    }
    return dest;
  }
  
}
