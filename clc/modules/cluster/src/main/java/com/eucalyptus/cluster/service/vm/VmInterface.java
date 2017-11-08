/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
 ************************************************************************/package com.eucalyptus.cluster.service.vm;

import java.util.Objects;

/**
 *
 */
public final class VmInterface {
  private final String interfaceId;
  private final String attachmentId;
  private final Integer device;
  private final String mac;
  private final String privateAddress;
  private volatile String publicAddress;

  public VmInterface(
      final String interfaceId,
      final String attachmentId,
      final Integer device,
      final String mac,
      final String privateAddress,
      final String publicAddress
  ) {
    this.interfaceId = interfaceId;
    this.attachmentId = attachmentId;
    this.device = device;
    this.mac = mac;
    this.privateAddress = privateAddress;
    this.publicAddress = Objects.toString( publicAddress, "0.0.0.0" );
  }

  public void assignPublic( String publicAddress ) {
    this.publicAddress = Objects.toString( publicAddress, "0.0.0.0" );
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public Integer getDevice() {
    return device;
  }

  public String getInterfaceId() {
    return interfaceId;
  }

  public String getMac() {
    return mac;
  }

  public String getPrivateAddress() {
    return privateAddress;
  }

  public String getPublicAddress() {
    return publicAddress;
  }
}
