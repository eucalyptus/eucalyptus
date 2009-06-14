package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.util.WalrusProperties;

import java.io.*;

import org.apache.log4j.Logger;

public class Command {
    private static Logger LOG = Logger.getLogger(Command.class);

    public static String run(String[] command) {
        try
        {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command);
            StreamConsumer error = new StreamConsumer(proc.getErrorStream());
            StreamConsumer output = new StreamConsumer(proc.getInputStream());
            error.start();
            output.start();
            proc.waitFor();
            output.join();
            return output.getReturnValue();
        } catch (Throwable t) {
            LOG.error(t);
        }
        return "";
    }
}