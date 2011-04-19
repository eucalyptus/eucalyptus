package com.eucalyptus.upgrade;

import java.lang.reflect.InvocationTargetException;

import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentDiscovery;
import com.eucalyptus.system.LogLevels;

/**
 * <p>MethodRunner runs a single static method, specified by arguments to the
 * <code>main</code> method. The static method specified must take only String
 * arguments, and may optionally return an int. If the method specified returns
 * an int, then that int will be returned to the calling shell as a return code.
 * If the specified method does not return an int, it must return void, in which
 * case, this will return failure (non-zero) to the calling shell in the event of
 * any exception, and success (0) otherwise.
 * 
 * <p>MethodRunner is invoked by the <code>clc/tools/runMethod.sh</code> bash
 * script. It's used as a part of Kyo's testing framework which requires things
 * to be executed from the shell and to provide return codes.
 * 
 * <p>MethodRunner does not require Eucalyptus to be running. MethodRunner
 * initializes everything, starts the db, sets up persistence contexts, etc. It
 * allows you to execute a single method within the context of Eucalyptus and to
 * get a return code.
 * 
 * @author tom.werges
 */
public class MethodRunner
{
	public static int RETURN_CODE_STARTUP_FAILED = 127;
	public static int RETURN_CODE_RUN_EXCEPTION  = 126;
	public static int RETURN_CODE_NO_SUCH_CLASS  = 125;
	public static int RETURN_CODE_NO_SUCH_METHOD = 124;
	public static int RETURN_CODE_ILLEGAL_ACCESS = 123;
	
	public static Integer LINE_BYTES = 0;
	static {
		String defaultLevel = System.getProperty("verbose");
		String columns = System.getenv("COLUMNS");
		System.setProperty("test.output.console", "INFO");
		System.setProperty("test.output.terminal", "INFO");
		if (columns != null && !"".equals(columns)) {
			LINE_BYTES = Integer.parseInt(System.getenv("COLUMNS"));
			System.setProperty("test.output.terminal", defaultLevel != null
					? defaultLevel
					: "INFO");
		} else {
			LINE_BYTES = 85;
			System.setProperty("test.output.console", "INFO");
		}
	}

	public static int count = 1;

	/**
	 * <p>Takes a series of command-line arguments, runs a static method
	 * specified in those arguments, and returns a return code to the shell
	 * which invoked it.
	 * 
	 * <p>Expects that the method specified is a static method which takes
	 * only String arguments and returns an int return code or void.
	 * 
	 * @param args
	 *            Should consist of 3 Strings: className, methodName,
	 *            and commaDelimitedStringArgs which are passed to the method
	 */
	public static void main(String[] args)
	{
		if (args.length < 2) throw new IllegalArgumentException("At least 2 params required");
		
		final String className = args[0];
		final String methodName = args[1];
		final String argsParam = (args.length>2) ? args[2] : null;
		
		int rv = 0;
		
		try {	
			startupEucalyptus();
		} catch (Throwable e) {
			e.printStackTrace();
			rv=RETURN_CODE_STARTUP_FAILED;
		}
		
		try {
			
			rv = runMethod(className, methodName, argsParam);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			rv = RETURN_CODE_NO_SUCH_CLASS;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			rv = RETURN_CODE_NO_SUCH_METHOD;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			rv = RETURN_CODE_ILLEGAL_ACCESS;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			rv = RETURN_CODE_RUN_EXCEPTION;
		} 

		System.exit(rv);
	}

	private static void startupEucalyptus()
	{
		try {
			System.setProperty("euca.log.level", "TRACE");
			System.setProperty("euca.log.appender", "console");
			System.setProperty("euca.log.exhaustive.cc", "FATAL");
			System.setProperty("euca.log.exhaustive.db", "FATAL");
			System.setProperty("euca.log.exhaustive.external", "FATAL");
			System.setProperty("euca.log.exhaustive.user", "FATAL");
			System.setProperty("euca.var.dir",
					System.getProperty("euca.home")
							+ "/var/lib/eucalyptus/");
			System.setProperty("euca.conf.dir",
					System.getProperty("euca.home")
							+ "/etc/eucalyptus/cloud.d/");
			System.setProperty("euca.log.dir",
					System.getProperty("euca.home")
							+ "/var/log/eucalyptus/");
			System.setProperty("euca.lib.dir",
					System.getProperty("euca.home")
							+ "/usr/share/eucalyptus/");
			boolean doTrace = "TRACE".equals(System
					.getProperty("euca.log.level"));
			boolean doDebug = "DEBUG".equals(System
					.getProperty("euca.log.level")) || doTrace;
			LogLevels.DEBUG = doDebug;
			LogLevels.TRACE = doDebug;

			if ((StandalonePersistence.eucaDest = System
					.getProperty("euca.upgrade.destination")) == null) {
				throw new RuntimeException(
						"Failed to find required 'euca.upgrade.destination' property");
			}
			ServiceJarDiscovery.processLibraries( );
			ServiceJarDiscovery.runDiscovery( new ComponentDiscovery( ) );
			StandalonePersistence.setupInitProviders();
			StandalonePersistence.runSetupDiscovery();
			StandalonePersistence.setupProviders();
			StandalonePersistence.setupNewDatabase();
		} catch (Exception e) {
			throw new RuntimeException(
					"Standalone persistence setup failed", e);
		}		
	}
	
	@SuppressWarnings("rawtypes")
	private static int runMethod(String className, String methodName, String args)
		throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
				InvocationTargetException
	{
		String[] methodArgsArray = new String[0];
		if (args != null) {
			methodArgsArray = args.split(",");
			System.out.printf("Executing class:%s method:%s args:%s\n",
					className, methodName, args);
		} else {
			System.out.printf("Executing class:%s method:%s\n", className,
					methodName);
		}
		
		Class[] params = new Class[methodArgsArray.length];
		for (int j = 0; j < params.length; j++) {
			params[j] = String.class;
		}


		Object retVal = Class.forName(className)
				.getDeclaredMethod(methodName, params)
				.invoke(null, (Object[]) methodArgsArray);

		if (retVal == null) {
			return 0;
		} else if (retVal instanceof Integer) {
			return ((Integer)retVal).intValue();
		} else {
			throw new NoSuchMethodException("No method with return type int or void");			
		}

	}

}
