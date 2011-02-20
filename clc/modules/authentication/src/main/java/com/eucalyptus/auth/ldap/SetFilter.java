package com.eucalyptus.auth.ldap;

import java.util.Collection;
import java.util.Set;
import com.google.common.collect.Sets;

public class SetFilter {

  private static final long serialVersionUID = 1L;

  private Set<String> set = Sets.newHashSet( );
  private boolean complement;
  
  public SetFilter( ) {
  }

  public SetFilter( boolean complement ) {
    this.complement = complement;
  }
  
  public void setComplement( boolean complement ) {
    this.complement = complement;
  }

  public boolean isComplement( ) {
    return complement;
  }
  
  public boolean contains( String element ) {
    boolean inSet = set.contains( element );
    return ( !complement && inSet ) || ( complement && !inSet );
  }
  
  public void add( String element ) {
    set.add( element );
  }
  
  public void addAll( Collection<String> elements ) {
    set.addAll( elements );
  }

  public String toString( ) {
    return ( complement ? "not" : "" ) + " " + set;
  }
  
}
