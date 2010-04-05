import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.*;
import javax.persistence.*;  

entity_list = []                   
euca_jar_files = new File(SubDirectory.LIB.toString()).listFiles().findAll{ it.name.matches(".*eucalyptus-.*\\.jar") };
euca_jars = euca_jar_files.collect{ new java.util.jar.JarFile( it ) };
euca_classes = euca_jars.collect{ it.entries().grep(~/.*eucalyptus[^$]*\.class.{0,1}/) };
euca_classes = euca_classes.collect{ clist -> clist.collect{ class_name = it.name.replaceAll("/",".").replaceAll("\\.class.{0,1}",""); } } ; 

euca_classes.each{ 
  clist -> entity_list.addAll( 
  clist.findAll{ 
    try{ 
      c = ClassLoader.getSystemClassLoader().loadClass( it )
      ( c.getAnnotation( javax.persistence.Entity.class ) != null \
          && c.getAnnotation( javax.persistence.PersistenceContext.class )?.name().equals("eucalyptus_${context_name}".toString()) ) \
      || c.getAnnotation( javax.persistence.MappedSuperclass.class ) != null \
      || c.getAnnotation( javax.persistence.Embeddable.class ) != null 
      } catch(Throwable t) { 
      false;
      }
  } ); 
};

entity_class_list = [ ClassLoader.getSystemClassLoader().loadClass("com.eucalyptus.entities.Counters") ]
entity_list.each{                  
  try {                            
    e = ClassLoader.getSystemClassLoader().loadClass( it );       
    entity_class_list.add e           
  } catch (Throwable t){
  }          
}                                  
entity_class_list 