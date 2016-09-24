/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.simpleworkflow.common.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
public class WorkflowClientStandalone {
  private static Logger    LOG     = Logger.getLogger(  WorkflowClientStandalone.class );
  private static WorkflowClientStandalone _instance = new WorkflowClientStandalone();
  public static WorkflowClientStandalone getInstance() {
    return _instance;
  }

  private List<Class> activityClasses = Lists.newArrayList();
  private List<Class> workflowClasses = Lists.newArrayList();
  private List<WorkflowClient> clients = Lists.newArrayList();
  private String jarFile = null; 
  private Set<String> allowedClassNames = Sets.newHashSet();
  private String credentialPropertyFile = null;
  private String swfEndpoint = null;
  private String taskList = null;
  private String domain = null;
  private int clientConnectionTimeout = 30000;
  private int clientMaxConnections = 100;
  private int domainRetentionPeriodInDays = 1;
  private int pollThreadCount = 1;
  
  private String logLevel = "DEBUG";
  private String logDir = "/var/log/eucalyptus";
  private String logAppender = "console-log";

  @SuppressWarnings("static-access")
  private static Options buildOptions() {
    final Options opts = new Options();
    opts.addOption(
        OptionBuilder
        .withLongOpt("endpoint")
        .hasArgs(1)
        .withDescription("SWF Service Endpoint")
        .isRequired()
        .create('e'));
        
    opts.addOption(OptionBuilder
        .withLongOpt("domain")
        .hasArgs(1)
        .withDescription("SWF Domain")
        .isRequired()
        .create('d'));
    
    opts.addOption(OptionBuilder
        .withLongOpt("tasklist")
        .hasArgs(1)
        .withDescription("SWF task list")
        .isRequired()
        .create('l'));
    
    opts.addOption(OptionBuilder
        .withLongOpt("timeout")
        .hasArgs(1)
        .withDescription("SWF client connection timeout")
        .isRequired(false)
        .create('o'));
    
    opts.addOption(OptionBuilder
        .withLongOpt("maxconn")
        .hasArgs(1)
        .withDescription("SWF client max connections")
        .isRequired(false)
        .create('m'));
    
    opts.addOption(OptionBuilder
        .withLongOpt("retention")
        .hasArgs(1)
        .withDescription("SWF domain retention period in days")
        .isRequired(false)
        .create('r'));
    
    opts.addOption(OptionBuilder
        .withLongOpt("threads")
        .hasArgs(1)
        .withDescription("Polling threads count")
        .isRequired(false)
        .create('t'));
  
    opts.addOption(OptionBuilder
        .withLongOpt("jar")
        .hasArgs(1)
        .withDescription("JAR file that implement workflows" 
            + " and activities")
        .isRequired(true)
        .create());
    
    opts.addOption(OptionBuilder
        .withLongOpt("classes")
        .hasArgs(1)
        .withDescription("Limit workflow and activities classes to load (class names are separated by ':')")
        .isRequired(false)
        .create());
    
    opts.addOption(OptionBuilder
        .withLongOpt("credential")
        .hasArgs(1)
        .withDescription("Property file containing AWS credentials to use (default is to use session credentials from instance's metadata")
        .isRequired(false)
        .create());
    
    opts.addOption(OptionBuilder
        .withLongOpt("loglevel")
        .hasArgs(1)
        .withDescription("Logging level (default: DEBUG)")
        .isRequired(false)
        .create());
    
    opts.addOption(OptionBuilder
        .withLongOpt("logdir")
        .hasArgs(1)
        .withDescription("Directory containing log files (default: /var/log/eucalyptus)")
        .isRequired(false)
        .create());
    
    opts.addOption(OptionBuilder
        .withLongOpt("logappender")
        .hasArgs(1)
        .withDescription("Log4j appender to use")
        .isRequired(false)
        .create());
    
    return opts;
  }
  
