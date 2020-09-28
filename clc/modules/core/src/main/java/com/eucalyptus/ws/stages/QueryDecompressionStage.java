/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.ws.stages;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import com.eucalyptus.ws.Handlers;

/**
 *
 */
public class QueryDecompressionStage implements UnrollableStage {

  @Override
  public void unrollStage( final ChannelPipeline pipeline ) {
    pipeline.addLast( "decompressor", new HttpContentDecompressor( ) );
    pipeline.addLast( "parameters-post", Handlers.queryParameterHandler( ) );
  }

  @Override
  public String getName( ) {
    return "query-decompression";
  }
}
