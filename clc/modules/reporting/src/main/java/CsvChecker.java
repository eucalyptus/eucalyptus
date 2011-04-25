
/**
 * <p>CsvChecker
 * 
 * @author tom.werges
 */
public class CsvChecker
{
	public static void main(String[] args)
	{
		if ( args.length !=3 ) {
			printHelp();
			System.exit(-1);
		}
	}
	
	private static void printHelp()
	{
		System.out.println(
"CsvChecker verifies that the values in a CSV (comma-separated values) file\n"
+ "are correct, by comparing those values against a reference CSV file.\n"
+ "CsvChecker can compare both numeric values and Strings (using regex\n"
+ "expressions).  CsvChecker can tolerate an error percentage for numeric\n"
+ "values, specified as a parameter.\n"
+ "\n"
+ "Usage: CsvChecker errorMargin checkedFile referenceFile\n"
+ "\n"
+ "The errorMargin should be a floating point value between 0 and 1. The\n"
+ "reference file is also a CSV file, and the checked file is checked against\n"
+ "it, according to an algorithm specified below.\n"
+ "\n"
+ "Each line in the checked file is verified against one or more lines in the\n"
+ "reference file. If the fields from the line of the checked file, match all\n"
+ "of the fields in the corresponding line of the reference file, then\n"
+ "CsvChecker proceeds to the next line in the checked file; otherwise\n"
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
+ "Author: T Werges\n"
		);
	}
}
