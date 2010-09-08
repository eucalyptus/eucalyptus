package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.EntityWrapper;

public class MultiDatabasePropertyEntry extends AbstractConfigurableProperty implements ConfigurableProperty {
	private static Logger LOG = Logger.getLogger( MultiDatabasePropertyEntry.class );
	protected String        baseMethodName;
	protected Method        get;
	protected Method        set;
	protected Method		setIdentifier;
	protected Field			field;
	protected Field			identifierField;
	protected String		identifierValue;
	protected String        persistenceContext;
	protected Class[]       setArgs;

	public MultiDatabasePropertyEntry( Class definingClass, String entrySetName, Field field, Field identifierField, String description, String defaultValue, PropertyTypeParser typeParser,
			Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias, String identifierValue ) {
		super( definingClass, entrySetName, field.getName( ), defaultValue, description, typeParser, readOnly, displayName, widgetType, alias );
		this.identifierField = identifierField;
		this.field = field;
		this.baseMethodName = field.getName( ).substring( 0, 1 ).toUpperCase( ) + field.getName( ).substring( 1 );
		String identifiedMethodName = identifierField.getName( ).substring( 0, 1 ).toUpperCase( ) + identifierField.getName( ).substring( 1 );
		this.identifierValue = identifierValue;
		this.persistenceContext = ( ( PersistenceContext ) definingClass.getAnnotation( PersistenceContext.class ) ).name( );
		this.setArgs = new Class[] { field.getType( ) };
		try {
			get = definingClass.getDeclaredMethod( "get" + this.baseMethodName );
			set = definingClass.getDeclaredMethod( "set" + this.baseMethodName, this.setArgs );
			setIdentifier = definingClass.getDeclaredMethod( "set" + identifiedMethodName, new Class[] { identifierField.getType()} );
		} catch ( Exception e ) {
			LOG.debug( e, e );
		}
	}

	private Method getSetter( ) {
		if( this.set != null ) {
			return this.set;
		} else {
			synchronized(this) {
				if( this.set == null ) {
					try {
						this.set = this.getDefiningClass( ).getDeclaredMethod( "set" + this.baseMethodName, this.setArgs );
					} catch ( Exception e ) {
						LOG.debug( "Known methods: " + this.getDefiningClass( ).getDeclaredMethods( ) );
						LOG.debug( "Known methods: " + this.getDefiningClass( ).getMethods( ) );
						LOG.debug( e, e );
					}
				}
			}
			return this.set;
		}
	}
	private Method getGetter( ) {
		if ( this.get != null ) {
			return this.get;
		} else {
			synchronized ( this ) {
				if ( this.get == null ) {
					try {
						this.get = this.getDefiningClass( ).getDeclaredMethod( "get" + this.baseMethodName );
					} catch ( Exception e ) {
						LOG.debug( "Known methods: " + this.getDefiningClass( ).getDeclaredMethods( ) );
						LOG.debug( "Known methods: " + this.getDefiningClass( ).getMethods( ) );
						LOG.debug( e, e );
					}
				}
			}
			return this.get;
		}
	}

	private Object getQueryObject( ) throws Exception {
		Object queryObject = super.getDefiningClass( ).newInstance( );
		setIdentifier.invoke(queryObject, identifierValue);
		return queryObject;
	}

	@Override
	public String getValue( ) {
		EntityWrapper db = new EntityWrapper( this.persistenceContext );
		try {
			Object o = db.getUnique( this.getQueryObject( ) );
			Method getter = this.getGetter( );
			Object prop = null;
			if ( getter != null ) {
				prop = getter.invoke( o );
			}
			String result = prop != null ? prop.toString( ) : "null";
			db.commit( );
			return result;
		} catch ( Exception e ) {
			db.rollback( );
			return "Unknown";
		}
	}

	@Override
	public String setValue( String s ) {
		EntityWrapper db = new EntityWrapper( this.persistenceContext );
		try {
			Object o = db.getUnique( this.getQueryObject( ) );
			Object prop = this.getTypeParser( ).parse( s );
			Method setter = this.getSetter( );
			if ( setter != null ) {
				setter.invoke( o, prop );
			}
			db.commit( );
			return s;
		} catch ( Exception e ) {
			db.rollback( );
			return "Error: " + e.getMessage( );
		}
	}

	@Override
	public void resetValue( ) {}

	public static class DatabasePropertyBuilder implements ConfigurablePropertyBuilder {
		private static Logger LOG = Logger.getLogger( MultiDatabasePropertyEntry.DatabasePropertyBuilder.class );

		@Override
		public ConfigurableProperty buildProperty( Class c, Field f ) throws ConfigurablePropertyException {
			if ( c.isAnnotationPresent( Entity.class ) && 
					!((ConfigurableClass)c.getAnnotation(ConfigurableClass.class)).singleton()) {
				ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
				Field identifierField = null;
				for( Field  field : c.getDeclaredFields( ) ) {
					if(field.isAnnotationPresent(ConfigurableIdentifier.class)) {
						identifierField = field;
						break;
					}
				}
				if(identifierField == null) {
					return null;
				}
				if(f.isAnnotationPresent( ConfigurableField.class ) ) {
					LOG.debug( "Checking field: " + c.getName( ) + "." + f.getName( ) );
					ConfigurableField annote = f.getAnnotation( ConfigurableField.class );
					String fqPrefix = classAnnote.root( );
					String alias = classAnnote.alias();
					String description = annote.description( );
					String defaultValue = annote.initial( );
					PropertyTypeParser p = PropertyTypeParser.get( f.getType( ) );
					try {
						if ( !Modifier.isStatic( f.getModifiers( ) ) && !f.isAnnotationPresent( Transient.class ) ) {        	
							ConfigurableProperty prop = new MultiDatabasePropertyEntry( c, fqPrefix, f, identifierField, description, defaultValue, p, annote.readonly( ), annote.displayName(), annote.type(), alias, null );
							return prop;
						}
					} catch ( Throwable e ) {
						LOG.debug( e, e );
						return null;
					}
				}
			} else {
				return null;
			}
			return null;
		}

	}

	public void setIdentifierValue(String value) {
		identifierValue = value;
	}

	@Override
	public String getEntrySetName( ) {
		if(identifierValue != null)
			return identifierValue + "." + this.entrySetName;
		else
			return this.entrySetName;
	}

	@Override
	public String getQualifiedName( ) {
		if(identifierValue != null)
			return identifierValue + "." + this.qualifiedName;
		else
			return this.qualifiedName;
	}
	
	public MultiDatabasePropertyEntry getClone(String identifierValue) {
		return new MultiDatabasePropertyEntry(definingClass, entrySetName, field, identifierField, description, defaultValue, typeParser, readOnly, displayName, widgetType, alias, identifierValue);
	}
}
