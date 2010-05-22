import groovy.sql.Sql;
import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.entities.PersistenceContexts;

class Example implements UpgradeScript {
  public Boolean accepts( String from, String to ) {
    return true;
  }
  public void upgrade( File oldEucaHome, File newEucaHome ) {
    PersistenceContexts.list().each{ String ctx ->
      println "Found persistence context:  ${ctx} with ${PersistenceContexts.listEntities().size()} registered entities."
    }
    StandalonePersistence.listConnections().each{ Sql conn ->
      println "Found jdbc connection:      ${conn.getConnection( ).getMetaData( ).getURL( )}"
    }
  }
}