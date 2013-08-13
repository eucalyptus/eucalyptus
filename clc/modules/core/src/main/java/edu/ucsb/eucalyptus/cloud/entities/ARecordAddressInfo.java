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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * @author Sang-Min Park
 * 
 * This class is introduced in 3.3 when we implement round-robin dns support for loadbalancers.
 * The Dns name to Ip address mapping is now 1-N.
 *
 */
@Entity
@PersistenceContext(name="eucalyptus_dns")
@Table( name = "ARecordAddresses" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ARecordAddressInfo extends AbstractPersistent {
	@Transient
	private static final long serialVersionUID = 1L;
	
    @ManyToOne
    @JoinColumn( name = "metadata_name_fk", nullable=false )
    @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
    private ARecordNameInfo nameinfo = null;
    
    @Column( name = "address" )
    private String address;
    
    @Column( name = "unique_name", unique=true, nullable=false)
    private String uniqueName;
    
    private ARecordAddressInfo(){}
    
    private ARecordAddressInfo(ARecordNameInfo name, String address){
    	this.nameinfo = name;
    	this.address=address;
    	uniqueName = this.createUniqueName();
    }
    
    public static ARecordAddressInfo newInstance(ARecordNameInfo name, String address){
    	ARecordAddressInfo newRec = new ARecordAddressInfo(name, address);
    	return newRec;
    }
    
    public static ARecordAddressInfo named(ARecordNameInfo name, String address){
    	return newInstance(name, address);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    @PrePersist
    private void generateOnCommit( ) {
    	if(this.uniqueName==null)
    		this.uniqueName = createUniqueName( );
    }

    protected String createUniqueName( ) {
    	return String.format("arecord-%s-%s", this.nameinfo.getName(), this.address);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((this.nameinfo == null) ? 0 : this.nameinfo.hashCode());
		return result;
	}
    
    @Override
    public boolean equals(Object obj){
    	if (this == obj)
			return true;
    	if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		ARecordAddressInfo other = (ARecordAddressInfo) obj;
		if (this.address == null) {
			if (other.address != null)
				return false;
		} else if (!this.address.equals(other.address))
			return false;
		if (this.nameinfo == null) {
			if (other.nameinfo != null)
				return false;
		} else if (!this.nameinfo.equals(other.nameinfo))
			return false;
		return true;
    }
    
    @Override
    public String toString(){
    	return String.format("A record address (%s-%s)", this.nameinfo != null? this.nameinfo.getName() : "unassigned", this.address);
    }
}
