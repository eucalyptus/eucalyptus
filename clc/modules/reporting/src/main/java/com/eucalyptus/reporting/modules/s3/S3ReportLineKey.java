package com.eucalyptus.reporting.modules.s3;


public class S3ReportLineKey
	implements Comparable<S3ReportLineKey>
{
	private final String label;
	private final String groupByLabel;

	/**
	 * @param label Cannot be null; every ReportLine has a label
	 * @param groupByLabel Can be null
	 */
	public S3ReportLineKey(String label, String groupByLabel)
	{
		super();
		if (label==null)
			throw new IllegalArgumentException("label can't be null");
		this.label = label;
		this.groupByLabel = groupByLabel;
	}

	public String getLabel()
	{
		return label;
	}

	public String getGroupByLabel()
	{
		return groupByLabel;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((groupByLabel == null) ? 0 : groupByLabel.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		S3ReportLineKey other = (S3ReportLineKey) obj;
		if (groupByLabel == null) {
			if (other.groupByLabel != null)
				return false;
		} else if (!groupByLabel.equals(other.groupByLabel))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	
	@Override
	public int compareTo(S3ReportLineKey other)
	{
		if (groupByLabel==null) {
			return label.compareTo(other.label);
		} else {
			return (groupByLabel.compareTo(other.groupByLabel)==0)
				? label.compareTo(other.label)
				: groupByLabel.compareTo(other.groupByLabel); 			
		}
	}

}
