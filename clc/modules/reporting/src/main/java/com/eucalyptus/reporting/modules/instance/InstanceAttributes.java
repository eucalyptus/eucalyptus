package com.eucalyptus.reporting.modules.instance;

import javax.persistence.*;

import org.hibernate.annotations.Entity;

import com.eucalyptus.entities.AbstractPersistent;

@SuppressWarnings("serial")
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_instance")
public class InstanceAttributes
	extends AbstractPersistent
{
	@Column(name="uuid")
	private String uuid;
	@Column(name="instance_id", nullable=false)
	private String instanceId;
	@Column(name="instance_type")
	private String instanceType;
	@Column(name="user_id")
	private String userId;
	@Column(name="account_id")
	private String accountId;
	@Column(name="cluster_name")
	private String clusterName;
	@Column(name="availability_zone")
	private String availabilityZone;

	/**
	 * This ctor is for internal use but must be made protected for Hibernate;
	 * do not extend.
	 */
	protected InstanceAttributes()
	{
		//NOTE: hibernate will overwrite these
		this.uuid = null;
		this.instanceId = null;
		this.instanceType = null;
		this.userId = null;
		this.clusterName = null;
		this.availabilityZone = null;
	}

	InstanceAttributes(String uuid, String instanceId, String instanceType,
				String accountId, String userId, String clusterName,
				String availabilityZone)
	{
		this.uuid = uuid;
		this.instanceId = instanceId;
		this.instanceType = instanceType;
		this.userId = userId;
		this.accountId = accountId;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
	}

	public String getUuid()
	{
		return this.uuid;
	}
	
	void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public String getInstanceId()
	{
		return this.instanceId;
	}

	public String getInstanceType()
	{
		return this.instanceType;
	}

	public String getUserId()
	{
		return this.userId;
	}

	
	public String getAccountId()
	{
		return accountId;
	}

	public String getClusterName()
	{
		return this.clusterName;
	}

	public String getAvailabilityZone()
	{
		return this.availabilityZone;
	}

	@Override
	public int hashCode()
	{
		return (uuid == null) ? 0 : uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		InstanceAttributes other = (InstanceAttributes) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "[uuid:" + this.uuid+ " instanceId:" + this.instanceId + " userId:" + this.userId + "]";
	}


}
