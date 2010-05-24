import edu.ucsb.eucalyptus.cloud.entities.AOEVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.BucketInfo;
import edu.ucsb.eucalyptus.cloud.entities.GrantInfo;
import edu.ucsb.eucalyptus.cloud.entities.MetaDataInfo;
import edu.ucsb.eucalyptus.cloud.entities.NSRecordInfo;
import edu.ucsb.eucalyptus.cloud.entities.ObjectInfo;
import edu.ucsb.eucalyptus.cloud.ws.WalrusControl;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.upgrade.StandalonePersistence;
import com.eucalyptus.upgrade.UpgradeScript;
import com.eucalyptus.util.WalrusProperties;

class UpgradeWalrus162eee implements UpgradeScript {
	static final String FROM_VERSION = "1.6.2";
	static final String TO_VERSION = "eee-2.0.0";
	private static Logger LOG = Logger.getLogger( UpgradeWalrus162eee.class );
	private static Map<String, Class> entityMap = new HashMap<String, Class>();

	@Override
	public Boolean accepts( String from, String to ) {
		if(FROM_VERSION.equals(from) && TO_VERSION.equals(to))
			return true;
		return false;
	}

	@Override
	public void upgrade(File oldEucaHome, File newEucaHome) {
		buildEntityMap();
		Set<String> entityKeys = entityMap.keySet();
		for (String entityKey : entityKeys) {
			String contextName = getContextName(entityKey);
			Sql conn = getConnection(contextName);
			if (conn != null) {
				Map<String, Method> setterMap = buildSetterMap(conn, entityKey);
				doUpgrade(contextName, conn, entityKey, setterMap);
			}
		}
	}

	private Sql getConnection(String contextName) {
		try {
			Sql conn = StandalonePersistence.getConnection(contextName);
			LOG.info("Getting context: " + contextName);
			return conn;
		} catch (SQLException e) {
			LOG.error(e);
			return null;
		}
	}

	private String getContextName(String entityKey) {
		Class entity = entityMap.get(entityKey);
		if (entity != null) {
			if(entity.isAnnotationPresent(PersistenceContext.class)) {
				PersistenceContext annot = (PersistenceContext) entity.getAnnotation(PersistenceContext.class);
				return annot.name();
			}
		}
		return null;		
	}

	private void doUpgrade(String contextName, Sql conn, String entityKey, Map<String, Method> setterMap) {
		List<GroovyRowResult> rowResults;
		try {
			rowResults = conn.rows("SELECT * FROM " + entityKey);
			EntityWrapper db =  new EntityWrapper(contextName);
			for (GroovyRowResult rowResult : rowResults) {
				Set<String> columns = rowResult.keySet();
				Object dest;
				try {
					dest = ClassLoader.getSystemClassLoader().loadClass(entityMap.get(entityKey).getCanonicalName()).newInstance();
				} catch (ClassNotFoundException e1) {
					LOG.error(e1);
					break;
				} catch (InstantiationException e) {
					LOG.error(e);
					break;
				} catch (IllegalAccessException e) {
					LOG.error(e);
					break;
				}
				for (String column : columns) {
					Method setter = setterMap.get(column);
					if(setter != null) {
						Object o = rowResult.get(column);
						if(o != null) {
							try {
								setter.invoke(dest, o);
							} catch (IllegalArgumentException e) {
								LOG.error(e);
							} catch (IllegalAccessException e) {
								LOG.error(e);
							} catch (InvocationTargetException e) {
								LOG.error(e);
							}
						}
					}
				}
				LOG.info("Upgraded: " + dest.getClass().getName());
				db.add(dest);
			}
			db.commit();
		} catch (SQLException e) {
			LOG.error(e);
			return;
		}		
	}

	private Map<String, Method> buildSetterMap(Sql conn, String entityKey) {
		Map<String, Method> setterMap = new HashMap<String, Method>();
		try {
			Object firstRow = conn.firstRow("SELECT * FROM " + entityKey);
			if(firstRow instanceof Map) {
				Set<String> columnNames = ((Map) firstRow).keySet();
				Class definingClass = entityMap.get(entityKey);
				Field[] fields = definingClass.getDeclaredFields();
				for(String column : columnNames) {
					for(Field f : fields) {
						if(f.isAnnotationPresent(Column.class)) {
							Column annotClass = (Column)f.getAnnotation(Column.class);						
							if(((String)column).toLowerCase().equals(annotClass.name().toLowerCase())) {
								String baseMethodName = f.getName( ).substring( 0, 1 ).toUpperCase( ) + f.getName( ).substring( 1 );
								try {
									Class[] classes = new Class[1];
									classes[0] = f.getType();
									Method setMethod = definingClass.getDeclaredMethod( "set" + baseMethodName, classes );
									setterMap.put(column, setMethod);
								} catch (SecurityException e) {
									LOG.error(e);
								} catch (NoSuchMethodException e) {
									LOG.error(e);
								}
								break;
							}
						}
					}
					if(setterMap.containsKey(column)) {
						LOG.info(column + " is set by: " + setterMap.get(column).getName());
					} else {
						LOG.info("No corresponding field found for: " + column);
					}
				}
			}
		} catch (SQLException e) {
			LOG.error(e);
		}
		return setterMap;
	}

	private void buildEntityMap() {
		entityMap.put("BUCKETS", BucketInfo.class);
		entityMap.put("OBJECTS", ObjectInfo.class);
		entityMap.put("GRANTS", GrantInfo.class);
		entityMap.put("METADATA", MetaDataInfo.class);
		entityMap.put("AOEVOLUMEINFO", AOEVolumeInfo.class);
		entityMap.put("NSRECORDS", NSRecordInfo.class);
	}

}