  private void readOptions(final CommandLine cli) throws NumberFormatException{
    this.swfEndpoint = cli.getOptionValue("endpoint");
    this.domain = cli.getOptionValue("domain");
    this.taskList = cli.getOptionValue("tasklist");
    
    if (cli.hasOption("timeout"))
      this.clientConnectionTimeout = Integer.parseInt(cli.getOptionValue("timeout"));
    if (cli.hasOption("maxconn"))
      this.clientMaxConnections = Integer.parseInt(cli.getOptionValue("maxconn"));
    if (cli.hasOption("retention"))
      this.domainRetentionPeriodInDays = Integer.parseInt(cli.getOptionValue("retention"));
    if (cli.hasOption("threads"))
      this.pollThreadCount = Integer.parseInt(cli.getOptionValue("threads"));
    if (cli.hasOption("jar"))
      this.jarFile = cli.getOptionValue("jar");
    if (cli.hasOption("credential"))
      this.credentialPropertyFile = cli.getOptionValue("credential");
    if (cli.hasOption("loglevel"))
      this.logLevel = cli.getOptionValue("loglevel");
    if (cli.hasOption("logdir"))
      this.logDir = cli.getOptionValue("logdir");
    if (cli.hasOption("logappender"))
      this.logAppender = cli.getOptionValue("logappender");
    if (cli.hasOption("classes")) {
      final String classNames = cli.getOptionValue("classes");
      if(classNames.contains(":")) {
        for (final String cls : classNames.split(":")) {
          this.allowedClassNames.add(cls);
        }
      }else {
        this.allowedClassNames.add(classNames);
      }
    }
  }
  
