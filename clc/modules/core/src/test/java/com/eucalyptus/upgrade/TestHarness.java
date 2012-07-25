/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.upgrade;

import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.Parameterized.Parameters;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentDiscovery;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class TestHarness
{
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
	private static Logger LOG = Logger.getLogger(TestHarness.class);
	private static List<Class<? extends Annotation>> testAnnotations = Lists
			.newArrayList(Before.class, After.class, Test.class,
					Parameters.class);

	public static void main(String[] args)
	{
		boolean anyFail = false;
		try {
			System.out.println(Arrays.asList(args));
			final Options opts = getCliOptions();
			final GnuParser cliParser = new GnuParser();
			final CommandLine cmd = cliParser.parse(opts, args);

			try {
				anyFail = Iterables.any(Lists.newArrayList(Opts.values()),
						new Predicate<Opts>()
						{
							public boolean apply(Opts o)
							{
								return !o.run(cmd, opts);
							}
						});
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);  //This is necessary now; some shutdown process is hanging
		//return (anyFail) ? 1 : 0;
	}

	enum Opts
	{
		help {
			@Override
			public boolean run(CommandLine cmd, Options opts)
			{
				if (cmd.hasOption(Opts.help.toString())
						|| !cmd.getArgList().isEmpty()
						|| cmd.getOptionValues("test") == null) {
					printHelp(opts);
					System.exit(0);
				}
				return true;
			}
		},
		runTest {
			@Override
			public boolean run(CommandLine cmd, Options opts)
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
//					Logs.DEBUG = doDebug;
//					Logs.TRACE = doDebug;

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

				runMethods(cmd, opts);
				return true;
			}
		};

		public abstract boolean run(CommandLine cmd, Options opts);
	}

	@SuppressWarnings("static-access")
	private static Options getCliOptions()
	{
		final Options opts = new Options();
		opts.addOption(OptionBuilder
				.withLongOpt("test")
				.hasArgs()
				.withDescription(
						"Run the specified test; this option can appear multiple times. Parameters can be set as key=value pairs. e.g. ConcurrencyTest:threads=16")
				.withArgName("TEST_CLASS[:PARAM=VALUE]*").create("t"));
		opts.addOption(OptionBuilder.withLongOpt(Opts.help.name())
				.withDescription("Show this help information.").create("h"));
		return opts;
	}

	@SuppressWarnings({"static-access"})
	private static void printHelp(Options opts)
	{
		try {
			PrintWriter out = new PrintWriter(System.out);
			HelpFormatter help = new HelpFormatter();
			help.setArgName("TESTS");
			help.printHelp(out, LINE_BYTES, "java -jar test.jar",
					"Options controlling the test harness.", opts, 2, 4, "",
					true);
			Multimap<Class, Method> testMethods = getTestMethods();
			help = new HelpFormatter();
			help.setLongOptPrefix("");
			help.setOptPrefix("");
			help.setSyntaxPrefix("");
			help.setLeftPadding(0);
			Options testOptions = new Options();
			for (Class c : testMethods.keySet()) {
				testOptions.addOption(OptionBuilder
						.withDescription(getDescription(c))
						.withLongOpt(c.getSimpleName()).create());
				for (Method m : testMethods.get(c)) {
					testOptions.addOption(OptionBuilder
							.withDescription(getDescription(m))
							.withLongOpt(c.getSimpleName() + "." + m.getName())
							.create());
				}
			}
			help.printHelp(out, LINE_BYTES, " ", "Tests:", testOptions, 0, 2,
					"", false);
			out.flush();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	@SuppressWarnings("unchecked")
	private static String getDescription(Object o)
	{
		Class c = null;
		Method m = null;
		if (o instanceof Class
				&& ((c = (Class) o).getAnnotation(TestDescription.class)) != null) {
			return ((TestDescription) c.getAnnotation(TestDescription.class))
					.value();
		} else if (o instanceof Method
				&& ((m = (Method) o).getAnnotation(TestDescription.class)) != null) {
			StringBuffer sb = new StringBuffer();
			for (Class a : Lists.newArrayList(Before.class, After.class,
					Test.class, Ignore.class, Parameters.class)) {
				if (m.getAnnotation(a) != null)
					sb.append("  @")
							.append(String.format("%-9.9s", a.getSimpleName()))
							.append(" ");
			}
			return sb
					.append(" ")
					.append(((TestDescription) m
							.getAnnotation(TestDescription.class)).value())
					.toString();
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	private static Multimap<Class, Method> getTestMethods() throws Exception
	{
		final Multimap<Class, Method> testMethods =  ArrayListMultimap.create( );
		List<Class> classList = Lists.newArrayList();
		for (File f : new File(System.getProperty("euca.home")
				+ "/usr/share/eucalyptus").listFiles()) {
			if (f.getName().startsWith("eucalyptus")
					&& f.getName().endsWith(".jar")
					&& !f.getName().matches(".*-ext-.*")) {
				try {
					JarFile jar = new JarFile(f);
					Enumeration<JarEntry> jarList = jar.entries();
					for( JarEntry j : Collections.list( jar.entries() ) ) {
						if (j.getName().matches(".*\\.class.{0,1}")) {
							String classGuess = j.getName()
									.replaceAll("/", ".")
									.replaceAll("\\.class.{0,1}", "");
							try {
								Class candidate = ClassLoader
										.getSystemClassLoader().loadClass(
												classGuess);
								for (final Method m : candidate
										.getDeclaredMethods()) {
									if (Iterables
											.any(testAnnotations,
													new Predicate<Class<? extends Annotation>>()
													{
														public boolean apply(
																Class<? extends Annotation> arg0)
														{
															return m.getAnnotation(arg0) != null;
														}
													})) {
										System.out.println("Added test class: "
												+ candidate.getCanonicalName());
										testMethods.put(candidate, m);
									}
								}
							} catch (ClassNotFoundException e) {
							}
						}
					}
					jar.close();
				} catch (Exception e) {
					System.out.println(e.getMessage());
					continue;
				}
			}
		}
		return testMethods;
	}

	public static boolean runTests(List<Class> tests)
	{
		System.out.println("TEST LIST SIZE:" + tests.size());
		for (Class testClass : tests) {
			System.out.println("Test:" + testClass);
		}
		JUnitCore core = new JUnitCore();
		// core.run( Cleanup.class );
		core.addListener(new TestListener());
		boolean rv = true;
		for (Class clazz : tests) {
			Result res = core.run(clazz);
			if (res == null || !res.wasSuccessful()) {
				rv = false;
			}
		}
		return rv;
		// tests = Lists.newArrayList( tests );
		// tests.add( Cleanup.class );
		// Result res = core.run( ( Class<?>[] ) tests.toArray( new Class[] {})
		// );
		// return res != null ? res.wasSuccessful( ) : false;
	}

	@SuppressWarnings("unchecked")
	private static void runMethods(final CommandLine cmd, final Options opts)
	{
		String[] optVals = cmd.getOptionValues("test");
		for (int i = 0; i < optVals.length; i++) {
			String[] argParts = optVals[i].split(":");
			String className = argParts[0];
			String methodName = argParts[1];
			String[] methodArgsArray = new String[0];
			if (argParts.length > 2) {
				methodArgsArray = argParts[2].split(",");
				System.out.printf("Executing class:%s method:%s args:%s\n", className,
						methodName, argParts[2]);
			} else {
				System.out.printf("Executing class:%s method:%s\n", className,
						methodName);
			}
			Class[] params = new Class[methodArgsArray.length];
			for (int j=0; j < params.length; j++) {
				params[j] = String.class;
			}
			
			try {
				Class clazz = Class.forName(className);
				clazz.getDeclaredMethod(methodName, params).invoke(null, (Object[])methodArgsArray);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			System.out.println("Executed method");
		}
		// List<Class> testList = Lists.transform( Lists.newArrayList(
		// cmd.getOptionValues( "test" ) ), new Function<String, Class>( ) {
		// public Class apply( String arg ) {
		// String[] argParts = arg.split(":");
		// String className = argParts[0];
		// System.out.println("CLASS NAME:" + className);
		// Class targetClass = null;
		// try {
		// targetClass = Class.forName( className );
		// } catch ( Exception e ) {
		// try {
		// targetClass = Class.forName( className + "Test" );
		// } catch ( Exception e1 ) {
		// }
		// }
		// if( targetClass == null ) {
		// printHelp( opts );
		// System.exit( 1 );
		// } else {
		// for( int i = 1; i < argParts.length; i++ ) {
		// String property = argParts[i].replaceAll("=.*","");
		// String value = argParts[i].replaceAll(".*=","");
		// try {
		// targetClass.getDeclaredMethod( property, String.class ).invoke( null,
		// value );
		// } catch ( Exception e ) {
		// System.out.println( e );
		// System.exit( 1 );
		// }
		// }
		// }
		// return targetClass;
		// }
		// } );
		// return testList;
	}
}
