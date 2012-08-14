package com.eucalyptus.reporting.art;

import java.util.*;

import com.eucalyptus.reporting.art.util.IndentingStringBuffer;

public class ArtObject
	implements PrettyPrintable
{	
	private final Map<String,Object> attrs;
	
	public ArtObject()
	{
		this.attrs = new HashMap<String, Object>();
	}
	
	public Set<String> getKeys()
	{
		return attrs.keySet();
	}
	
	public boolean hasAttribute(String key)
	{
		return attrs.containsKey(key);
	}

	public Object getAttributeValue(String key)
	{
		return attrs.get(key);
	}
	
	public void putAttribute(String key, String value)
	{
		this.attrs.put(key, value);
	}
	
	public void putAttribute(String key, Long value)
	{
		this.attrs.put(key, value);
	}

	public void putAttribute(String key, Double value)
	{
		this.attrs.put(key, value);
	}

	public void putAttribute(String key, ArtObject value)
	{
		this.attrs.put(key, value);
	}

	public void putAttribute(String key, ArtArray value)
	{
		this.attrs.put(key, value);
	}

	/**
	 * @return A string representation of the entire object graph in JSON-like
	 *   format.
	 */
	@Override
	public String toString()
	{
		return prettyPrint(0);
	}
	
	/**
	 * @return A string representation of the entire object graph in JSON-like
	 *   format.
	 */
	@Override
	public String prettyPrint(int numIndents)
	{
		IndentingStringBuffer sb = new IndentingStringBuffer();
		sb.appendIndent(numIndents, "{\n");
		boolean first = true;
		for (String key: getKeys()) {
			if (!first) {
				sb.append(",\n");
			}
			sb.appendIndent(numIndents+1,"\"");
			sb.append(key);
			sb.append("\" : ");
			Object val = getAttributeValue(key);
			if (val instanceof PrettyPrintable) {
				sb.append(((PrettyPrintable)val).prettyPrint(numIndents+1)); //Recurse
			} else if (val instanceof String) {
				sb.append("\"");
				sb.append(val.toString());
				sb.append("\"");
			} else {
				sb.append(val.toString());
			}
			first = false;
		}
		sb.appendIndent(numIndents,"}");
		sb.append("\n");
		return sb.toString();		
	}

}
