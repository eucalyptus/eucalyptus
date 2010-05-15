package com.eucalyptus.auth.ldap;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

/**
 * Wrapped JDNI Attributes for convenience.
 */
public class LdapAttributes {
  
  Attributes attrs = new BasicAttributes( );
  
  public LdapAttributes( ) {}
  
  /**
   * Add an attribute.
   * @param name
   * @param values
   * @return
   */
  public LdapAttributes addAttribute( String name, Object... values ) {
    if ( values.length == 0 ) {
      attrs.put( name, null );
    } else if ( values.length == 1 ) {
      attrs.put( name, values[0] );
    } else {
      Attribute attr = new BasicAttribute( name );
      for ( Object value : values ) {
        attr.add( value );
      }
      attrs.put( attr );
    }
    return this;
  }
  
  /**
   * Get the wrapped JNDI Attributes
   * @return
   */
  public Attributes getAttributes() {
    return attrs;
  }
}
