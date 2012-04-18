package com.eucalyptus.auth.ldap;

import java.util.Set;
import com.google.common.collect.Sets;

public class Selection {

  private String searchFilter;
  private Set<String> selected = Sets.newHashSet( );
  private Set<String> notSelected = Sets.newHashSet( );
  
  public Selection( ) {
  }
  
  public void setSearchFilter( String searchFilter ) {
    this.searchFilter = searchFilter;
  }
  public String getSearchFilter( ) {
    return searchFilter;
  }
  public void setSelected( Set<String> selected ) {
    this.selected = selected;
  }
  public Set<String> getSelected( ) {
    return selected;
  }
  public void setNotSelected( Set<String> notSelected ) {
    this.notSelected = notSelected;
  }
  public Set<String> getNotSelected( ) {
    return notSelected;
  }
  
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "filter='" ).append( this.searchFilter ).append( "';" );
    sb.append( "select='" ).append( this.selected ).append( "';" );
    sb.append( "not-select='" ).append( this.notSelected ).append( "'" );
    return sb.toString( );
  }
  
}
