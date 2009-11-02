%define is_suse %(test -e /etc/SuSE-release && echo 1 || echo 0)
%define is_centos %(test -e /etc/redhat-release && echo 1 || echo 0)
%if %is_suse
%define __dhcp    dhcp-server
%define __httpd   apache2
%define __libvirt libvirt
%define __xen     xen, xen-tools
%define __curl    libcurl4
%define __bridge  br0
%endif
%if %is_centos
%define __dhcp    dhcp
%define __httpd   httpd
%define __libvirt libvirt >= 0.6
%define __xen     xen
%define __curl    curl
%define __bridge  xenbr0
%endif

Summary:       Elastic Utility Computing Architecture
Name:          eucalyptus
Version:       1.6.1
Release:       1
License:       GPLv3
Group:         Applications/System
%if %is_centos
BuildRequires: gcc, make, libvirt >= 0.6, curl-devel, ant, ant-nodeps, java-sdk >= 1.6.0, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0
Requires:      vconfig, wget, rsync
%endif
%if %is_suse
BuildRequires: gcc, make, libcurl-devel, ant, ant-nodeps, java-sdk >= 1.6.0, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0
Requires:      vlan
%endif

Conflicts:     eucalyptus-cloud < 1.6, eucalyptus-cc < 1.6, eucalyptus-nc < 1.6
Vendor:        Eucalyptus Systems
#Icon:          someicon.xpm
Source:        http://open.eucalyptus.com/downloads/eucalyptus-%{version}.tgz
URL:           http://open.eucalyptus.com

%description
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the common parts: you will need to install either
eucalyptus-cloud, eucalyptus-cc or eucalyptus-nc (or all of them).

%package common-java
Summary:      Elastic Utility Computing Architecture - ws java stack 
Requires:     eucalyptus >= 1.6, java-sdk >= 1.6.0, lvm2, groovy
Conflicts:    eucalyptus < 1.6
Group:        Applications/System

%description common-java
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the java WS stack.

%package walrus
Summary:      Elastic Utility Computing Architecture - cloud controller
Requires:     eucalyptus-common-java >= 1.6, java-sdk >= 1.6.0, lvm2
Conflicts:    eucalyptus-walrus < 1.6
Group:        Applications/System

%description walrus
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains walrus.

%package sc
Summary:      Elastic Utility Computing Architecture - walrus
Requires:     eucalyptus-common-java >= 1.6, java-sdk >= 1.6.0, lvm2, vblade
Conflicts:    eucalyptus-cloud < 1.6
Group:        Applications/System

%description sc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the storage controller part of eucalyptus.

%package cloud
Summary:      Elastic Utility Computing Architecture - cloud controller
Requires:     eucalyptus-common-java >= 1.6, java-sdk >= 1.6.0, lvm2
Conflicts:    eucalyptus-cloud < 1.6
Group:        Applications/System

%description cloud
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cloud controller part of eucalyptus.

%package cc
Summary:      Elastic Utility Computing Architecture - cluster controller
Requires:     eucalyptus >= 1.6, %{__httpd}, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0, iptables, bridge-utils, eucalyptus-gl >= 1.5, %{__dhcp}, vtun
Conflicts:    eucalyptus < 1.6, eucalyptus-nc < 1.6
Group:        Applications/System

%description cc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cluster controller part of eucalyptus.

%package nc
Summary:      Elastic Utility Computing Architecture - node controller
Requires:     eucalyptus >= 1.6, %{__httpd}, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0, bridge-utils, eucalyptus-gl >= 1.5, %{__libvirt}, %{__curl}, %{__xen}
Conflicts:    eucalyptus < 1.6, eucalyptus-cc < 1.6
Group:        Applications/System

%description nc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the node controller part of eucalyptus.

%package gl
Summary:      Elastic Utility Computing Architecture - log service
Requires:     eucalyptus >= 1.6, %{__httpd}, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0
Conflicts:    eucalyptus < 1.6
Group:        Applications/System

