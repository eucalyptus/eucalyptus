package com.eucalyptus.reporting.user;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_user")
public class ReportingUser
{
	@Id @Column(name="user_id", nullable=false)
	private String id;
	@Column(name="user_name", nullable=false)
	private String name;

	public ReportingUser()
	{
		//empty
	}
	
	public ReportingUser(String id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		if (id == null) throw new IllegalArgumentException("id cant be null");
		this.id = id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		if (name == null) throw new IllegalArgumentException("id cant be null");
		this.name = name;
	}
	
}
