/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.compute.common.internal.vpc;

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_dhcp_options" )
public class DhcpOption extends AbstractPersistent {
  private static final long serialVersionUID = 1L;

  @ManyToOne( optional = false )
  @JoinColumn( name = "metadata_dhcp_option_set_id" )
  private DhcpOptionSet dhcpOptionSet;

  @Column( name = "metadata_key", nullable = false )
  private String key;

  @ElementCollection
  @CollectionTable( name = "metadata_dhcp_option_values" )
  @Column( name = "metadata_value" )
  @OrderColumn( name = "metadata_value_index")
  private List<String> values;

  protected DhcpOption() {
  }

  protected DhcpOption( final DhcpOptionSet dhcpOptionSet, final String key, final List<String> values ) {
    this.dhcpOptionSet = dhcpOptionSet;
    this.key = key;
    this.values = values;
  }

  public static DhcpOption create( final DhcpOptionSet dhcpOptionSet, final String key, final String value ) {
    return create( dhcpOptionSet, key, Lists.newArrayList( value ) );
  }

  public static DhcpOption create( final DhcpOptionSet dhcpOptionSet, final String key, final List<String> values ) {
    return new DhcpOption( dhcpOptionSet, key, values );
  }

  public DhcpOptionSet getDhcpOptionSet() {
    return dhcpOptionSet;
  }

  public void setDhcpOptionSet( final DhcpOptionSet dhcpOptionSet ) {
    this.dhcpOptionSet = dhcpOptionSet;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( final String key ) {
    this.key = key;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues( final List<String> values ) {
    this.values = values;
  }
}