%description gl
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the internal log service of eucalyptus.

%prep
%setup -n eucalyptus-%{version}

%build
./configure --with-axis2=/opt/packages/axis2-1.4 --with-axis2c=/opt/euca-axis2c --enable-debug --prefix=/
cd clc
make deps
cd ..
make 2> err.log > out.log

%install
make install
ls /usr/share/eucalyptus/*jar|grep -v eucalyptus-walrus|grep -v eucalyptus-storagecontroller|grep -v eucalyptus-cloud > jar_list

%clean
make uninstall
rm -rf $RPM_BUILD_DIR/eucalyptus-%{version}
# most of the files are taken care of by uninstall, but not the
# directories
rm -rf /var/lib/eucalyptus
rm -rf /var/run/eucalyptus
rm -rf /usr/lib/eucalyptus
rm -rf /usr/share/eucalyptus
rm -rf /etc/eucalyptus
rm -rf /usr/share/doc/eucalyptus-%{version}

%files
%doc LICENSE INSTALL README CHANGELOG
/etc/eucalyptus/eucalyptus.conf
/var/lib/eucalyptus/keys
/var/log/eucalyptus
/var/run/eucalyptus
/usr/share/eucalyptus/add_key.pl
/usr/share/eucalyptus/euca_ipt
/usr/share/eucalyptus/populate_arp.pl
/usr/lib/eucalyptus/euca_rootwrap
/usr/lib/eucalyptus/euca_mountwrap
/usr/sbin/euca_conf
/usr/sbin/euca_sync_key
/usr/sbin/euca_killall
/etc/eucalyptus/httpd.conf
/etc/eucalyptus/eucalyptus-version

%files common-java -f jar_list
/etc/init.d/eucalyptus-cloud
/etc/eucalyptus/cloud.d
/var/lib/eucalyptus/db
/var/lib/eucalyptus/modules
/var/lib/eucalyptus/webapps
/usr/lib/eucalyptus/liblvm2control.so
/usr/sbin/eucalyptus-cloud

%files cloud
/usr/share/eucalyptus/eucalyptus-cloud-*.jar

%files walrus
/usr/share/eucalyptus/eucalyptus-walrus-*.jar

%files sc
/usr/share/eucalyptus/eucalyptus-storagecontroller-*.jar

%files cc
/opt/euca-axis2c/services/EucalyptusCC
/etc/init.d/eucalyptus-cc
/etc/eucalyptus/vtunall.conf.template

%files nc
/usr/share/eucalyptus/gen_libvirt_xml
/usr/share/eucalyptus/gen_kvm_libvirt_xml
/usr/share/eucalyptus/partition2disk
/usr/share/eucalyptus/get_xen_info
/usr/share/eucalyptus/get_sys_info
/usr/share/eucalyptus/detach.pl
/usr/sbin/euca_test_nc
/opt/euca-axis2c/services/EucalyptusNC
/etc/init.d/eucalyptus-nc

%files gl
/opt/euca-axis2c/services/EucalyptusGL

%pre
if [ "$1" = "2" ]; 
then
	# let's see where we installed
	cd /
	[ -e /opt/eucalyptus/etc/eucalyptus/eucalyptus-version ] && cd /opt/eucalyptus

	# stop all old services
	[ -x etc/init.d/eucalyptus-cloud ] && etc/init.d/eucalyptus-cloud stop 
	[ -x etc/init.d/eucalyptus-cc ] && etc/init.d/eucalyptus-cc stop
	[ -x etc/init.d/eucalyptus-nc ] && etc/init.d/eucalyptus-nc stop

	# used for upgrade
	cp -f etc/eucalyptus/eucalyptus.conf /etc/eucalyptus/eucalyptus.conf.old
fi

%post
# we need a eucalyptus user
if ! getent passwd eucalyptus > /dev/null ; then
%if %is_suse
	groupadd eucalyptus
	useradd -M eucalyptus -g eucalyptus
%endif
%if %is_centos
	adduser -M eucalyptus 
%endif
fi
# let's configure eucalyptus
/usr/sbin/euca_conf -d / --instances /usr/local/eucalyptus/ -hypervisor xen -bridge %{__bridge}

# final setup and set the new user
/usr/sbin/euca_conf -setup -user eucalyptus

%post common-java
chkconfig --add eucalyptus-cloud
if [ "$1" = "2" ];
then
	if [ -e /opt/eucalyptus/var/lib/eucalyptus/db/eucalyptus_volumes.properties ];
	then
		/usr/share/eucalyptus/euca_upgrade --old /opt/eucalyptus --new / || true
	fi
fi

%post cloud
/usr/sbin/euca_conf --enable cloud
%if %is_centos
if [ -e /etc/sysconfig/system-config-securitylevel ];
then
	if ! grep 8773:tcp /etc/sysconfig/system-config-securitylevel > /dev/null ; 
	then
		echo "--port=8773:tcp" >> /etc/sysconfig/system-config-securitylevel
		echo "--port=8443:tcp" >> /etc/sysconfig/system-config-securitylevel
	fi
fi
%endif

%post walrus
/usr/sbin/euca_conf --enable walrus

%post sc
/usr/sbin/euca_conf --enable sc

%post cc
chkconfig --add eucalyptus-cc
%if %is_centos
if [ -e /etc/sysconfig/system-config-securitylevel ];
then
	if ! grep 8774:tcp /etc/sysconfig/system-config-securitylevel > /dev/null ; 
	then
		echo "--port=8774:tcp" >> /etc/sysconfig/system-config-securitylevel
	fi
fi
%endif

%post nc
chkconfig --add eucalyptus-nc
%if %is_centos
if [ -e /etc/sysconfig/system-config-securitylevel ];
then
	if ! grep 8775:tcp /etc/sysconfig/system-config-securitylevel > /dev/null ; 
	then
		echo "--port=8775:tcp" >> /etc/sysconfig/system-config-securitylevel
	fi
fi
%endif
%if %is_suse
if [ -e /etc/PolicyKit/PolicyKit.conf ]; 
then
	if ! grep eucalyptus /etc/PolicyKit/PolicyKit.conf > /dev/null ;
	then
		sed -i '/<config version/ a <match action="org.libvirt.unix.manage">\n   <match user="eucalyptus">\n      <return result="yes"/>\n   </match>\n</match>' /etc/PolicyKit/PolicyKit.conf
	fi
fi
%endif


%postun
# in case of removal let's try to clean up the best we can
if [ "$1" = "0" ];
then
	rm -rf /var/log/eucalyptus
	rm -rf /etc/eucalyptus/http*
fi

%preun cloud
if [ "$1" = "0" ];
then
%if %is_centos
	if [ -e /etc/sysconfig/system-config-securitylevel ];
	then
		sed -i '/^--port=8773/ d' /etc/sysconfig/system-config-securitylevel
		sed -i '/^--port=8443/ d' /etc/sysconfig/system-config-securitylevel
	fi
%endif
	[ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable cloud
	[ -e /etc/init.d/eucalyptus-cloud ] && /etc/init.d/eucalyptus-cloud restart
fi


%preun walrus
if [ "$1" = "0" ];
then
	[ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable walrus
	[ -e /etc/init.d/eucalyptus-cloud ] && /etc/init.d/eucalyptus-cloud restart
fi

%preun sc
if [ "$1" = "0" ];
then
	[ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable sc
	[ -e /etc/init.d/eucalyptus-cloud ] && /etc/init.d/eucalyptus-cloud restart
fi

%preun common-java
if [ "$1" = "0" ];
then
	/etc/init.d/eucalyptus-cloud stop || true
	chkconfig --del eucalyptus-cloud || true
	rm -f /var/lib/eucalyptus/services
fi

%preun cc
if [ "$1" = "0" ];
then
	/etc/init.d/eucalyptus-cc stop || true
	chkconfig --del eucalyptus-cc || true
%if %is_centos
	if [ -e /etc/sysconfig/system-config-securitylevel ];
	then
		sed -i '/^--port=8774/ d' /etc/sysconfig/system-config-securitylevel
	fi
%endif
fi

%preun nc
if [ "$1" = "0" ];
then
	/etc/init.d/eucalyptus-nc stop || true
	chkconfig --del eucalyptus-nc || true
%if %is_centos
	if [ -e /etc/sysconfig/system-config-securitylevel ];
	then
		sed -i '/^--port=8775/ d' /etc/sysconfig/system-config-securitylevel
	fi
%endif
fi

%changelog gl
*Sun Nov 1 2009 Eucalyptus Systems (support@open.eucalyptus.com)
- New version (1.6.1)
- install in / instead of /opt/eucalyptus

*Mon Jun 15 2009 Eucalyptus Systems (support@open.eucalyptus.com)
- New version (1.5.2)

*Thu Apr 16 2009 mayhem group (support@open.eucalyptus.com)
- New eucalyptus version

*Mon Jan  5 2009 mayhem group (support@open.eucalyptus.com)
- Added new service

%changelog cloud
*Sun Nov 1 2009 Eucalyptus Systems (support@open.eucalyptus.com)
- New version (1.6.1)
- install in / instead of /opt/eucalyptus

*Mon Jun 15 2009 eucalyptus systems (support@open.eucalyptus.com)
- New version (1.5.2)

*Thu Apr 16 2009 mayhem group (support@open.eucalyptus.com)
- Support for groups in ACLS
- Fixed issues with meta data support
- Web browser form-based uploads via HTTP POST
- Object copying
- Query string authentication
- Support for arbitrary key names
- Compressed image downloads and fixes to image caching
- Reduced memory requirement 

*Mon Jan  5 2009 mayhem group (support@open.eucalyptus.com)
- Added cloud/Walrus configuration, including clusters and VM types
- Revamped 'credentials' tab with new options to edit user information
  and hide/show "secret" strings
- Editing of user information for the administrator, including
  confirmation dialog for deletion of users
- User-initiated password recovery mechanism
- Fixed a couple of bugs: ' ' in username, spurious double additions
- User, Cluster, and System keys are now stored in PKCS#12 keystores and
  have moved to var/lib/eucalyptus/keys
- Configuration is handled entirely through the web interface
- Clusters dynamically added/started/stopped
- Webservices operations complete up to EC2 2008-05-05 (w/o EBS):
- "Elastic IP" address support
- Image registration and attribute manipulation
- GetConsole and RebootInstances support
- Working Security Groups support for clusters in MANAGED network mode
- see website for additional details, extensions, and caveats:
  http://open.eucalyptus.com/wiki/API_v1.4
- Instance Metadata service (including userData)
- Workaround to use standard tools for registering kernels & ramdisks

*Mon Jul 28 2008 mayhem group (support@open.eucalyptus.com)
- First revision: split from eucalyptus to allow installation of only
  this module.
- Fix the instance ID naming collision.

%changelog cc
*Sun Nov 1 2009 Eucalyptus Systems (support@open.eucalyptus.com)
- New version (1.6.1)
- install in / instead of /opt/eucalyptus

*Mon Jun 15 2009 eucalyptus systems (support@open.eucalyptus.com)
- New version (1.5.2)

*Thu Apr 16 2009 mayhem group (support@open.eucalyptus.com)
- Network improvement: new MANAGED-NOVLAN mode 

*Mon Jan  5 2009 mayhem group (support@open.eucalyptus.com)
- Cluster controller now handles concurrent requests (no longer have to 
  restrict apache to allow only one connection at a time)
- Cluster controller scheduling policy can now be configured in 
  eucalyptus.conf (ROUNDROBIN and GREEDY currently supported)

*Mon Jul 28 2008 mayhem group (support@open.eucalyptus.com)
- First revision: split from eucalyptus to allow installation of only
  this module.

%changelog nc
*Sun Nov 1 2009 Eucalyptus Systems (support@open.eucalyptus.com)
- New version (1.6.1)
- install in / instead of /opt/eucalyptus

*Mon Jun 15 2009 eucalyptus systems (support@open.eucalyptus.com)
- New version (1.5.2)

*Thu Apr 16 2009 mayhem group (support@open.eucalyptus.com)
- Support for the KVM hypervisor
- Compression & retries on Walrus downloads
- Reworked caching (now with configurable limit) 

*Mon Jan  5 2009 mayhem group (support@open.eucalyptus.com)
- Retrieval of images from Walrus instead of NFS-mounted file system
- New caching and cleanup code, including start-time integrity checks
- Script-based discovery of node resources (no assumptions about stat)
- On-the-fly generation of libvirt XML configuration
- MAX_CORES overrides actual number of cores both down and up
- More robust instance state reporting to Cluster Controller
- Moved libvirt errors to nc.log and suppressed harmless ones
- Serialized some Xen invocations to guard against non-determinism
- Added proper swap creation, also "ephemeral" disk support

*Mon Jul 28 2008 mayhem group (support@open.eucalyptus.com)
- First revision: split from eucalyptus to allow installation of only
  this module.
- Implemented caching of the instances.
- More robust checking for running instances.

%changelog
*Sun Nov 1 2009 Eucalyptus Systems (support@open.eucalyptus.com)
- New version (1.6.1)
- install in / instead of /opt/eucalyptus

*Mon Jun 15 2009 eucalyptus systems (support@open.eucalyptus.com)
- New version (1.5.2)

*Thu Apr 16 2009 mayhem group (support@open.eucalyptus.com)
- Elastic Block Store (EBS) support (volumes & snapshots) 
- Better Java installation checking
- New command-line administration: euca_conf -addcluster ... -addnode ...
- Non-root user deployment of Eucalyptus
- Binary packages for more distributions (Ubuntu et al) 
- Cloud registration with Rightscale (from admin's 'Credentials' tab)
- New configuration options for Walrus
- Better screening of usernames
- Fixed account confirmation glitches 

*Mon Jan  5 2009 mayhem group (support@open.eucalyptus.com)
- Added new networking subsystem that no longer depends on VDE
- Added support for elastic IP assignment and security using the 'MANAGED' 
  networking mode
- Added Walrus: a Amazon S3 interface compatible storage manager. Walrus
  handles storage of user data as well as filesystem images, kernels, and
  ramdisks.
- Support for new operations: reboot instance and get console output.
- Revamped logging throughout, with five levels a la log4j.

*Mon Jul 28 2008 mayhem group (support@open.eucalyptus.com)
- Removed cloud, cluster controller and node controller and created their
  own packages.
- Added the possibility of installing Eucalyptus from RPMs (without ROCKS).

*Tue Jul 01 2008 mayhem group (support@open.eucalyptus.com)
- Added WS-security for internal communication
- Added URL Query Interface for interacting with Eucalyptus
- Cluster Controller improvements:
   - Instance caching added to improve performance under
     certain conditions
   - Thread locks removed to improve performance
   - NC resource information gathered asynchronously to
     improve scheduler performance
- Network control improvements:
   - Added ability to configure 'public' instance interface
     network parameters (instead of hardcoded 10. network)
   - Lots of reliability changes
- Cloud Controller improvements:
   - Pure in-memory database
   - Image registration over WS interface
   - Improved build procedure
- Web interface improvements:
    - For all users (query interface credentials, listing of
      available images)
    - For the administrator (addition, approval, disabling,
      and deletion of users; disabling of images)
- Numerous bug fixes, improving stability and performance.
   In particular, but not limited to:
   - Recovering Cloud Controller system state
   - Timeout-related error reporting
   - Slimmer log files, with timestamps

*Sat May 21 2008 mayhem group (support@open.eucalyptus.com)
- first release of eucalyptus