  private static void printHelp(final Options opts, final String error) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setDescPadding(0);
    String header = "\n"
        + "Welcome to the Standalone SWF Host!\n"
        + "The program discovers and hosts SWF workflows and activities";
    final String footer = error == null? "\n" : String.format("\n%s", error);
    formatter.printHelp("java -cp jarfiles com.eucalyptus.simpleworkflow.common.client.WorkflowClientStandalone", header, opts, footer, true);
  }

  private static void initLogs() {
    final WorkflowClientStandalone instance = WorkflowClientStandalone.getInstance();
    System.setProperty("euca.log.level", instance.logLevel);
    System.setProperty("euca.log.dir", instance.logDir);  
    System.setProperty("euca.log.appender", instance.logAppender);
    Logs.init();
  }
  
  private void discoverWorkflows() throws Exception {
    final File f = new File(this.jarFile);
    if (f.exists() && !f.isDirectory())
      processJar(f);
    else
      throw new Exception(String.format("No such file is found: %s", this.jarFile));
  }

  private void processJar( File f ) throws Exception {
    final JarFile jar = new JarFile( f );
    final Properties props = new Properties( );
    final List<JarEntry> jarList = Collections.list( jar.entries( ) );
    LOG.trace( "-> Trying to load component info from " + f.getAbsolutePath( ) );
    for ( final JarEntry j : jarList ) {
      try {
        if ( j.getName( ).matches( ".*\\.class.{0,1}" ) ) {
          handleClassFile( f, j );
        }
      } catch ( RuntimeException ex ) {
        LOG.error( ex, ex );
        jar.close( );
        throw ex;
      }
    }
    jar.close( );
  }

  private void handleClassFile( final File f, final JarEntry j ) throws IOException, RuntimeException {
    final String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( "\\.class.{0,1}", "" );
    try {
      final Class candidate = ClassLoader.getSystemClassLoader( ).loadClass( classGuess );
      final Ats ats = Ats.inClassHierarchy(candidate);
      if ((this.allowedClassNames.isEmpty() ||
              this.allowedClassNames.contains(candidate.getName()) ||
              this.allowedClassNames.contains(candidate.getCanonicalName()) ||
                  this.allowedClassNames.contains(candidate.getSimpleName()))
                  && ( ats.has( Workflow.class ) || ats.has( Activities.class ) )
                  && !Modifier.isAbstract( candidate.getModifiers() ) &&
                  !Modifier.isInterface( candidate.getModifiers( ) ) &&
                  !candidate.isLocalClass( ) &&
                  !candidate.isAnonymousClass( ) ) {
        if ( ats.has( Workflow.class ) ) {
          this.workflowClasses.add(candidate);
          LOG.debug( "Discovered workflow implementation class: " + candidate.getName( ) );
        } else {
          this.activityClasses.add(candidate);
          LOG.debug( "Discovered activity implementation class: " + candidate.getName( ) );
        }
      }
    } catch ( final ClassNotFoundException e ) {
      LOG.debug( e, e );
    }
  }
  
  private AWSCredentialsProvider getCredentialsProvider() {
    AWSCredentialsProvider provider = null;     
    if (this.credentialPropertyFile != null) {
        provider = new AWSCredentialsProvider() {
          private String accessKey = null;
          private String secretAccessKey = null;
          
          private void readProperty() throws FileNotFoundException, IOException{
            final FileInputStream stream = 
                new FileInputStream(new File(credentialPropertyFile));
            try {
                Properties credentialProperties = new Properties();
                credentialProperties.load(stream);

                if (credentialProperties.getProperty("accessKey") == null ||
                    credentialProperties.getProperty("secretKey") == null) {
                    throw new IllegalArgumentException(
                        "The specified file (" + credentialPropertyFile
                        + ") doesn't contain the expected properties 'accessKey' "
                        + "and 'secretKey'."
                    );
                }
                accessKey = credentialProperties.getProperty("accessKey");
                secretAccessKey = credentialProperties.getProperty("secretKey");
            } finally {
                try {
                    stream.close();
                } catch (final IOException e) {
                }
            }
          }
          @Override
          public AWSCredentials getCredentials() {
            if (this.accessKey == null || this.secretAccessKey == null) {
              try{
                readProperty();
              }catch(final Exception ex) {
                throw new RuntimeException("Failed to read credentials file", ex);
              }
            }
            return new BasicAWSCredentials(accessKey, secretAccessKey);
          }

          @Override
          public void refresh() {
            this.accessKey = null;
          }
        };
    } else {
      provider = new InstanceProfileCredentialsProvider();
    }

    return provider;
  }
  
  private String buildClientConfig() {
    return String.format("{\"ConnectionTimeout\": %d, \"MaxConnections\": %d}",
        this.clientConnectionTimeout, this.clientMaxConnections);
  }
  
  private String buildWorkflowWorkerConfig() {
    return String.format("{ \"DomainRetentionPeriodInDays\": %d, \"PollThreadCount\": %d }",
        this.domainRetentionPeriodInDays, this.pollThreadCount);
  }
  
  private String buildActivityWorkerConfig() {
    return String.format("{ \"DomainRetentionPeriodInDays\": %d, \"PollThreadCount\": %d }",
        this.domainRetentionPeriodInDays, this.pollThreadCount);
  }
  
  private AmazonSimpleWorkflow getAWSClient() {
    final AWSCredentialsProvider provider = this.getCredentialsProvider();
    final String clientConfig = this.buildClientConfig();
    final AmazonSimpleWorkflow client = Config.buildClient(provider, this.swfEndpoint, clientConfig);
    return client;
  }
  
  private static void addShutdownHook(final AmazonSimpleWorkflow swfClient) {
    Runtime.getRuntime().addShutdownHook(new Thread( new Runnable() { 
      public void run() {
        LOG.debug("Shutting down existing SWF clients");
        final WorkflowClientStandalone instance = WorkflowClientStandalone.getInstance();
        for (final WorkflowClient client : instance.clients) {
          try{
            client.stop();
          }catch(final InterruptedException ex) {
            ;
          }
        }
        swfClient.shutdown();
      }
    }));
  }
  
  public static void main(String[] args) {
    final WorkflowClientStandalone instance = WorkflowClientStandalone.getInstance();
    final Options opts = buildOptions();
    final GnuParser cliParser = new GnuParser();
    CommandLine cmd =  null; 
    try{
      cmd = cliParser.parse(opts, args);
    }catch(final ParseException ex) {
      printHelp(opts, null);
      System.exit(1);
    }
    
    try{
      instance.readOptions(cmd);
    }catch(final NumberFormatException ex) {
      printHelp(opts, "Some number format arguents are not recognizable");
      System.exit(1);
    }
    
    initLogs();
    
    LOG.debug("Starting Workflow Standalone Host");
    try{
      instance.discoverWorkflows();
    }catch(final Exception ex) {
      LOG.debug("Failed to discover workflow and activities implementation");
      printHelp(opts, "Failed to discover implementation classes");
      System.exit(1);
    }

    try {
      final AmazonSimpleWorkflow swfClient = instance.getAWSClient();
      addShutdownHook(swfClient);

      final WorkflowClient workflowClient = new WorkflowClient(
              instance.workflowClasses.toArray(new Class<?>[instance.workflowClasses.size()]),
              instance.activityClasses.toArray(new Class<?>[instance.activityClasses.size()]),
              false,
              swfClient,
              instance.domain,
              instance.taskList,
              instance.buildWorkflowWorkerConfig(),
              instance.buildActivityWorkerConfig());
      workflowClient.start();
      instance.clients.add(workflowClient);
    }catch(final Exception ex) {
      LOG.debug("Failed to create workflow clients", ex);
      System.exit(1);
    }

    do {
      try{
        Thread.sleep(1000);
      }catch(final Exception ex) {
      }
    }while(true);
  }
}
