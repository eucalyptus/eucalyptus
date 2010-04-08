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
    destType.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each{ 
      vars[it.name]=it
    }
    def check=vars.clone()
    sources.each{ src -> 
      src.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each {  f ->
        check.remove(f.name)
      }
    }
    check.each{ k,v -> println "WARNING: the field ${destType.class.name}.${k} will not be set since it is not defined in any of ${args}"; }    
  }
  
  public T compose( T dest, Object... args ) {
    def props = vars.clone();
    args.each{ src ->
      src.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each {
        if( props.containsKey( it.name ) ) {
          LOG.debug("Accepting ${src.class.name}.${it.name} as ${dest.class.name}.${it.name}=${src[it.name]}");
          dest[it.name]=src[it.name];
          props.remove(it.name);
        } else {
          LOG.debug("WARNING: Ignoring ${src.class.name}.${it.name} as it is not in the destination type.");
        }
      }
    }
    dest.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class" }.each { LOG.debug("${dest.class.name}.${it.name} = ${dest[it.name]}"); }
    return dest;
  }
  
  public List<Object> project( T source, Object... args ) {
    args.each{ dest ->
      def props = dest.metaClass.properties.collect{ p -> p.name };
      source.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class"&&props.contains(it.name)&&source[it.name]!=null }.each { sourceField -> 
        dest[sourceField.name]=source[sourceField.name];
      }
    }
  }
  
  public static Object update( Object source, Object dest ) {
    source.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class"&&props.contains(it.name)&&source[it.name]!=null }.each{ sourceField ->
      dest[sourceField.name]=source[sourceField.name];
    }
  }
  
  public static Object updateNulls( Object source, Object dest ) {
    source.metaClass.properties.findAll{ it.name!="metaClass"&&it.name!="class"&&props.contains(it.name) }.each{ sourceField ->
      dest[sourceField.name]=source[sourceField.name];
    }
  }
  
}
