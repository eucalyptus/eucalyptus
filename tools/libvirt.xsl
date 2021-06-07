<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 2008 Regents of the University of California
  Copyright 2009-2012 Ent. Services Development Corporation LP

  Redistribution and use of this software in source and binary forms,
  with or without modification, are permitted provided that the
  following conditions are met:

    Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer
    in the documentation and/or other materials provided with the
    distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
  THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
  COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
  AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
  SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
  WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
  REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
  IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
  NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
-->

<!--
This XSL-T file is used to construct two types of XML documents:

- one that libvirt can use to launch a Eucalyptus instance (the
job formerly performed by gen_*libvirt_xml Perl scripts). As input
it assumes an XML document produced the the Node Controller
that describes a Eucalyptus instance to be launched.

- one that libvirt can use to attach a disk to an instance
-->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output encoding="UTF-8" indent="yes" method="xml"/>

    <xsl:template match="/instance">
        <!-- sanity check on the hypervisor type - we only know 'kvm' and 'xen' -->
        <xsl:if test="/instance/hypervisor/@type != 'kvm' and /instance/hypervisor/@type != 'qemu'">
           <xsl:message terminate="yes">ERROR: invalid or unset /instance/hypervisor/@type parameter</xsl:message>
        </xsl:if>
        <domain>
            <xsl:attribute name="type">
                <xsl:value-of select="/instance/hypervisor/@type"/>
            </xsl:attribute>
            <name>
                <xsl:value-of select="/instance/name"/>
            </name>
            <description>Eucalyptus instance <xsl:value-of select="/instance/name"/></description>
            <os>
                <xsl:choose>
                    <xsl:when test="/instance/os/@platform = 'linux' and /instance/backing/root/@type = 'image'">
                        <!-- for Linux image-store-based instances -->
                        <xsl:if test="/instance/hypervisor/@type = 'xen'">
                            <type>linux</type>
                        </xsl:if>
                        <xsl:if test="/instance/hypervisor/@type = 'kvm' or /instance/hypervisor/@type = 'qemu'">
                            <type>hvm</type>
                        </xsl:if>
                        <xsl:if test="/instance/ramdisk!=''">
                            <initrd>
                                <xsl:value-of select="/instance/ramdisk"/>
                            </initrd>
                        </xsl:if>
                        <xsl:if test="/instance/kernel!=''">
                            <kernel>
                                <xsl:value-of select="/instance/kernel"/>
                            </kernel> 
                        </xsl:if>
                        <xsl:if test="/instance/kernel!='' and /instance/ramdisk!=''">
                            <xsl:choose>
                                <xsl:when test="(/instance/hypervisor/@type = 'kvm' or /instance/hypervisor/@type = 'qemu') and /instance/os/@virtioRoot = 'true'">
                                  <cmdline>
                                    <xsl:choose>
                                      <xsl:when test="/instance/rootDirective!=''">root=<xsl:value-of select="/instance/rootDirective"/> console=ttyS0</xsl:when>
                                      <xsl:otherwise>root=/dev/vda1 console=ttyS0</xsl:otherwise>
                                    </xsl:choose>
                                  </cmdline>
                                </xsl:when>
                                <xsl:otherwise>
                                  <cmdline>
                                    <xsl:choose>
                                      <xsl:when test="/instance/rootDirective!=''">root=<xsl:value-of select="/instance/rootDirective"/> console=ttyS0</xsl:when>
                                      <xsl:otherwise>root=/dev/sda1 console=ttyS0</xsl:otherwise>
                                    </xsl:choose>
                                  </cmdline>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                    </xsl:when>
                    <xsl:when test="/instance/os/@platform = 'windows' or /instance/backing/root/@type = 'ebs'">
                        <!-- for all Windows and EBS-backed-root Linux instances -->
                        <type>hvm</type>
                        <xsl:if test="/instance/hypervisor/@type = 'xen'">
                            <loader>/usr/lib/xen/boot/hvmloader</loader>
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:message terminate="yes">ERROR: invalid or unset /instance/os/@platform or /instance/backing/root/@type parameter</xsl:message>
                    </xsl:otherwise>
                </xsl:choose>
                <boot dev='hd'/>
                <smbios mode='sysinfo'/>
            </os>
            <sysinfo type='smbios'>
                <system>
                    <entry name='serial'>ec2<xsl:value-of select="substring(/instance/uuid,4)"/></entry>
                    <entry name='uuid'>ec2<xsl:value-of select="substring(/instance/uuid,4)"/></entry>
                </system>
            </sysinfo>
            <features>
                <acpi/>
                <xsl:if test="/instance/hypervisor/@type = 'xen' and ( /instance/os/@platform = 'windows' or /instance/backing/root/@type = 'ebs' )">
                    <apic/>
                    <pae/>
                </xsl:if>
            </features>
            <clock offset="localtime"/>
            <on_poweroff>destroy</on_poweroff>
            <on_reboot>restart</on_reboot>
            <on_crash>destroy</on_crash>
            <vcpu>
                <xsl:value-of select="/instance/cores"/>
            </vcpu>
            <cpu>
	      <xsl:if test="/instance/cpuPassthrough = 'true'">
		<xsl:attribute name="mode">host-passthrough</xsl:attribute>
	      </xsl:if>
              <topology>
                <xsl:attribute name="sockets">1</xsl:attribute>
                <xsl:attribute name="cores">
		  <xsl:value-of select="/instance/cores"/>
		</xsl:attribute>
                <xsl:attribute name="threads">1</xsl:attribute>
	      </topology>
            </cpu>
            <memory>
                <xsl:value-of select="/instance/memoryKB"/>
            </memory>
            <devices> 
                <xsl:if test="/instance/hypervisor/@type = 'xen' and ( /instance/os/@platform = 'windows' or /instance/backing/root/@type = 'ebs' )">
                    <xsl:choose>
                        <xsl:when test="/instance/hypervisor/@bitness = '32'">
                            <emulator>/usr/lib/xen/bin/qemu-dm</emulator>
                        </xsl:when>
                        <xsl:otherwise>
                            <emulator>/usr/lib64/xen/bin/qemu-dm</emulator>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>

                <rng model="virtio">
                    <backend model="random">/dev/urandom</backend>
                </rng>

                <!-- disks or partitions (on Xen) from VBR -->

                <xsl:for-each select="/instance/disks/diskPath">
                    <disk>
                        <xsl:attribute name="device">
                            <xsl:value-of select="@targetDeviceType"/>
                        </xsl:attribute>
                        <xsl:attribute name="type">
                            <xsl:value-of select="@sourceType"/>
                        </xsl:attribute>
                        <driver cache="none"/>
                        <source>
                            <xsl:choose>
                                <xsl:when test="@sourceType = 'file'">
                                    <xsl:attribute name="file">
                                        <xsl:value-of select="."/>
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:when test="@sourceType = 'block'">
                                    <xsl:attribute name="dev">
                                        <xsl:value-of select="."/>
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:message terminate="yes">ERROR: invalid or unset /instance/disks/disk/@sourcetype parameter</xsl:message>
                                </xsl:otherwise>
                            </xsl:choose>
                        </source>
                        <target>
	                    <xsl:choose> 
			       <xsl:when test="(/instance/hypervisor/@type='kvm' or /instance/hypervisor/@type = 'qemu') and ( /instance/os/@platform='windows' or /instance/os/@virtioRoot = 'true')">
                                   <xsl:attribute name="bus">virtio</xsl:attribute>
			  	   <xsl:attribute name="dev"> 
                                        <xsl:call-template name="string-replace-all">
 		    		           <xsl:with-param name="text" select="@targetDeviceName"/>
		  		           <xsl:with-param name="replace" select="'sd'"/>
                                           <xsl:with-param name="by" select="'vd'"/>
 				        </xsl:call-template>
                                   </xsl:attribute>
	                       </xsl:when>
			       <xsl:when test="/instance/hypervisor/@type='xen' and ( /instance/os/@platform='windows' or /instance/backing/root/@type = 'ebs' )"> 
                                  <xsl:attribute name="bus">xen</xsl:attribute>
				  <xsl:attribute name="dev">
					<xsl:call-template name="string-replace-all">
					    <xsl:with-param name="text" select="@targetDeviceName"/>
					    <xsl:with-param name="replace" select="'sd'"/>
				            <xsl:with-param name="by" select="'xvd'"/>
                                        </xsl:call-template>
				  </xsl:attribute>
			       </xsl:when>
			       <xsl:otherwise>
			           <xsl:attribute name="dev">
                               		<xsl:value-of select="@targetDeviceName"/>
                            	   </xsl:attribute>
                            	   <xsl:attribute name="bus">
                               		<xsl:value-of select="@targetDeviceBus"/>
                            	   </xsl:attribute>
			        </xsl:otherwise>
	                    </xsl:choose>
                        </target>
			<serial>
			  <xsl:value-of select="@serial"/>
			</serial>
                    </disk>
                </xsl:for-each>
                <xsl:if test="/instance/disks/floppyPath != ''">
                    <disk type="file" device="floppy">
                        <driver cache="none"/>
                        <source>
                            <xsl:attribute name="file">
                               <xsl:value-of select="/instance/disks/floppyPath"/>
                            </xsl:attribute>
                        </source>
                        <target dev="fda"/>
                    </disk>
                </xsl:if>

		<!-- disks backed by EBS -->

                <xsl:for-each select="/instance/volumes/volume/libvirt">
		    <xsl:copy-of select="node()"/>
		</xsl:for-each>

                <!-- network cards -->

                <xsl:for-each select="/instance/nics/nic">
                    <xsl:call-template name="nic"/>
                    <!-- <interface type="bridge">
                        <source>
                            <xsl:attribute name="bridge">
                                <xsl:value-of select="@bridgeDeviceName"/>
                            </xsl:attribute>
                        </source>
                        <mac>
                            <xsl:attribute name="address">
                                <xsl:value-of select="@mac"/>
                            </xsl:attribute>
                        </mac>
			<target>
                          <xsl:attribute name="dev">
                            <xsl:value-of select="@guestDeviceName"/>
                          </xsl:attribute>
                        </target>
                        <xsl:choose>
                            <xsl:when test="(/instance/hypervisor/@type='kvm' or /instance/hypervisor/@type = 'qemu') and /instance/os/@platform = 'windows'">
                                <model type="virtio"/>
                            </xsl:when>
                            <xsl:when test="(/instance/hypervisor/@type='kvm' or /instance/hypervisor/@type = 'qemu') and /instance/os/@virtioNetwork = 'true'">
                                <model type="virtio"/>
                            </xsl:when>
                            <xsl:when test="/instance/hypervisor/@type = 'kvm' and /instance/os/@platform = 'linux'">
                                <model type="e1000"/>
                            </xsl:when>
                        </xsl:choose>
                        <xsl:if test="/instance/hypervisor/@type = 'xen'">
                            <script path="/etc/xen/scripts/vif-bridge"/>
                        </xsl:if> -->
                        <!-- Directive to libvirt for ensuring that primary nic is eth0 interface in guest -->
                        <!--  <xsl:if test="@device = '0'">
                        	<address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>
                        </xsl:if>
                    </interface> -->
                </xsl:for-each>

		<!-- console -->

	<xsl:choose>
                <xsl:when test="(/instance/hypervisor/@type = 'kvm' or /instance/hypervisor/@type = 'qemu')">
                    <serial type="file">
                        <source>
                            <xsl:attribute name="append">on</xsl:attribute>
                            <xsl:attribute name="path">
                                <xsl:value-of select="/instance/consoleLogPath"/>
                            </xsl:attribute>
                        </source>
                        <target port="1"/>
                    </serial>
                </xsl:when>
                <xsl:when test="/instance/hypervisor/@type = 'xen' and /instance/os/@platform = 'windows'">
                    <serial type="pty">
                        <source path="/dev/pts/3"/>
                        <target port="0"/>
                    </serial>
                    <input type="tablet" bus="usb"/>
                    <input type="mouse" bus="ps2"/>
                </xsl:when>
                <xsl:when test="/instance/hypervisor/@type = 'xen' and /instance/backing/root/@type = 'ebs'">
                    <console type="pty"/>
                </xsl:when>
	</xsl:choose>
                <!-- <graphics type='vnc' port='-1' autoport='yes' keymap='en-us'/> -->
            </devices>
        </domain>
    </xsl:template>

    <xsl:template match="/volume">
      <disk type="block">
	<driver cache="none">
	  <xsl:choose> 
	    <xsl:when test="/volume/hypervisor/@type='xen'">
	      <xsl:attribute name="name">phy</xsl:attribute>
	    </xsl:when>
	    <xsl:when test="/volume/hypervisor/@type='kvm' or /volume/hypervisor/@type='qemu'">
	      <xsl:attribute name="name">qemu</xsl:attribute>
	    </xsl:when>
	  </xsl:choose>
	</driver>
	<source>
	  <xsl:attribute name="dev">
	    <xsl:value-of select="/volume/diskPath"/>
	  </xsl:attribute>
	</source>
	<target>
	  <xsl:choose> 
            <!-- on KVM, always use virtio disk devices for Windows and when requested to do so in NC configuration -->
            <!-- NOTE: Alternatively, we can limit non-Windows use of virtio to when contains(/volume/diskPath/@targetDeviceName, 'vd') -->
	    <xsl:when test="(/volume/hypervisor/@type='kvm' or /volume/hypervisor/@type = 'qemu') and ( /volume/os/@platform='windows' or /volume/os/@virtioDisk = 'true')">
              <xsl:attribute name="bus">virtio</xsl:attribute>
	      <xsl:attribute name="dev"> 
                <xsl:call-template name="string-replace-all">
 		  <xsl:with-param name="text" select="/volume/diskPath/@targetDeviceName"/>
		  <xsl:with-param name="replace" select="'sd'"/>
                  <xsl:with-param name="by" select="'vd'"/>
 		</xsl:call-template>
              </xsl:attribute>
	    </xsl:when>
            <!-- on Xen, always use Xen PV disk devices for Windows and when attaching to an EBS-backed instance -->
            <!-- Long-term, we should probably mandate PV devices for instance-store-backed instances, too, but we did not want to break existing images -->
	    <xsl:when test="/volume/hypervisor/@type='xen' and ( /volume/os/@platform='windows' or /volume/backing/root/@type = 'ebs' )"> 
              <xsl:attribute name="bus">xen</xsl:attribute>
	      <xsl:attribute name="dev">
		<xsl:call-template name="string-replace-all">
		  <xsl:with-param name="text" select="/volume/diskPath/@targetDeviceName"/>
		  <xsl:with-param name="replace" select="'sd'"/>
		  <xsl:with-param name="by" select="'xvd'"/>
                </xsl:call-template>
	      </xsl:attribute>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:attribute name="dev">
                <xsl:value-of select="/volume/diskPath/@targetDeviceName"/>
              </xsl:attribute>
              <xsl:attribute name="bus">
                <xsl:value-of select="/volume/diskPath/@targetDeviceBus"/>
              </xsl:attribute>
	    </xsl:otherwise>
	  </xsl:choose>
	</target>
        <xsl:if test="/volume/diskPath/@serial != ''">
          <serial>
            <xsl:value-of select="/volume/diskPath/@serial"/>
          </serial>
        </xsl:if>
      </disk>
    </xsl:template>

    <xsl:template name="string-replace-all">
        <xsl:param name="text" />
        <xsl:param name="replace" />
        <xsl:param name="by" />
        <xsl:choose>
            <xsl:when test="contains($text, $replace)">
                <xsl:value-of select="substring-before($text,$replace)" />
                <xsl:value-of select="$by" />
                    <xsl:call-template name="string-replace-all">
                        <xsl:with-param name="text"
                           select="substring-after($text,$replace)" />
                        <xsl:with-param name="replace" select="$replace" />
                        <xsl:with-param name="by" select="$by" />
                    </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="nic" match="/nic">
      <interface type="bridge">
        <source>
          <xsl:attribute name="bridge">
            <xsl:value-of select="@bridgeDeviceName" />
          </xsl:attribute>
        </source>
        <mac>
          <xsl:attribute name="address">
            <xsl:value-of select="@mac" />
          </xsl:attribute>
        </mac>
        <target>
          <xsl:attribute name="dev">
            <xsl:value-of select="@guestDeviceName" />
          </xsl:attribute>
        </target>
        <xsl:choose>
          <xsl:when test="(@hypervisorType='kvm' or @hypervisorType = 'qemu') and @osPlatform = 'windows'">
            <model type="virtio" />
          </xsl:when>
          <xsl:when test="(@hypervisorType='kvm' or@hypervisorType = 'qemu') and @osVirtioNetwork = 'true'">
            <model type="virtio" />
          </xsl:when>
          <xsl:when test="@hypervisorType = 'kvm' and @osPlatform = 'linux'">
            <model type="e1000" />
          </xsl:when>
        </xsl:choose>
        <xsl:if test="@hypervisorType = 'xen'">
          <script path="/etc/xen/scripts/vif-bridge" />
        </xsl:if>
        <!-- Directive to libvirt for ensuring that primary nic is eth0 interface in guest -->
        <xsl:if test="@device = '0'">
          <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0' />
        </xsl:if>
      </interface>
    </xsl:template>
    
</xsl:transform>
