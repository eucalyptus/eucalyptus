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
%define __libvirt euca-libvirt >= 1.5
%define __xen     xen
%define __curl    curl
%define __bridge  xenbr0
%endif

Summary:       Elastic Utility Computing Architecture
Name:          eucalyptus
Version:       1.5.2
Release:       1
License:       BSD
Group:         Applications/System
%if %is_centos
BuildRequires: gcc, make, euca-libvirt >= 1.5, curl-devel, ant, ant-nodeps, java-sdk >= 1.6.0, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0
Requires:      vconfig, aoetools, vblade, wget, rsync
%endif
%if %is_suse
BuildRequires: gcc, make, libcurl-devel, ant, ant-nodeps, java-sdk >= 1.6.0, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0
Requires:      vlan, aoetools, vblade
%endif

Conflicts:     eucalyptus-cloud < 1.5, eucalyptus-cc < 1.5, eucalyptus-nc < 1.5
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

%package cloud
Summary:      Elastic Utility Computing Architecture - cloud controller
Requires:     eucalyptus >= 1.5.2, java-sdk >= 1.6.0, ant, ant-nodeps, lvm2
Conflicts:    eucalyptus < 1.5, eucalyptus-cc < 1.5
Group:        Applications/System

%description cloud
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cloud controller part of eucalyptus.

%package cc
Summary:      Elastic Utility Computing Architecture - cluster controller
Requires:     eucalyptus >= 1.5.2, %{__httpd}, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0, iptables, bridge-utils, eucalyptus-gl >= 1.5, %{__dhcp}
Conflicts:    eucalyptus < 1.5, eucalyptus-nc < 1.5
Group:        Applications/System

%description cc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cluster controller part of eucalyptus.

%package nc
Summary:      Elastic Utility Computing Architecture - node controller
Requires:     eucalyptus >= 1.5.2, %{__httpd}, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0, bridge-utils, eucalyptus-gl >= 1.5, %{__libvirt}, %{__curl}, %{__xen}
Conflicts:    eucalyptus < 1.5, eucalyptus-cc < 1.5
Group:        Applications/System

%description nc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the node controller part of eucalyptus.

%package gl
Summary:      Elastic Utility Computing Architecture - log service
Requires:     eucalyptus >= 1.5, %{__httpd}, euca-axis2c >= 1.5.0, euca-rampartc >= 1.2.0
Conflicts:    eucalyptus < 1.5
Group:        Applications/System

%description gl
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the internal log service of eucalyptus.

%prep
%setup -n eucalyptus

%build
# let's be sure we have the right configuration file
if [ -f tools/eucalyptus.conf.rpmbased ];
then
	cp -f tools/eucalyptus.conf.rpmbased tools/eucalyptus.conf
fi
%if %is_suse
./configure --with-axis2=/opt/packages/axis2-1.4 --with-axis2c=/opt/euca-axis2c --enable-debug --prefix=/opt/eucalyptus
%endif
%if %is_centos
./configure --with-libvirt=/opt/euca-libvirt --with-axis2=/opt/packages/axis2-1.4 --with-axis2c=/opt/euca-axis2c --enable-debug --prefix=/opt/eucalyptus
%endif
cd clc
make deps
cd ..
make

%install
make install

%clean
rm -rf /opt/eucalyptus/etc /opt/eucalyptus/usr /opt/eucalyptus/var
rm -rf /opt/eucalyptus/lib
rm -rf $RPM_BUILD_DIR/eucalyptus
rm -f /etc/init.d/eucalyptus-cloud /etc/init.d/eucalyptus-nc /etc/init.d/eucalyptus-cc

