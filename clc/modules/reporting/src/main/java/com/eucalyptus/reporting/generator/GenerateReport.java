package com.eucalyptus.reporting.generator;

import java.io.*;

import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.generator.ReportGenerator.Format;

public class GenerateReport
{
	private static File baseDir = new File("/tmp/");
	private static File destFile = new File(baseDir, "report.html");
	private static ReportGenerator reportGenerator = new ReportGenerator(baseDir);
	private static Format format = Format.HTML;
	private static Period period = new Period(0, Long.MAX_VALUE);
	
	public static void generateInstanceReportOneCriterion(String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		System.out.println(" ----> GENERATING INSTANCE REPORT WITH " + crit);

		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			reportGenerator.generateInstanceReport(period, format, crit, dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}
	}

	public static void generateInstanceReportTwoCriteria(String groupBy,
			String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		GroupByCriterion groupCrit = getCriterion(groupBy);
		System.out.println(" ----> GENERATING INSTANCE REPORT WITH " + crit
				+ "," + groupCrit); 

		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			reportGenerator.generateNestedInstanceReport(period, format,
					groupCrit, crit, dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}
	}

	public static void generateStorageReportOneCriterion(String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		System.out.println(" ----> GENERATING STORAGE REPORT WITH " + crit);
 
		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			reportGenerator.generateStorageReport(period, format, crit, dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}
	}

	public static void generateStorageReportTwoCriteria(String groupBy,
			String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		GroupByCriterion groupCrit = getCriterion(groupBy);
		System.out.println(" ----> GENERATING STORAGE REPORT WITH " + crit
				+ "," + groupCrit); 

		OutputStream dest = null;
		try {
			dest = new FileOutputStream(destFile);
			reportGenerator.generateNestedStorageReport(period, format,
					groupCrit, crit, dest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (dest != null) {
				try {
					dest.close();
				} catch (IOException iox) {
					iox.printStackTrace();
				}
			}
		}

	}

	private static GroupByCriterion getCriterion(String name)
	{
		/* throws an IllegalArgument which we allow to percolate up
		 */
		return GroupByCriterion.valueOf(name.toUpperCase());
	}

}
