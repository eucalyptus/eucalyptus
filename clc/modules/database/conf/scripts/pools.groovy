import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Component;
import org.logicalcobwebs.proxool.ProxoolFacade;

Class.forName('org.logicalcobwebs.proxool.ProxoolDriver');
poolProps = [
  'proxool.simultaneous-build-throttle': '16',
  'proxool.minimum-connection-count': '16',
  'proxool.maximum-connection-count': '128',
  /*'proxool.house-keeping-test-sql': 'select CURRENT_DATE',*/
  'user': 'sa',
  'password': ''/*Hashes.getHexSignature( )*/,
]
p = new Properties();
p.putAll(poolProps)
String dbDriver = 'org.hsqldb.jdbcDriver';
String url = "proxool.eucalyptus_${context_name}:${dbDriver}:${Component.db.uri.toASCIIString( )}_${context_name}";
ProxoolFacade.registerConnectionPool(url, p);

[
  'hibernate.connection.provider_class': 'org.hibernate.connection.ProxoolConnectionProvider',
  'hibernate.proxool.pool_alias': "eucalyptus_${context_name}",
  'hibernate.proxool.existing_pool': 'true'
]