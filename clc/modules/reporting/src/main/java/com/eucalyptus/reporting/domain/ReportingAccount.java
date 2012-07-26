package com.eucalyptus.reporting.domain;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Entity;

/**
 * <p>ReportingAccount is a eucalyptus account stored in the reporting database. The reporting
 * subsystem keeps its own historical record of users and accounts, seperate from the CLC
 * database. This is because the reporting subsystem stores historical information. It will retain
 * user and account information after those users and accounts have been deleted, so you can
 * still generate reports of accounts and users that no longer exist.
 *
 * <p>NOTE: Accounts must be created using the <code>ReportingAccountCrud</code> class. Do not 
 * instantiate one of these or store it in the database directly.
 *
 * @author tom.werges
 */
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_reporting")
@Table(name="reporting_account")
public class ReportingAccount
{
	@Id @Column(name="account_id", nullable=false)
	private String id;
	@Column(name="account_name", nullable=false)
	private String name;

	/**
 	 * <p>Do not instantiate this class directly; use the ReportingAccountCrud class.
 	 */
	protected ReportingAccount()
	{
		/* Hibenrate will extend this class and overwrite these values. */
		this.id = null;
		this.name = null;
	}
	
	ReportingAccount(String id, String name)
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
