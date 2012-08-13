package com.eucalyptus.reporting.art;

import java.util.*;

import com.eucalyptus.reporting.art.util.IndentingStringBuffer;

public class ArtArray
	implements PrettyPrintable
{
	private List<Object> values;

	public ArtArray()
	{
		this.values = new ArrayList<Object>();
	}

	public void add(ArtObject value)
	{
		this.values.add(value);
	}

	public void add(ArtArray value)
	{
		this.values.add(value);
	}

	public void add(Long value)
	{
		this.values.add(value);
	}

	public void add(String value)
	{
		this.values.add(value);
	}

	public void add(Double value)
	{
		this.values.add(value);
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (Object val: values) {
			sb.append(val.toString());
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public String prettyPrint(int numIndents)
	{
		IndentingStringBuffer sb = new IndentingStringBuffer(0);
		sb.append("[");
		boolean first = true;
		for (Object val: values) {
			if (!first) {
				sb.append(",\n");
			}
			if (val instanceof PrettyPrintable) {
				sb.append(((PrettyPrintable)val).prettyPrint(numIndents+1));
			} else if (val instanceof String) {
				sb.appendIndent(numIndents+1, "\"");
				sb.append((String)val);
				sb.append("\"");
			} else {
				sb.appendIndent(numIndents+1, val.toString());
			}
			first = false;
		}
		sb.append("]");
		return sb.toString();
	}

}
