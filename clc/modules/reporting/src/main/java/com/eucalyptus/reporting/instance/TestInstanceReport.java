package com.eucalyptus.reporting.instance;

import java.io.*;

public class TestInstanceReport
{
	private final static double ERROR = 0.2d;

	public static void main(String[] args) throws Exception
	{
		if (args.length != 6) {
			throw new IllegalArgumentException(
					"args: reportPath expectedNum expectedDays expectedNetIo expectedDiskIo expectedNumUsers");
		}
		final File reportFile = new File(args[0]);
		final int expectedNum = Integer.parseInt(args[1]);
		final int expectedDays = Integer.parseInt(args[2]);
		final int expectedNetIo = Integer.parseInt(args[3]);
		final int expectedDiskIo = Integer.parseInt(args[4]);
		final int expectedNumUsers = Integer.parseInt(args[5]);

		boolean allTestsSucceeded = true;
		int numUsers = 0;
		BufferedReader reader = new BufferedReader(new FileReader(reportFile));
		for (String line = reader.readLine(); line != null; line = reader
				.readLine()) {
			numUsers++;
			String[] items = line.split(",");
			if (items[1].startsWith("user-")) {
				System.out.println(line);

				final String user = items[1];
				final int numM1Small = Integer.parseInt(items[2]);
				final int numM1SmallDays = Integer.parseInt(items[3]);
				final int numC1Medium = Integer.parseInt(items[4]);
				final int numC1MediumDays = Integer.parseInt(items[5]);
				final int numM1Large = Integer.parseInt(items[6]);
				final int numM1LargeDays = Integer.parseInt(items[7]);
				final int numM1XLarge = Integer.parseInt(items[8]);
				final int numM1XLargeDays = Integer.parseInt(items[9]);
				final int numC1XLarge = Integer.parseInt(items[11]);
				final int numC1XLargeDays = Integer.parseInt(items[12]);
				final int netIo = Integer.parseInt(items[13]);
				final int diskIo = Integer.parseInt(items[14]);

				final int totalNum = numM1Small + numC1Medium + numM1Large
						+ numM1XLarge + numC1XLarge;
				final int totalDays = numM1SmallDays + numC1MediumDays
						+ numM1LargeDays + numM1XLargeDays + numC1XLargeDays;

				System.out.printf("m1Small:(#:%d,days:%d) c1Medium(#:%d,days:%d) "
								+ "m1Large(#:%d,days:%d) m1XLarge:(#:%d,days:%d) c1XLarge:"
								+ "(#:%d,days:%d) netIo:%d diskIo:%d\n",
								numM1Small, numM1SmallDays, numC1Medium,
								numC1MediumDays, numM1Large, numM1LargeDays,
								numM1XLarge, numM1XLargeDays, numC1XLarge,
								numC1XLargeDays, netIo, diskIo);

				boolean testSucceeded = 
					isWithinError(netIo, expectedNetIo, ERROR)
					&& isWithinError(diskIo, expectedDiskIo, ERROR)
					&& isWithinError(totalNum, expectedNum, ERROR)
					&& isWithinError(totalDays, expectedDays, ERROR);

				allTestsSucceeded = allTestsSucceeded && testSucceeded;
			}
			allTestsSucceeded = allTestsSucceeded && (numUsers == expectedNumUsers);
		}
		reader.close();
		System.exit(allTestsSucceeded ? 0 : 1);
	}

	private static boolean isWithinError(long val, long correctVal, double errorPercent)
	{
		return isWithinError((double)val, (double)correctVal, errorPercent);
	}

	private static boolean isWithinError(double val, double correctVal, double errorPercent)
	{
		return correctVal * (1-errorPercent) < val
				&& val < correctVal * (1+errorPercent);
	}


}
