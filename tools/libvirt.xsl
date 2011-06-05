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
		<xsl:if test="instance/hypervisor/@type != 'kvm' and instance/hypervisor/@type != 'xen'">
			<xsl:message terminate="yes">ERROR: invalid or unset /instance/hypervisor/@type parameter</xsl:message>
		</xsl:if>
		<domain>
			<xsl:attribute name="type">
				<xsl:value-of select="instance/hypervisor/@type"/>
			</xsl:attribute>
			<name>
				<xsl:value-of select="instance/name"/>
			</name>
			<description>Eucalyptus instance <xsl:value-of select="instance/name"/></description>
			<os>
                          <xsl:choose>
                            <xsl:when test="instance/hypervisor/@capability = 'unknown'">
			      <type>hvm</type>
                            </xsl:when>
                            <xsl:when test="instance/hypervisor/@capability = 'xen'">
			      <type>linux</type>
                            </xsl:when>
                            <xsl:when test="instance/hypervisor/@capability = 'hw'">
			      <type>hvm</type>
                            </xsl:when>
                            <xsl:when test="instance/hypervisor/@capability = 'xen+hw'">
			      <type>linux</type>
                            </xsl:when>
                          </xsl:choose>
				<xsl:if test="instance/loader != ''">
				  <loader><xsl:value-of select="instance/loader"/></loader>
				</xsl:if>
				<xsl:if test="instance/ramdisk!=''">
					<initrd>
						<xsl:value-of select="instance/ramdisk"/>
					</initrd>
				</xsl:if>
				<xsl:if test="instance/kernel!=''">
					<kernel>
						<xsl:value-of select="instance/kernel"/>
					</kernel>
					<xsl:choose>
						<xsl:when test="instance/os/@virtioRoot = 'true'">
						  <cmdline>root=/dev/vda1 console=ttyS0</cmdline>
							<root>/dev/vda1</root>
						</xsl:when>
						<xsl:when test="instance/os/@virtioRoot = 'false'">
						  <cmdline>root=/dev/sda1 console=ttyS0</cmdline>
							<root>/dev/sda1</root>
						</xsl:when>
						<xsl:otherwise>
							<xsl:message terminate="yes">ERROR: invalid or unset /instance/os/@virtioRoot parameter</xsl:message>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
			</os>
			<features>
				<xsl:for-each select="instance/features">
					<xsl:copy-of select="*"/>
				</xsl:for-each>
			</features>
			<clock offset="localtime"/>
			<on_poweroff>destroy</on_poweroff>
			<on_reboot>restart</on_reboot>
			<on_crash>destroy</on_crash>
			<vcpu>
				<xsl:value-of select="instance/cores"/>
			</vcpu>
			<memory>
				<xsl:value-of select="instance/memoryKB"/>
			</memory>
			<devices>
				<xsl:if test="instance/emulator!=''">
					<emulator>
						<xsl:value-of select="instance/emulator"/>
					</emulator>
				</xsl:if>
				<xsl:for-each select="instance/disks/diskPath">
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
							    <xsl:when test="/instance/os/@virtioRoot = 'true'">
							      <xsl:attribute name="dev">
								<xsl:value-of select="@targetDeviceNameVirtio"/>
							      </xsl:attribute>
							      <xsl:attribute name="bus">
								<xsl:value-of select="@targetDeviceBusVirtio"/>
							      </xsl:attribute>
							    </xsl:when>
							    <xsl:when test="/instance/os/@virtioRoot = 'false'">
							      <xsl:attribute name="dev">
								<xsl:value-of select="@targetDeviceName"/>
							      </xsl:attribute>
							      <xsl:attribute name="bus">
								<xsl:value-of select="@targetDeviceBus"/>
							      </xsl:attribute>
							    </xsl:when>
							  </xsl:choose>
						</target>
					</disk>
				</xsl:for-each>
				<xsl:for-each select="instance/nics/nic">
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
						<model>
							<xsl:attribute name="type">
								<xsl:value-of select="@modelType"/>
							</xsl:attribute>
						</model>
						<xsl:if test="/instance/hypervisor/@type = 'xen'">
							<script path="/etc/xen/scripts/vif-bridge"/>
						</xsl:if>
					</interface>
				</xsl:for-each>
				<xsl:if test="/instance/hypervisor/@type = 'kvm'">
					<serial type="file">
						<source>
							<xsl:attribute name="path"><xsl:value-of select="/instance/consoleLogPath"/></xsl:attribute>
						</source>
						<target port="1"/>
					</serial>
				</xsl:if>
				<xsl:if test="/instance/extras/@fakeSerial = 'true'">
					<serial type="pty">
						<source path="/dev/pts/3"/>
						<target port="0"/>
					</serial>
				</xsl:if>
				<xsl:if test="/instance/extras/@fakeTablet = 'true'">
					<input type="tablet" bus="usb"/>
				</xsl:if>
				<xsl:if test="/instance/extras/@fakeMouse = 'true'">
					<input type="mouse" bus="ps2"/>
				</xsl:if>
				<!--graphics type="vnc" port="-1" autoport="yes" listen="0.0.0.0"-->
			</devices>
		</domain>
	</xsl:template>
</xsl:transform>
