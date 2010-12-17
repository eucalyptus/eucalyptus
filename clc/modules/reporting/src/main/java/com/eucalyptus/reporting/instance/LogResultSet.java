package com.eucalyptus.reporting.instance;

import java.util.*;

/**
 * <p>LogResultSet contains the results of a query of InstanceUsageLog.
 * LogResultSet is conceptually similar to a jdbc ResultSet; it has a
 * "cursor" which you can move forward by calling <pre>next()</pre>, and a set
 * of objects for each cursor position.
 *
 * <p>As with a JDBC ResultSet, you must call <pre>next()</pre> once before
 * any results become available.
 * 
 * <p>LogResultSet should be used as follows:
 * <pre>
 * while (logResultSet.next()) {
 *    System.out.println(logResultSet.getInstanceAttributes().getInstanceId());
 *    //...whatever other code you want...
 * }
 * </pre>
 *
 * <p>At some point in the future, LogResultSet may be lazily loaded.
 * 
 * @author tom.werges
 */
public class LogResultSet
{
	private final List<Item> list;
	private int pos;
	
	LogResultSet()
	{
		list = new ArrayList<Item>();
		pos = -1;
	}

	void addItem(InstanceAttributes insAttrs, PeriodUsageData pud)
	{
		list.add(new Item(insAttrs, pud));
	}

	public boolean next()
	{
		return (++pos >= list.size());
	}

	public InstanceAttributes getInstanceAttributes()
	{
		return list.get(pos).getInstanceAttributes();
	}

	public PeriodUsageData getPeriodUsageData()
	{
		return list.get(pos).getPeriodUsageData();
	}

	private class Item
	{
		private final InstanceAttributes insAttrs;
		private final PeriodUsageData pud;
		
		Item(InstanceAttributes insAttrs, PeriodUsageData pud)
		{
			this.insAttrs = insAttrs;
			this.pud = pud;
		}
		
		InstanceAttributes getInstanceAttributes()
		{
			return this.insAttrs;
		}
		
		PeriodUsageData getPeriodUsageData()
		{
			return this.pud;
		}
	}
	
}
