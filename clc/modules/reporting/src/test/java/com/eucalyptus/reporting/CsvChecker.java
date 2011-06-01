package com.eucalyptus.reporting;

import java.io.*;
import java.util.*;

/**
 * <p>CsvChecker verifies the contents of a CSV file against a reference CSV
 * file. CsvChecker is run from the command-line. See the method
 * <code>printHelp</code> or run from the command-line with no arguments
 * to get a full description of its usage.
 * 
 * @author tom.werges
 */
public class CsvChecker
{
	public static boolean debug = false;

	public static void main(String[] args)
		throws Exception
	{
		if ( args.length < 3 ) {
			printHelp();
			System.exit(-1);
		}
		
		final double errorMargin = Double.parseDouble(args[0]);
		final File referenceFile = new File(args[1]);
		final File checkedFile = new File(args[2]);
		debug = (args.length > 3 && args[3].equals("debug"));

		boolean passed = true;

		BufferedReader checkedReader = null;
		BufferedReader refReader = null;
		try {
			checkedReader = new BufferedReader(new FileReader(checkedFile));
			refReader = new BufferedReader(new FileReader(referenceFile));

			final List<ReferenceLine> refLines = new ArrayList<ReferenceLine>();
			int refLineNum = 0;
			for (String line = refReader.readLine(); line != null; line = refReader
					.readLine()) {
				if (line.length()==0) {
					continue;
				}
				ReferenceLine refLine = null;
				try {
					refLine = ReferenceLine.parseLine(line);
				} catch (ParseException pe) {
					System.err.println("Parse failed in reference file, line " + refLineNum);
					pe.printStackTrace(System.err);
					System.exit(-1);
				}
				if (refLine != null) refLines.add(refLine);
				refLineNum++;
			}

			String[] fields;
			int lineCnt = 0;
			refLineNum = 0;

			outerLoop:   //There's NOTHING wrong with labels...
			for (String line = checkedReader.readLine(); line != null; line = checkedReader
					.readLine()) {
				fields = line.split(",");
				refLineNum = 0;
				for (ReferenceLine refLine : refLines) {

					final boolean shouldMatch = refLine.shouldMatch(fields);
					final boolean doesMatch =
						( shouldMatch ? refLine.doesMatch(fields, errorMargin) : false);

					if (debug) System.err.printf("refLine:%d line:%d shouldMatch:%s doesMatch:%s\n",
								refLineNum, lineCnt, shouldMatch, doesMatch);

					passed = passed && ( shouldMatch ? doesMatch : true );
					
					if (!passed) {
						System.err.printf("Failed checkLine:%d refLine:%d\n",
								lineCnt, refLineNum);
						break outerLoop;
					}
					refLineNum++;
				}
				lineCnt++;
			}
		} finally {
			if (checkedReader != null) checkedReader.close();
			if (refReader != null) refReader.close();			
		}
		
		System.exit(passed ? 0 : 1);
	}

	private static class ReferenceLine
	{
		private static final String DOUBLE_PLUS_FIELD_PREFIX="++";
		
		private final int doublePlusFieldInd;
		private final String[] fields;

		ReferenceLine(int doublePlusFieldInd, String[] fields)
		{
			this.doublePlusFieldInd = doublePlusFieldInd;
			this.fields = fields;
		}

		static ReferenceLine parseLine(String line)
			throws ParseException
		{
			int doublePlusInd=-1;
			String[] fields = line.split(",");
			for (int i=0; i<fields.length; i++) {
				if (fields[i].startsWith(DOUBLE_PLUS_FIELD_PREFIX)) {
					doublePlusInd=i;
					fields[i]=fields[i].substring(2);
				}
			}
			if (doublePlusInd==-1) {
				throw new ParseException("Line lacks ++ field");
			}
			return new ReferenceLine(doublePlusInd, fields);
		}

		boolean shouldMatch(String[] otherFields)
		{
			if (otherFields.length < doublePlusFieldInd) {
				return false;
			} else {
				return otherFields[doublePlusFieldInd].matches(fields[doublePlusFieldInd]);
			}
		}

