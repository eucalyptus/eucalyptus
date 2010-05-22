import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.entities.PersistenceContexts;

def Example implements UpgradeScript {
  def accepts( String from, String to ) {
    return true;
  }
  def upgrade( File oldEucaHome, File newEucaHome ) {
    print "HOOOOOOOOOOOOOOOOOOORAH!!!11!!>!@!"
    PersistenceContexts.list().each{ String ctx ->
      print "Found persistence context:  ${ctx} with ${PersistenceContexts.listEntities().size()} registered entities."
    }
    StandalonePersistence.listConnections().each{ Sql conn ->
      print "Found jdbc connection:      ${conn.getConnection( ).getMetaData( ).getURL( )}"
    }
  }
}