%files
%doc LICENSE INSTALL README CHANGELOG
/opt/eucalyptus/etc/eucalyptus/eucalyptus.conf
/opt/eucalyptus/var/lib/eucalyptus/keys
/opt/eucalyptus/var/log
/opt/eucalyptus/var/run
/opt/eucalyptus/usr/share/eucalyptus/add_key.pl
/opt/eucalyptus/usr/share/eucalyptus/euca_ipt
/opt/eucalyptus/usr/share/eucalyptus/populate_arp.pl
/opt/eucalyptus/usr/lib/eucalyptus/euca_rootwrap
/opt/eucalyptus/usr/sbin/euca_conf
/opt/eucalyptus/usr/sbin/euca_sync_key
/opt/eucalyptus/usr/sbin/euca_killall
/opt/eucalyptus/etc/eucalyptus/httpd.conf
/opt/eucalyptus/etc/eucalyptus/eucalyptus-version

%files cloud
/opt/eucalyptus/etc/eucalyptus/cloud.d/eucalyptus.xml
/opt/eucalyptus/etc/eucalyptus/cloud.d
/opt/eucalyptus/etc/eucalyptus/cloud.xml
/opt/eucalyptus/usr/share/eucalyptus/*jar
/opt/eucalyptus/var/lib/eucalyptus/db
/opt/eucalyptus/var/lib/eucalyptus/modules
/opt/eucalyptus/var/lib/eucalyptus/webapps
/opt/eucalyptus/usr/lib/eucalyptus/libfsstorage.so
/opt/eucalyptus/usr/lib/eucalyptus/liblvm2control.so
/opt/eucalyptus/etc/init.d/eucalyptus-cloud

%files cc
/opt/euca-axis2c/services/EucalyptusCC
/opt/eucalyptus/etc/init.d/eucalyptus-cc

%files nc
/opt/eucalyptus/usr/lib/eucalyptus/euca_mountwrap
/opt/eucalyptus/usr/share/eucalyptus/gen_libvirt_xml
/opt/eucalyptus/usr/share/eucalyptus/gen_kvm_libvirt_xml
/opt/eucalyptus/usr/share/eucalyptus/partition2disk
/opt/eucalyptus/usr/share/eucalyptus/get_xen_info
/opt/eucalyptus/usr/share/eucalyptus/get_sys_info
/opt/eucalyptus/usr/share/eucalyptus/detach.pl
/opt/eucalyptus/usr/sbin/euca_test_nc
/opt/euca-axis2c/services/EucalyptusNC
/opt/eucalyptus/etc/init.d/eucalyptus-nc

%files gl
/opt/euca-axis2c/services/EucalyptusGL

%pre
if [ -x /etc/init.d/eucalyptus ]; 
then
	# stop the old services
	/etc/init.d/eucalyptus stop || true
	chkconfig --del eucalyptus || true
fi
if [ "$1" = "2" ]; 
then
	cd /opt/eucalyptus

	# save a copy of the old conf file
	cp -f etc/eucalyptus/eucalyptus.conf etc/eucalyptus/eucalyptus.conf.preupgrade

	# let's check if we have already the db in the right place, then
	# it's an upgrade from >= 1.5.x and no special case
	if [ ! -e etc/eucalyptus/eucalyptus-version ];
	then

		# we upgrade only from 1.4
		if [ ! -e usr/share/eucalyptus/euca_ipt ];
		then
			echo "Cannot upgrade from version earlier than 1.4"
			exit 2
		fi

		# let's try to save the old configuration
		if [ -e /root/eucalyptus-pre-%{version}-rollback.tar ];
		then
			mv -f /root/eucalyptus-pre-%{version}-rollback.tar /root/eucalyptus-pre-%{version}-rollback.tar.old
		fi

		# let's save database and keys
		rm -f var/eucalyptus/db/eucalyptus.lck
		tar cf /root/eucalyptus-pre-%{version}-rollback.tar var/eucalyptus/db var/eucalyptus/keys/*.p* 2> /dev/null || true
	fi
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
# let's get the default bridge 
/opt/eucalyptus/usr/sbin/euca_conf -bridge %{__bridge} 

# upgrade?
if [ "$1" = "2" ];
then
	cd /opt/eucalyptus
	
	# eucalyptus.conf was marked noreplace, so the new one could be named
	# *.rpmnew. Let's move it over (we did take a copy anyway)
	if [ -e etc/eucalyptus/eucalyptus.conf.rpmnew -a etc/eucalyptus/eucalyptus.conf.rpmnew -nt etc/eucalyptus/eucalyptus.conf ];
	then
		cp -f /opt/eucalyptus/etc/eucalyptus/eucalyptus.conf.rpmnew /opt/eucalyptus/etc/eucalyptus/eucalyptus.conf
	fi

	# if we have an old config file we try to upgrade
	if [ -e etc/eucalyptus/eucalyptus.conf.preupgrade ];
	then
		usr/sbin/euca_conf -upgrade-conf /opt/eucalyptus/etc/eucalyptus/eucalyptus.conf.preupgrade 
	fi

	# and now let's move the keys into the new place
	if [ -e var/eucalyptus/keys/cloud-cert.pem ];
	then
		mv -f var/eucalyptus/keys/*.p* var/lib/eucalyptus/keys
	fi
fi
# final setup and set the new user
/opt/eucalyptus/usr/sbin/euca_conf -d /opt/eucalyptus -setup -user eucalyptus

%post cloud
if [ "$1" = "2" ]; 
then
	cd /opt/eucalyptus

	# if upgrading from version 1.5.x nothing to do
	if [ ! -e var/lib/eucalyptus/db/eucalyptus.script ];
	then
		if [ -d var/eucalyptus/db ];
		then
			mkdir -p var/lib/eucalyptus/db
			cp -ar var/eucalyptus/db var/lib/eucalyptus
		fi
	fi
fi
ln -sf /opt/eucalyptus/etc/init.d/eucalyptus-cloud /etc/init.d/eucalyptus-cloud
chkconfig --add eucalyptus-cloud

%post cc
ln -sf /opt/eucalyptus/etc/init.d/eucalyptus-cc /etc/init.d/eucalyptus-cc
chkconfig --add eucalyptus-cc

%post nc
ln -sf /opt/eucalyptus/etc/init.d/eucalyptus-nc /etc/init.d/eucalyptus-nc
. /opt/eucalyptus/etc/eucalyptus/eucalyptus.conf
chkconfig --add eucalyptus-nc

%postun
# in case of removal let's try to clean up the best we can
if [ "$1" = "0" ];
then
	rm -rf /opt/eucalyptus/var/log
	rm -rf /opt/eucalyptus/usr
	rm -rf /opt/eucalyptus/etc/eucalyptus/http*
	rm -rf /opt/eucalyptus/etc/init.d
	# for now we leave the keys, the database and the config file
fi

%preun cloud
if [ -x /opt/eucalyptus/usr/sbin/euca_conf ];
then
	if [ -x /etc/init.d/eucalyptus-cloud ]; 
	then
		/etc/init.d/eucalyptus-cloud stop || /bin/true
	fi
fi
if [ "$1" = "0" ];
then
	chkconfig --del eucalyptus-cloud || true
fi

%preun cc
if [ -x /opt/eucalyptus/usr/sbin/euca_conf ];
then
	if [ -x /etc/init.d/eucalyptus-cc ]; 
	then
		/etc/init.d/eucalyptus-cc stop || /bin/true
	fi
fi
if [ "$1" = "0" ];
then
	chkconfig --del eucalyptus-cc || true
fi

%preun nc
if [ -x /opt/eucalyptus/usr/sbin/euca_conf ];
then
	if [ -x /etc/init.d/eucalyptus-nc ]; 
	then
		/etc/init.d/eucalyptus-nc stop || /bin/true
	fi
fi
if [ "$1" = "0" ];
then
	chkconfig --del eucalyptus-nc || true
fi

%changelog gl
*Mon Jun 15 2009 Eucalyptus Systems (support@open.eucalyptus.com)
- New version (1.5.2)

*Thu Apr 16 2009 mayhem group (support@open.eucalyptus.com)
- New eucalyptus version

*Mon Jan  5 2009 mayhem group (support@open.eucalyptus.com)
- Added new service

%changelog cloud
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
