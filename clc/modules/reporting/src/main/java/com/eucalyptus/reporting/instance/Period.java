package com.eucalyptus.reporting.instance;

import javax.persistence.*;

/**
 * <p>Period represents a time period. Period is immutable because there can
 * be multiple references to one Period.
 * 
 * @author tom.werges
 */
@Embeddable
public class Period
{
	//NOTE: hibernate can modify final fields using reflection
	@Column(name="beginning_ms", nullable=false)
	private final Long beginningMs;
	@Column(name="ending_ms", nullable=false)
	private final Long endingMs;

	/**
	 * For use by hibernate; don't extend this class
	 */
	protected Period()
	{
		//NOTE: hibernate will override these, despite finality
		this.beginningMs = null;
		this.endingMs = null;
	}

	public Period(long beginningMs, long endingMs)
	{
		this.beginningMs = new Long(beginningMs);
		this.endingMs = new Long(endingMs);
	}

	public long getBeginningMs()
	{
		assert this.beginningMs != null; //hibernate notNullable
		return this.beginningMs.longValue();
	}

	public long getEndingMs()
	{
		assert this.endingMs != null; //hibernate notNullable
		return this.endingMs.longValue();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((beginningMs == null) ? 0 : beginningMs.hashCode());
		result = prime * result
				+ ((endingMs == null) ? 0 : endingMs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Period other = (Period) obj;
		if (beginningMs == null) {
			if (other.beginningMs != null)
				return false;
		} else if (!beginningMs.equals(other.beginningMs))
			return false;
		if (endingMs == null) {
			if (other.endingMs != null)
				return false;
		} else if (!endingMs.equals(other.endingMs))
			return false;
		return true;
	}


}
