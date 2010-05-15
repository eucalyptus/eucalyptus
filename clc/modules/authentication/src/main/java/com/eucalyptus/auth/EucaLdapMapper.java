package com.eucalyptus.auth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.BaseAuthorization;
import com.google.common.collect.Lists;

/**
 * All the mundane work to handle the conversion between LDAP attributes
 * and User/Group entities based on the annotations.
 * @author wenye
 *
 */
public class EucaLdapMapper {
  private static Logger        LOG                           = Logger.getLogger( EucaLdapMapper.class );
  
  private static final Pattern USER_LDAP_CERT_STRING_PATTERN = Pattern.compile( "\\{(\\S*)\\}(\\S+)" );
  
  private static Class classType( Object o ) {
    return o instanceof Class ? ( Class ) o : o.getClass( );
  }
  
  /**
   * Get the LDAP attribute names for an annotated class field.
   * 
   * @param object
   * @param fieldName
   * @return
   */
  public static String[] getFieldAttributeNames( Object object, String fieldName ) {
    try {
      if ( object != null ) {
        Class clazz = classType( object );
        Field field = clazz.getDeclaredField( fieldName );
        Ldap annotation = field.getAnnotation( Ldap.class );
        {
          if ( annotation != null ) {
            return annotation.names( );
          }
        }
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    return null;
  }
  
  /**
   * Map LDAP attributes to Eucalyptus entity fields
   * 
   * @param attrs
   * @param dests
   * @return
   */
  public static List<Object> attributeToEntity( Attributes attrs, Object... dests ) {
    List<Object> entities = Lists.newArrayList( );
    for ( Object o : dests ) {
      if ( o != null ) {
        Class clazz = classType( o );
        Object object = null;
        try {
          object = clazz.newInstance( );
        } catch ( InstantiationException e ) {
          LOG.debug( e, e );
        } catch ( IllegalAccessException e ) {
          LOG.debug( e, e );
        }
        for ( Field field : clazz.getDeclaredFields( ) ) {
          Ldap annotation = field.getAnnotation( Ldap.class );
          if ( annotation != null ) {
            for ( String name : annotation.names( ) ) {
              Attribute attr = attrs.get( name );
              if ( attr != null ) {
                toField( annotation.converter( ), object, field, attr );
              }
            }
          }
        }
        entities.add( object );
      }
    }
    return entities;
  }
  
  /**
   * Map Eucalyptus entity fields to LDAP attributes
   * 
   * @param sources
   * @return
   */
  public static Attributes entityToAttribute( Object... sources ) {
    Attributes attrs = new BasicAttributes( );
    for ( Object object : sources ) {
      if ( object != null ) {
        for ( Field field : object.getClass( ).getDeclaredFields( ) ) {
          Ldap annotation = field.getAnnotation( Ldap.class );
          if ( annotation != null ) {
            Attribute[] result = toAttributes( annotation.converter( ), object, field, annotation.names( ) );
            if ( result != null ) {
              for ( Attribute attr : result ) {
                if ( attr != null ) {
                  attrs.put( attr );
                }
              }
            }
          }
        }
      }
    }
    return attrs;
  }
  
  private static Attribute[] toAttributes( EucaLdapMapping mapping, Object object, Field field, String[] attrNames ) {
    Object value = null;
    try {
      value = getField( object, field );
    } catch ( IllegalArgumentException e ) {
      LOG.error( e, e );
    }
    Attribute[] attrs = null;
    if ( value != null ) {
      switch ( mapping ) {
        case DEFAULT:
        case PASSWORD:
          if ( !"".equals( value ) ) {
            attrs = new Attribute[1];
            attrs[0] = new BasicAttribute( attrNames[0], value );
          }
          break;
        case INTEGER:
          attrs = new Attribute[1];
          attrs[0] = new BasicAttribute( attrNames[0], ( ( Integer ) value ).toString( ) );
          break;
        case BOOLEAN:
          attrs = new Attribute[1];
          attrs[0] = new BasicAttribute( attrNames[0], ( ( Boolean ) value ) ? "TRUE" : "FALSE" );
          break;
        case LONG:
          attrs = new Attribute[1];
          attrs[0] = new BasicAttribute( attrNames[0], ( ( Long ) value ).toString( ) );
          break;
        case CERTIFICATE:
          attrs = toCertificateAttribute( value );
          break;
        case PERMISSION:
          attrs = new Attribute[1];
          attrs[0] = toPermissionAttribute( attrNames[0], value );
          break;
        case MEMBERSHIP:
          attrs = new Attribute[1];
          attrs[0] = toEucaGroupIdAttribute( attrNames[0], value );
          break;
      }
    }
    return attrs;
  }
  
  private static void toField( EucaLdapMapping mapping, Object object, Field field, Attribute attr ) {
    Object value = null;
    try {
      switch ( mapping ) {
        case DEFAULT:
          value = attr.get( );
          setField( object, field, value );
          break;
        case INTEGER:
          value = Integer.parseInt( ( String ) attr.get( ) );
          setField( object, field, value );
          break;
        case BOOLEAN:
          value = ( ( String ) attr.get( ) ).equals( "TRUE" );
          setField( object, field, value );
          break;
        case LONG:
          value = Long.parseLong( ( String ) attr.get( ) );
          setField( object, field, value );
          break;
        case PASSWORD:
          value = new String( ( byte[] ) attr.get( ) );
          setField( object, field, value );
          break;
        case CERTIFICATE:
          setCertificateField( object, field, attr );
          break;
        case PERMISSION:
          setAuthListField( object, field, attr );
          break;
        case MEMBERSHIP:
          setEucaGroupIdsField( object, field, attr );
          break;
      }
    } catch ( NumberFormatException e ) {
      LOG.error( e, e );
    } catch ( NamingException e ) {
      LOG.error( e, e );
    } catch ( IllegalArgumentException e ) {
      LOG.error( e, e );
    }
  }
  
  /**
   * Get a field's getter/setter name
   * 
   * @param field
   * @param getter
   * @return
   */
  private static String getAccessorName( String field, boolean getter ) {
    StringBuilder sb = new StringBuilder( );
    if ( getter ) {
      sb.append( "get" );
    } else {
      sb.append( "set" );
    }
    sb.append( Character.toUpperCase( field.charAt( 0 ) ) );
    sb.append( field.substring( 1 ) );
    return sb.toString( );
  }
  
  /**
   * Get a field. If it is private, access by its getter.
   * 
   * @param object
   * @param field
   * @return
   */
  private static Object getField( Object object, Field field ) {
    try {
      if ( !Modifier.isPrivate( field.getModifiers( ) ) ) {
        return field.get( object );
      } else {
        Class clazz = object.getClass( );
        Method method = clazz.getDeclaredMethod( getAccessorName( field.getName( ), true ) );
        if ( !Modifier.isPrivate( method.getModifiers( ) ) ) {
          return method.invoke( object );
        }
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    return null;
  }
  
  /**
   * Set a field. If it is private, access by its setter.
   * 
   * @param object
   * @param field
   * @param value
   */
  private static void setField( Object object, Field field, Object value ) {
    try {
      if ( !Modifier.isPrivate( field.getModifiers( ) ) ) {
        field.set( object, value );
      } else {
        Class clazz = object.getClass( );
        Method method = clazz.getDeclaredMethod( getAccessorName( field.getName( ), false ), value.getClass( ) );
        if ( !Modifier.isPrivate( method.getModifiers( ) ) ) {
          method.invoke( object, value );
        }
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  
  private static void setCertificateField( Object object, Field field, Attribute attr ) {
    try {
      List<X509Cert> certs = ( List<X509Cert> ) getField( object, field );
      if ( certs == null ) {
        setField( object, field, ( Object ) ( new ArrayList<X509Cert>( ) ) );
      }
      toCertificateField( attr, certs );
    } catch ( IllegalArgumentException e ) {
      LOG.error( e, e );
    }
  }
  
  private static void setAuthListField( Object object, Field field, Attribute attr ) {
    try {
      List<BaseAuthorization> perms = ( List<BaseAuthorization> ) getField( object, field );
      if ( perms == null ) {
        setField( object, field, ( Object ) ( new ArrayList<BaseAuthorization>( ) ) );
      }
      toAuthListField( attr, perms );
    } catch ( IllegalArgumentException e ) {
      LOG.error( e, e );
    }
  }
  
  private static void setEucaGroupIdsField( Object object, Field field, Attribute attr ) {
    try {
      List<String> eucaGroupIds = ( List<String> ) getField( object, field );
      if ( eucaGroupIds == null ) {
        setField( object, field, ( Object ) ( new ArrayList<String>( ) ) );
      }
      toEucaGroupIdsField( attr, eucaGroupIds );
    } catch ( IllegalArgumentException e ) {
      LOG.error( e, e );
    }
  }
  
  /**
   * Get the certificate representation in LDAP (eucalyptus format): {alias}pem_certificate_in_b64
   * 
   * @param cert
   * @return
   */
  private static String toLdapEucaCertString( X509Cert cert ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( '{' );
    if ( cert.getAlias( ) != null ) {
      sb.append( cert.getAlias( ) );
    }
    sb.append( '}' );
    sb.append( cert.getPemCertificate( ) );
    return sb.toString( );
  }
  
  private static Attribute[] toCertificateAttribute( Object object ) {
    List<X509Cert> certs = ( List<X509Cert> ) object;
    Attribute[] attrs = new Attribute[2];
    attrs[0] = attrs[1] = null;
    for ( X509Cert cert : certs ) {
      System.out.println( "Processing " + cert.getAlias( ) );
      if ( cert != null ) {
        Boolean revoked = cert.isRevoked( );
        if ( revoked != null && revoked.booleanValue( ) ) {
          if ( attrs[1] == null ) {
            attrs[1] = new BasicAttribute( LdapConstants.EUCA_REVOKED_CERTIFICATE );
          }
          attrs[1].add( toLdapEucaCertString( cert ) );
          System.out.println( "Adding revoked " + cert.getAlias( ) );
        } else {
          if ( attrs[0] == null ) {
            attrs[0] = new BasicAttribute( LdapConstants.EUCA_CERTIFICATE );
          }
          attrs[0].add( toLdapEucaCertString( cert ) );
          System.out.println( "Adding " + cert.getAlias( ) );
        }
      }
    }
    return attrs;
  }
  
  /**
   * Get an X509Cert from LDAP attribute string, in format: "{alias}base64_pem_cert_data"
   * 
   * @param ldapCertString
   * @param revoked
   * @return
   */
  private static X509Cert toX509Cert( String ldapCertString, boolean revoked ) {
    Matcher matcher = USER_LDAP_CERT_STRING_PATTERN.matcher( ldapCertString );
    if ( !matcher.matches( ) ) {
      return null;
    }
    X509Cert cert = new X509Cert( );
    cert.setAlias( matcher.group( 1 ) );
    cert.setPemCertificate( matcher.group( 2 ) );
    cert.setRevoked( revoked );
    return cert;
  }
  
  private static void toCertificateField( Attribute attr, List<X509Cert> certs ) {
    boolean revoked = LdapConstants.EUCA_REVOKED_CERTIFICATE.equals( attr.getID( ) );
    try {
      NamingEnumeration values = attr.getAll( );
      while ( values.hasMore( ) ) {
        String value = ( String ) values.next( );
        if ( value != null ) {
          X509Cert cert = toX509Cert( value, revoked );
          if ( cert != null ) {
            certs.add( cert );
          } else {
            LOG.debug( "Parsing LDAP eucaCertificate/eucaRevokedCertificate attr failed: " + value );
          }
        }
      }
    } catch ( NamingException e ) {
      LOG.error( e, e );
    }
  }
  
  /**
   * @param value
   *          The authorization format: "className:value"
   * @return
   */
  private static BaseAuthorization toAuthorization( String value ) {
    String[] components = value.split( ":" );
    if ( components.length < 2 ) {
      return null;
    }
    BaseAuthorization auth = null;
    try {
      Class clazz = Class.forName( components[0] );
      Object object = clazz.newInstance( );
      if ( !( object instanceof BaseAuthorization ) ) {
        LOG.debug( "Can not instantiate permission " + components[0] );
        return null;
      }
      auth = ( BaseAuthorization ) object;
      auth.setValue( components[1] );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
    return auth;
  }
  
  private static void toAuthListField( Attribute attr, List<BaseAuthorization> authList ) {
    try {
      NamingEnumeration values = attr.getAll( );
      while ( values.hasMore( ) ) {
        String value = ( String ) values.next( );
        if ( value != null ) {
          BaseAuthorization auth = toAuthorization( value );
          if ( auth != null ) {
            authList.add( auth );
          } else {
            LOG.debug( "Parsing LDAP permission attr failed: " + value );
          }
        }
      }
    } catch ( NamingException e ) {
      LOG.error( e, e );
    }
  }
  
  private static void toEucaGroupIdsField( Attribute attr, List<String> eucaGroupIds ) {
    try {
      NamingEnumeration values = attr.getAll( );
      while ( values.hasMore( ) ) {
        String value = ( String ) values.next( );
        if ( value != null ) {
          eucaGroupIds.add( value );
        }
      }
    } catch ( NamingException e ) {
      LOG.error( e, e );
    }
  }
  
  /**
   * Format "className:value"
   * 
   * @param perm
   * @return
   */
  private static String toLdapEucaPermString( BaseAuthorization perm ) {
    return perm.getClass( ).getName( ) + ":" + perm.getValue( );
  }
  
  private static Attribute toPermissionAttribute( String name, Object object ) {
    List<BaseAuthorization> perms = ( List<BaseAuthorization> ) object;
    Attribute attr = null;
    for ( BaseAuthorization perm : perms ) {
      if ( perm != null ) {
        if ( attr == null ) {
          attr = new BasicAttribute( name );
        }
        attr.add( toLdapEucaPermString( perm ) );
      }
    }
    return attr;
  }
  
  private static Attribute toEucaGroupIdAttribute( String name, Object object ) {
    List<String> eucaGroupIds = ( List<String> ) object;
    Attribute attr = null;
    for ( String id : eucaGroupIds ) {
      if ( id != null ) {
        if ( attr == null ) {
          attr = new BasicAttribute( name );
        }
        attr.add( id );
      }
    }
    return attr;
  }
}
