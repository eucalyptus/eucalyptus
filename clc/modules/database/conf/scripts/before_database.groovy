import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.entities.PersistenceContexts;
/* this crap is hsqldb specific */
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.*;

//Component.db.markLocal( );
//Component.db.markEnabled( );
//Component.db.setHostAddress( "127.0.0.1" );
props = [
  "hsqldb.script_format":"0",
  "runtime.gc_interval":"0",
  "sql.enforce_strict_size":"false",
  "hsqldb.cache_size_scale":"8",
  "readonly":"false",
  "hsqldb.nio_data_file":"true",
  "hsqldb.cache_scale":"14",
  "version":"1.8.0",
  "hsqldb.default_table_type":"memory",
  "hsqldb.cache_file_scale":"1",
  "hsqldb.log_size":"10",
  "modified":"no",
  "hsqldb.cache_version":"1.7.0",
  "hsqldb.original_version":"1.8.0",
  "hsqldb.compatible_version":"1.8.0",
]
config = "CREATE SCHEMA PUBLIC AUTHORIZATION DBA\n" + 
  "CREATE USER SA PASSWORD \"${Hmacs.generateSystemSignature( )}\"\n" +  
  "GRANT DBA TO SA\n" + 
  "SET WRITE_DELAY 100 MILLIS\n" +  
  "SET SCHEMA PUBLIC\n";
PersistenceContexts.list( ).each{ context_name ->
  d = new File("${SubDirectory.DB.toString()}/${context_name}.script");
  p = new File("${SubDirectory.DB.toString()}/${context_name}.properties");
  if( !d.exists() ) {
    d.write(config);
  }
  if( !p.exists() ) {
    Properties prop = new Properties();
    prop.putAll( props );
    prop.store( p.newWriter(), "-- Eucalyptus generated HSQLDB configuration --" );
  }
}
