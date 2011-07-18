<?xml version="1.0" encoding="UTF-8"?>
<!--
This XSL-T file is used to construct an XML document that 
libvirt can use to launch a Eucalyptus instance (the job 
formerly performed by gen_*libvirt_xml Perl scripts). As input
it assumes an XML document produced the the Node Controller
that describes a Eucalyptus instance to be launched.
-->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output encoding="UTF-8" indent="yes" method="xml"/>
    <xsl:template match="/">
        <!-- sanity check on the hypervisor type - we only know 'kvm' and 'xen' -->
        <xsl:if test="/instance/hypervisor/@type != 'kvm' and /instance/hypervisor/@type != 'xen'">
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
                    <xsl:when test="/instance/os/@platform = 'linux'">
                        <!-- Linux-specific configuration -->
                        <xsl:if test="/instance/hypervisor/@type = 'xen'">
                            <type>linux</type>
                        </xsl:if>
                        <xsl:if test="/instance/hypervisor/@type = 'kvm'">
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
                            <cmdline>root=/dev/sda1 console=ttyS0</cmdline>
                            <xsl:choose>
                                <xsl:when test="/instance/os/@virtioRoot = 'true'">
                                    <root>/dev/vda1</root>
                                </xsl:when>
                                <xsl:when test="/instance/os/@virtioRoot = 'false'">
                                    <root>/dev/sda1</root>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:message terminate="yes">ERROR: invalid or unset /instance/os/@virtioRoot parameter</xsl:message>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                    </xsl:when>
                    <xsl:when test="/instance/os/@platform = 'windows'">
                        <!-- Windows-specific configuration -->
                        <type>hvm</type>
                        <xsl:if test="/instance/hypervisor/@type = 'xen'">
                            <loader>/usr/lib/xen/boot/hvmloader</loader>
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:message terminate="yes">ERROR: invalid or unset /instance/os/@platform parameter</xsl:message>
                    </xsl:otherwise>
                </xsl:choose>
            </os>
            <features>
                <acpi/>
                <xsl:if test="/instance/hypervisor/@type = 'xen' and /instance/os/@platform = 'windows'">
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
            <memory>
                <xsl:value-of select="/instance/memoryKB"/>
            </memory>
            <devices>
                <xsl:if test="/instance/hypervisor/@type = 'xen' and /instance/os/@platform = 'windows'">
                    <emulator>/usr/lib64/xen/bin/qemu-dm</emulator>
                </xsl:if>
                <!-- disks -->
                <xsl:for-each select="/instance/disks/diskPath">
                    <disk>
                        <xsl:attribute name="device">
                            <xsl:value-of select="@targetDeviceType"/>
                        </xsl:attribute>
                        <xsl:attribute name="type">
                            <xsl:value-of select="@sourceType"/>
                        </xsl:attribute>
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
			       <xsl:when test="/instance/hypervisor/@type='kvm' and /instance/os/@platform='windows'">
                                   <xsl:attribute name="bus">virtio</xsl:attribute>
			  	   <xsl:attribute name="dev"> 
                                        <xsl:call-template name="string-replace-all">
 		    		           <xsl:with-param name="text" select="@targetDeviceName"/>
		  		           <xsl:with-param name="replace" select="'sd'"/>
                                           <xsl:with-param name="by" select="'vd'"/>
 				        </xsl:call-template>
                                   </xsl:attribute>
	                       </xsl:when>
			       <xsl:when test="/instance/hypervisor/@type='xen' and /instance/os/@platform='windows'"> 
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
                    </disk>
                </xsl:for-each>
                <xsl:if test="/instance/disks/floppyPath != ''">
                    <disk type="file" device="floppy">
                        <source>
                            <xsl:attribute name="file">
                               <xsl:value-of select="/instance/disks/floppyPath"/>
                            </xsl:attribute>
                        </source>
                        <target dev="fda"/>
                    </disk>
                </xsl:if>
                <!-- network cards -->
                <xsl:for-each select="/instance/nics/nic">
                    <interface type="bridge">
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
                        <xsl:choose>
                            <xsl:when test="/instance/hypervisor/@type = 'kvm' and /instance/os/@platform = 'windows'">
                                <model type="virtio"/>
                            </xsl:when>
                            <xsl:when test="/instance/hypervisor/@type = 'kvm' and /instance/os/@virtioRoot = 'true'">
                                <model type="virtio"/>
                            </xsl:when>
                            <xsl:when test="/instance/hypervisor/@type = 'kvm' and /instance/os/@platform = 'linux'">
                                <model type="e1000"/>
                            </xsl:when>
                        </xsl:choose>
                        <xsl:if test="/instance/hypervisor/@type = 'xen'">
                            <script path="/etc/xen/scripts/vif-bridge"/>
                        </xsl:if>
                    </interface>
                </xsl:for-each>
                <xsl:if test="/instance/hypervisor/@type = 'kvm'">
                    <serial type="file">
                        <source>
                            <xsl:attribute name="path">
                                <xsl:value-of select="/instance/consoleLogPath"/>
                            </xsl:attribute>
                        </source>
                        <target port="1"/>
                    </serial>
                </xsl:if>
                <xsl:if test="/instance/hypervisor/@type = 'xen' and /instance/os/@platform = 'windows'">
                    <serial type="pty">
                        <source path="/dev/pts/3"/>
                        <target port="0"/>
                    </serial>
                    <input type="tablet" bus="usb"/>
                    <input type="mouse" bus="ps2"/>
                </xsl:if>
                <!-- <graphics type='vnc' port='-1' autoport='yes' keymap='en-us' listen='0.0.0.0'/> -->
            </devices>
        </domain>
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
</xsl:transform>
