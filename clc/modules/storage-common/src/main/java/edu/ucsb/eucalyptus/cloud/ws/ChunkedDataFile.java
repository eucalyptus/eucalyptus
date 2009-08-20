package edu.ucsb.eucalyptus.cloud.ws;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.jboss.netty.handler.stream.ChunkedFile;

import edu.ucsb.eucalyptus.constants.IsData;

public class ChunkedDataFile extends ChunkedFile implements IsData {

	public ChunkedDataFile(RandomAccessFile file, long offset, long length,
			int chunkSize) throws IOException {
		super(file, offset, length, chunkSize);
	}		
}

