/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.objectstorage.pipeline.handlers

import com.eucalyptus.objectstorage.exceptions.s3.MalformedPOSTRequestException
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception
import com.google.common.primitives.Bytes
import groovy.transform.CompileStatic
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.junit.Test
import static org.junit.Assert.fail

/**
 * Created by zhill on 4/2/14.
 */
@CompileStatic
class FormPOSTFilteringAggregatorTest {
  static byte[] crlf = [(char)'\r', (char)'\n']
  static byte[] boundaryBytes = '--boundary'.getBytes('UTF-8')
  static byte[] boundaryFinalBytes = '--boundary--\r\n'.getBytes('UTF-8')

  /**
   * @param aggregator
   * @param inputContent the buffer to filter, assumes the data starts at the set readerIndex in the buffer
   * @param data the original file content to validate against the inputContent should include this data
   * @return
   */
  static int checkFilteredOutput(FormPOSTFilteringAggregator aggregator, ChannelBuffer inputContent, int lastBufferedSize, ChannelBuffer aggregatedOutputBuffer) {
    int startReadIndex = inputContent.readerIndex()
    //println 'Start index: ' + startReadIndex + ' Last buffered: ' + lastBufferedSize
    int inputSize = inputContent.readableBytes()
    //println 'Input size: ' + inputSize
    int processingSize = inputSize + lastBufferedSize //total size processed by the scanner
    //println 'Processing size: ' + processingSize
    ChannelBuffer inputToScan = ChannelBuffers.buffer(inputSize)
    inputContent.readBytes(inputToScan, inputSize)
    ChannelBuffer result = aggregator.scanForFormBoundary(inputToScan)
    int chunkSize = result.readableBytes()
    int bufferedSize = processingSize - chunkSize
    assert(chunkSize <= processingSize) //To account for any random \r that occur too close to the end and must be buffered
    aggregatedOutputBuffer.writeBytes(result)

    assert(inputContent.copy(0, startReadIndex - lastBufferedSize + chunkSize).array() == aggregatedOutputBuffer.copy(0,aggregatedOutputBuffer.readableBytes()).array())
    return bufferedSize
  }


  @Test
  void testScanForFormBoundarySimple() {
    FormPOSTFilteringAggregator aggregator = new FormPOSTFilteringAggregator();
    int dataSize = 64
    aggregator.setupBoundary(boundaryBytes)
    byte[] initial = POSTRequestGenerator.getRandomData(dataSize)
    //Add some cr lf chars to test handling those mid-stream
    initial[(int)(dataSize / 2)] = 0x0D; //cr
    initial[(int)(dataSize / 2 + 1) ] = 0x0A; //lf
    ChannelBuffer buffer = ChannelBuffers.dynamicBuffer()
    buffer.writeBytes(Bytes.concat(initial, crlf, boundaryFinalBytes))
    ChannelBuffer outputBuffer = ChannelBuffers.dynamicBuffer()
    int buffered = checkFilteredOutput(aggregator, buffer, 0, outputBuffer)
    assert(buffered == boundaryFinalBytes.length + 2)

    aggregator.setupBoundary(boundaryBytes)
    buffer = ChannelBuffers.dynamicBuffer();
    buffer.writeBytes(Bytes.concat(initial, crlf, boundaryBytes, crlf, POSTRequestGenerator.getRandomData(dataSize), crlf, boundaryFinalBytes))
    outputBuffer.clear()

    try {
      buffered = checkFilteredOutput(aggregator, buffer, 0, outputBuffer)
      assert(buffered == 0)
      fail('Should have received an exception for trailing content after the boundary')
    } catch(MalformedPOSTRequestException e) {
      println "Correctly caught POST exception due to trailing content after boundary"
    }

  }

  //Writes the bytes from src to dest starting at destOffset in dest
  void writeBytesToBuffer(ChannelBuffer dest, ChannelBuffer src, int destOffset) {
    dest.markWriterIndex()
    dest.writerIndex(destOffset)
    dest.writeBytes(src)
    dest.resetWriterIndex()
  }

  @Test
  void testMultiChunkScan() {
    int iterations = 10
    for(int j = 0; j < iterations; j++) {
      FormPOSTFilteringAggregator aggregator = new FormPOSTFilteringAggregator();
      aggregator.setupBoundary(boundaryBytes)
      byte[] newlineBoundaryBytes = Bytes.concat(crlf, boundaryFinalBytes)
      ChannelBuffer data = ChannelBuffers.dynamicBuffer()
      ChannelBuffer outputData = ChannelBuffers.dynamicBuffer()
      int chunkSize = 1 * 1024
      int totalDataSize = 128 * chunkSize

      int bufferedSize = 0
      int maxBufferSize = boundaryBytes.length + 2 // for the \r\n
      int chunks = (int)(totalDataSize / chunkSize)
      for(int i = 0 ; i < chunks ; i++ ) {
        data.writeBytes(POSTRequestGenerator.getRandomData(chunkSize))

        //If last chunk, write the trailer in
        if(i == chunks - 1) {
          data.writeBytes(newlineBoundaryBytes)
          maxBufferSize = newlineBoundaryBytes.length + 2 //for the \r\n
        }
        bufferedSize = checkFilteredOutput(aggregator, data, bufferedSize, outputData)
        assert(bufferedSize <= maxBufferSize)
      }

      assert(data.copy(0, totalDataSize).array() == outputData.copy(0, outputData.readableBytes()).array())
      println 'Test iteration complete. Passed: ' + (j + 1) + ' of ' + iterations
    }
  }

  @Test
  void testMultiChunkScanWithTrailer() {
    int iterations = 10
    for(int j = 0; j < iterations; j++) {
      FormPOSTFilteringAggregator aggregator = new FormPOSTFilteringAggregator();
      aggregator.setupBoundary(boundaryBytes)
      byte[] trailerBytes = Bytes.concat(crlf, 'Content-Disposition: form-data; name="myfield"\r\n\r\nfieldvalue1'.getBytes('UTF-8'))
      ChannelBuffer data = ChannelBuffers.dynamicBuffer()
      ChannelBuffer outputData = ChannelBuffers.dynamicBuffer()
      int chunkSize = 1 * 1024
      int totalDataSize = 128 * chunkSize

      int bufferedSize = 0
      int maxBufferSize = boundaryBytes.length + 2 // for the \r\n
      int chunks = (int)(totalDataSize / chunkSize)
      for(int i = 0 ; i < chunks ; i++ ) {
        data.writeBytes(POSTRequestGenerator.getRandomData(chunkSize))

        //If last chunk, write the trailer in
        if(i == chunks - 1) {
          data.writeBytes(boundaryBytes)
          data.writeBytes(trailerBytes)
        }
        try {
          bufferedSize = checkFilteredOutput(aggregator, data, bufferedSize, outputData)
          assert(bufferedSize <= maxBufferSize)
        } catch(Exception e) {
          if(i == chunks -1) {
            printf 'Caught expected error on trailer'
          } else {
            fail('Unexpected exception caught: ' + e.getMessage())
          }
        }
      }
    }
  }
}
