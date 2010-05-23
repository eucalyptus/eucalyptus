import groovy.sql.Sql;
import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.entities.EntityWrapper;

class Example implements UpgradeScript {
  public Boolean accepts( String from, String to ) {
    return true;
  }
  public void upgrade( File oldEucaHome, File newEucaHome ) {
    PersistenceContexts.list().each{ String ctx ->
      println "Found persistence context:  ${ctx} with ${PersistenceContexts.listEntities( ctx ).size( )} registered entities."
      PersistenceContexts.listEntities( ctx ).each{ ent ->
        println " - ${ent.toString()}"
      }
    }
    StandalonePersistence.listConnections().each{ Sql conn ->
      println "Found jdbc connection:      ${conn.getConnection( ).getMetaData( ).getURL( )}"
    }
    StandalonePersistence.getConnection("eucalyptus_general").rows('SELECT * FROM COUNTERS').each{ 
      println "Found old system counters:  msg_count=${it.MSG_COUNT}"
      EntityWrapper db = EntityWrapper.get( Counters.class );
      try {
        try {
          c = db.getUnique( Counters.uninitialized() );
          println "Found existing system counters: ${c.dump()}"
        } catch( Throwable t ) {
          Counters c = new Counters();
          c.setMessageId( it.MSG_COUNT );
          db.add( c );
        }
      } catch( Throwable t ) {
        t.printStackTrace( );
      } finally {
        db.commit();
      }
      println "Added new system counters:  ${c.dump()}"
    }
  }
}