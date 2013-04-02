/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package edu.ucsb.eucalyptus.cloud.entities;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

/**
 * @author Sang-Min Park
 * 
 * This class is introduced in 3.3 when we implement round-robin dns support for loadbalancers.
 * The Dns name to Ip address mapping is now 1-N.
 *
 */

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_dns")
@Table( name = "ARecordNames" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ARecordNameInfo extends AbstractPersistent {
    
	@Transient
	private static final long serialVersionUID = 1L;
	
	@Column( name = "name" )
    private String name;
	
    @Column( name = "zone")
    private String zone;
    
    @Column( name = "recordclass" )
    private Integer recordclass;
    
    @Column( name = "ttl" )
    private Long ttl;
    

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "nameinfo")
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
	private Collection<ARecordAddressInfo> addresses = null;
	
    public ARecordNameInfo() {}
    
    public static ARecordNameInfo newInstance(String name, String zone, Integer recordClass, Long ttl){
    	ARecordNameInfo newInstance = new ARecordNameInfo();
    	newInstance.setName(name);
    	newInstance.setZone(zone);
    	newInstance.setRecordclass(recordClass);
    	newInstance.setTtl(ttl);
    	return newInstance;
    }
    
    public static ARecordNameInfo named(String name, String zone){
    	ARecordNameInfo newInstance = new ARecordNameInfo();
    	newInstance.setName(name);
    	newInstance.setZone(zone);
    	return newInstance;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public Integer getRecordclass() {
        return recordclass;
    }

    public void setRecordclass(Integer recordclass) {
        this.recordclass = recordclass;
    }
    
    public Collection<ARecordAddressInfo> getAddresses(){
    	return this.addresses;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((zone == null) ? 0 : zone.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ARecordNameInfo other = (ARecordNameInfo) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (zone == null) {
			if (other.zone != null)
				return false;
		} else if (!zone.equals(other.zone))
			return false;
		return true;
	}   
	
	@Override
	public String toString(){
		return String.format("A record %s", this.name);
	}
}