		boolean doesMatch(String[] otherFields, double errorMargin)
		{
			if (! (fields.length == otherFields.length)) {
				if (debug) System.err.printf("Field counts differ, %d %d\n",
					fields.length, otherFields.length);
				return false;
			}
			for (int i=0; i < fields.length; i++) {
				if (i==doublePlusFieldInd) continue;
				if (fields[i].trim().length()==0) continue; //ignore empty ref fields
				if (! fieldMatches(fields[i], otherFields[i], errorMargin)) {
					if (debug) System.err.printf("Field %d doesn't match, field:%s other:%s\n",
						i, fields[i], otherFields[i]);
					return false;
				}
			}
			return true;
		}

		private static boolean fieldMatches(String refField, String checkField,
				double errorMargin)
		{
			try {
				double refVal   = Double.parseDouble(refField);
				double checkVal = Double.parseDouble(checkField);
				return isWithinError(checkVal, refVal, errorMargin);
			} catch (NumberFormatException nfe) {
				return checkField.matches(refField);
			}
		}
		
		private static boolean isWithinError(double val, double correctVal,
				double errorPercent)
		{
			if (val == correctVal) {
				return true;
			} else {
				return correctVal * (1 - errorPercent) < val
						&& val < correctVal * (1 + errorPercent);
			}
		}
	}
	
	private static class ParseException
		extends Exception
	{
		ParseException(String msg) { super(msg); }
	}
	
	private static void printHelp()
	{
		System.out.println(
"\nCsvChecker verifies that the values in a CSV (comma-separated values) file\n"
+ "are correct, by comparing those values against a reference CSV file.\n"
+ "CsvChecker can compare both numeric values and Strings (using regex\n"
+ "expressions).  CsvChecker can tolerate an error percentage for numeric\n"
+ "values, specified as a parameter. \n"
+ "\n"
+ "Usage: CsvChecker errorMargin referenceFile checkFile [debug]\n"
+ "\n"
+ "Paramters should have the following format.  The errorMargin should be a\n"
+ "floating point value between 0 and 1. The reference file must be a CSV file.\n"
+ "\n"
+ "The checked file is checked against the reference file, according to the\n"
+ "following algorithm.  Each line in the checked file is verified against one or\n"
+ "more lines in the reference file. If the fields from the line of the checked\n"
+ "file, match all of the fields in the corresponding line of the reference file,\n"
+ "then CsvChecker proceeds to the next line in the checked file; otherwise\n"
+ "CsvChecker returns failure (-1) right away.\n"
+ "\n"
+ "Each line in the reference file specifies which lines in the checked file\n"
+ "it's going to be checked against. Each line in the reference file must have\n"
+ "one field starting with two plusses (++). This field indicates which lines\n"
+ "in the checked file are to be checked against this line in the reference\n"
+ "file. All lines in the checked file which have the corresponding field\n"
+ "matching the regex following the ++ will be checked against that line. For\n"
+ "example, if the reference file has a line with the third field reading\n"
+ "'++user-.*', then only lines in the checked file with the third field\n"
+ "matching that regex will be checked against that line.\n"
+ "\n"
+ "Each line in the reference file also contains other fields, which are\n"
+ "numeric or regex expressions. Those fields must match every corresponding\n"
+ "field in every applicable line of the checked file.  Numeric values can\n"
+ "differ by an error margin specified as a parameter to CsvChecker.\n"
+ "\n"
+ "Here is an example line from a reference file: 3,4,++user-.*,7,author:.*,,\n"
+ "This means: check every line in the checked file which has a third field\n"
+ "matching the regex 'user-.*'. Every such line must have its first field be\n"
+ "3, its second be 4, and so on, with its 5th field matching 'author:.*'. If\n"
+ "all the fields of that line match, then the entire line in the checked file\n"
+ "passes, and CsvChecker moves to the next line in the checked file. Any\n"
+ "blank fields in the reference line will match anything.\n"
+ "\n"
+ "In this way, CsvChecker can verify that any lines in a checked file which\n"
+ "match a pattern, have specified values. This is used to verify the\n"
+ "correctness of various kinds of CSV reports, for testing.\n"
+ "\n"
+ "Author: T Werges\n"
		);
	}
}
