%global is_suse %(test -e /etc/SuSE-release && echo 1 || echo 0)
%global is_centos %(grep CentOS /etc/redhat-release > /dev/null && echo 1 || echo 0)
%global is_fedora %(grep Fedora /etc/redhat-release > /dev/null && echo 1 || echo 0)
%global euca_dhcp    dhcp
%global euca_httpd   httpd
%global euca_curl    curl
%global euca_libcurl curl-devel
%global euca_build_req vconfig, wget, rsync
%if %is_suse
%global euca_dhcp    dhcp-server
%global euca_httpd   apache2
%global euca_libvirt xen-tools, libvirt
%global euca_hypervisor xen
%global euca_curl    libcurl4
%global euca_libcurl libcurl-devel
%global euca_bridge  br0
%global euca_java    java-sdk >= 1.6.0
%global euca_iscsi_client open-iscsi
%global euca_iscsi_server tgt
%global euca_build_req vlan
%endif
%if %is_centos
%global euca_libvirt libvirt >= 0.6
%global euca_hypervisor xen
%global euca_bridge  xenbr0
%global euca_java    java-sdk >= 1.6.0
%global euca_iscsi_client iscsi-initiator-utils
%global euca_iscsi_server scsi-target-utils
%endif
%if %is_fedora
%global euca_libvirt libvirt
%global euca_hypervisor kvm
%global euca_bridge  br0
%global euca_java    java-devel >= 1:1.6.0
%global euca_iscsi_client iscsi-initiator-utils
%global euca_iscsi_server scsi-target-utils
%endif

%if %is_centos
BuildRoot:     %{_tmppath}/%{name}-%{version}-root
%endif
Summary:       Elastic Utility Computing Architecture
Name:          eucalyptus
Version:       2.0.1
Release:       1
License:       GPLv3
Group:         Applications/System
BuildRequires: gcc, make, %{euca_libvirt}-devel, %{euca_libvirt}, %{euca_libcurl}, ant, ant-nodeps, %{euca_java}, euca-axis2c >= 1.6.0, euca-rampartc >= 1.3.0, %{euca_iscsi_client}
Requires:      %{euca_build_req}, perl-Crypt-OpenSSL-RSA, perl-Crypt-OpenSSL-Random

Source:        http://eucalyptussoftware.com/downloads/releases/eucalyptus-%{version}.tar.gz
URL:           http://open.eucalyptus.com

%description
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the common parts: you will need to install either
eucalyptus-cloud, eucalyptus-cc or
eucalyptus-nc (or all of them).

%package common-java
Summary:      Elastic Utility Computing Architecture - ws java stack 
Requires:     eucalyptus = 2.0.1, %{euca_java}, lvm2
Group:        Applications/System

%description common-java
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the java WS stack.

%package walrus
Summary:      Elastic Utility Computing Architecture - walrus
Requires:     eucalyptus-common-java = 2.0.1, %{euca_java}, lvm2
Group:        Applications/System

%description walrus
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains storage component for your cloud: images and buckets
are handled by walrus. Tipically this package is installed alongside the
cloud controller.

%package sc
Summary:      Elastic Utility Computing Architecture - storage controller
Requires:     eucalyptus-common-java = 2.0.1, %{euca_java}, lvm2, vblade, %{euca_iscsi_server}
Group:        Applications/System

%description sc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the storage controller part of eucalyptus which
handles the elastic blocks for a given cluster. Tipically you install it
alongside the cluster-controller.

%package cloud
Summary:      Elastic Utility Computing Architecture - cloud controller
Requires:     eucalyptus-common-java = 2.0.1, %{euca_java}, lvm2
Group:        Applications/System

%description cloud
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cloud controller part of eucalyptus: the cloud
controller needs to be reachable by both the cluster controller and from
the cloud clients.

%package cc
Summary:      Elastic Utility Computing Architecture - cluster controller
Requires:     eucalyptus = 2.0.1, eucalyptus-gl = 2.0.1, %{euca_httpd}, euca-axis2c >= 1.6.0, euca-rampartc >= 1.3.0, iptables, bridge-utils, %{euca_dhcp}, vtun
Group:        Applications/System

%description cc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cluster controller part of eucalyptus: it
handles multiple node controllers.

%package nc
Summary:      Elastic Utility Computing Architecture - node controller
Requires:     eucalyptus = 2.0.1, eucalyptus-gl = 2.0.1, %{euca_httpd}, euca-axis2c >= 1.6.0, euca-rampartc >= 1.3.0, bridge-utils, %{euca_libvirt}, %{euca_curl}, %{euca_hypervisor}, %{euca_iscsi_client}
Group:        Applications/System

%description nc
EUCALYPTUS is an open source service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the node controller part of eucalyptus: this is the
components that handles the instances.

%package gl
Summary:      Elastic Utility Computing Architecture - log service
Requires:     eucalyptus = 2.0.1, %{euca_httpd}, euca-axis2c >= 1.6.0, euca-rampartc >= 1.3.0
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
export DESTDIR=$RPM_BUILD_ROOT
./configure --with-axis2=/opt/packages/axis2-1.4 --with-axis2c=/opt/euca-axis2c --enable-debug --prefix=/
cd clc
make deps
cd ..
make 2> err.log > out.log

%install
export DESTDIR=$RPM_BUILD_ROOT
make install
#CWD=`pwd`
#cd $RPM_BUILD_ROOT
#ls usr/share/eucalyptus/*jar | sed "s/^/\//" > $CWD/jar_list
#cd $CWD

%clean
export DESTDIR=$RPM_BUILD_ROOT
make uninstall
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
rm -rf $RPM_BUILD_DIR/eucalyptus-%{version}

%files
%doc LICENSE INSTALL README CHANGELOG
/etc/eucalyptus/eucalyptus.conf
/var/lib/eucalyptus/keys
/var/log/eucalyptus
/var/run/eucalyptus
/usr/share/eucalyptus/add_key.pl
/usr/share/eucalyptus/euca_ipt
/usr/share/eucalyptus/populate_arp.pl
/usr/share/eucalyptus/euca_upgrade
/usr/lib/eucalyptus/euca_rootwrap
/usr/lib/eucalyptus/euca_mountwrap
/etc/bash_completion.d/euca_conf
/usr/sbin/euca_conf
/usr/sbin/euca_sync_key
/usr/sbin/euca_killall
/etc/eucalyptus/httpd.conf
/etc/eucalyptus/eucalyptus-version
/usr/share/eucalyptus/connect_iscsitarget.pl
/usr/share/eucalyptus/disconnect_iscsitarget.pl
/usr/share/eucalyptus/get_iscsitarget.pl
/usr/sbin/euca-add-user
/usr/sbin/euca-add-user-group
/usr/sbin/euca-delete-user
/usr/sbin/euca-delete-user-group
/usr/sbin/euca-deregister-cluster
/usr/sbin/euca-deregister-storage-controller
/usr/sbin/euca-deregister-walrus
/usr/sbin/euca-describe-clusters
/usr/sbin/euca-describe-properties
/usr/sbin/euca-describe-storage-controllers
/usr/sbin/euca-describe-user-groups
/usr/sbin/euca-describe-users
/usr/sbin/euca-describe-walruses
/usr/sbin/euca-get-credentials
/usr/sbin/euca-modify-property
/usr/sbin/euca-register-cluster
/usr/sbin/euca-register-storage-controller
/usr/sbin/euca-register-walrus
/usr/sbin/euca_admin/__init__.py
/usr/sbin/euca_admin/clusters.py
/usr/sbin/euca_admin/generic.py
/usr/sbin/euca_admin/groups.py
/usr/sbin/euca_admin/properties.py
/usr/sbin/euca_admin/storagecontrollers.py
/usr/sbin/euca_admin/users.py
/usr/sbin/euca_admin/walruses.py

#%files common-java -f jar_list
%files common-java
/etc/init.d/eucalyptus-cloud
/etc/eucalyptus/cloud.d
/var/lib/eucalyptus/db
/var/lib/eucalyptus/modules
/var/lib/eucalyptus/webapps
/usr/lib/eucalyptus/liblvm2control.so
/usr/sbin/eucalyptus-cloud
/usr/share/eucalyptus/*jar*
/usr/share/eucalyptus/licenses

%files cloud

%files walrus

%files sc

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
	EUCADIRS="/ /opt/eucalyptus/"
	for i in $EUCADIRS
	do
	    if [ -e $i/etc/eucalyptus/eucalyptus-version ]; then
		EUCADIR=$i
		break
	    fi
	done
	cd $EUCADIR

	# stop all old services
	if [ -x etc/init.d/eucalyptus-cloud ];
	then
		 etc/init.d/eucalyptus-cloud stop
	fi
	if [ -x etc/init.d/eucalyptus-cc ]; 
	then
		 etc/init.d/eucalyptus-cc stop
	fi
	if [ -x etc/init.d/eucalyptus-nc ]; 
	then
		 etc/init.d/eucalyptus-nc stop
	fi

	# save a backup of important data
	DATESTR=`date +%s`
	echo /root/eucalyptus.backup.$DATESTR > /tmp/eucaback.dir
	mkdir -p /root/eucalyptus.backup.$DATESTR
	cd /root/eucalyptus.backup.$DATESTR
	EUCABACKUPS=""
	for i in $EUCADIR/var/lib/eucalyptus/keys/ $EUCADIR/var/lib/eucalyptus/db/ $EUCADIR/etc/eucalyptus/eucalyptus.conf $EUCADIR/etc/eucalyptus/eucalyptus-version $EUCADIR/usr/share/eucalyptus/
	do
	    if [ -e $i ]; then
		EUCABACKUPS="$EUCABACKUPS $i"
	    fi
	done
	tar cf -  $EUCABACKUPS 2>/dev/null | tar xf - 2>/dev/null
	cd $EUCADIR
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
%if %is_fedora
	adduser -U eucalyptus 
%endif
fi

if [ "$1" = "1" ]; 
then
	# let's configure eucalyptus
	/usr/sbin/euca_conf -d / --instances /usr/local/eucalyptus/ -hypervisor %{euca_hypervisor} -bridge %{euca_bridge}
fi
if [ "$1" = "2" ];
then
	/usr/sbin/euca_conf -d / --instances /usr/local/eucalyptus/ -hypervisor %{euca_hypervisor} -bridge %{euca_bridge}
	if [ -f /tmp/eucaback.dir ]; then
	    BACKDIR=`cat /tmp/eucaback.dir`
	    if [ -d "$BACKDIR" ]; then
		/usr/share/eucalyptus/euca_upgrade --old $BACKDIR --new / --conf --keys --sameversion >/dev/null 2>&1
		/usr/sbin/euca_conf -setup
	    fi
	fi
fi

# final setup and set the new user
/usr/sbin/euca_conf -setup -user eucalyptus

%post common-java
if [ "$1" = "2" ];
then
    if [ -f /tmp/eucaback.dir ]; then
	BACKDIR=`cat /tmp/eucaback.dir`
	if [ -d "$BACKDIR" ]; then
	    /usr/sbin/euca_conf -setup
	    if [ -f "$BACKDIR/etc/eucalyptus/eucalyptus-version" -a -f "/etc/eucalyptus/eucalyptus-version" ]; then
		export OLDVERSION=`cat $BACKDIR/etc/eucalyptus/eucalyptus-version`
		export NEWVERSION=`cat /etc/eucalyptus/eucalyptus-version`
		if [ "$OLDVERSION" != "$NEWVERSION" ]; then
		    rm -f /usr/share/eucalyptus/eucalyptus-*$OLDVERSION*.jar
		    rm -f /usr/share/eucalyptus/groovy-1.6.3.jar
		    rm -f /usr/share/eucalyptus/asm2-2.2.3.jar
		fi
	    fi
	    /usr/share/eucalyptus/euca_upgrade --old $BACKDIR --new / --db --sameversion >/dev/null 2>&1
	    /usr/sbin/euca_conf -setup
	fi
    fi
fi
chkconfig --add eucalyptus-cloud
/usr/sbin/euca_conf -setup -user eucalyptus

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
# upgrade
#if [ "$1" = "2" ];
#then
#	if [ -f /tmp/eucaback.dir ]; then
#	    BACKDIR=`cat /tmp/eucaback.dir`
#	    if [ -d "$BACKDIR" ]; then
#		/usr/share/eucalyptus/euca_upgrade --old $BACKDIR --new / --db
#		/usr/sbin/euca_conf -setup
#	    fi
#	fi

#	cd /
#	[ -e /opt/eucalyptus/etc/eucalyptus/eucalyptus-version ] && cd /opt/eucalyptus
#	if [ -e var/lib/eucalyptus/db/eucalyptus_volumes.properties ];
#	then
#		# if groovy was installed on the same shell the
#		# environment can be wrong: we need to souce groovy env
#		if [ -e /etc/profile.d/groovy.sh ];
#		then
#			. /etc/profile.d/groovy.sh
#		fi
#		/usr/share/eucalyptus/euca_upgrade --old /opt/eucalyptus --new / --db
#	fi
#fi

%post walrus
/usr/sbin/euca_conf --enable walrus

%post sc
/usr/bin/killall -9 vblade
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
#if [ "$1" = "2" ];
#then
#	if [ -f /tmp/eucaback.dir ]; then
#	    BACKDIR=`cat /tmp/eucaback.dir`
#	    if [ -d "$BACKDIR" ]; then
#		echo /usr/share/eucalyptus/euca_upgrade --old $BACKDIR --new / --conf --keys
#	    fi
#	fi
#fi

%post nc
chkconfig --add eucalyptus-nc
%if %is_fedora
	usermod -G kvm eucalyptus
%endif
%if %is_centos
if [ -e /etc/sysconfig/system-config-securitylevel ];
then
	if ! grep 8775:tcp /etc/sysconfig/system-config-securitylevel > /dev/null ; 
	then
		echo "--port=8775:tcp" >> /etc/sysconfig/system-config-securitylevel
	fi
fi
%endif
#%if %is_suse
#if [ -e /etc/PolicyKit/PolicyKit.conf ]; 
#then
#	if ! grep eucalyptus /etc/PolicyKit/PolicyKit.conf > /dev/null ;
#	then
#		sed -i '/<config version/ a <match action="org.libvirt.unix.manage">\n   <match user="eucalyptus">\n      <return result="yes"/>\n   </match>\n</match>' /etc/PolicyKit/PolicyKit.conf
#	fi
#fi
#%endif
#if [ "$1" = "2" ];
#then
#    if [ -f /tmp/eucaback.dir ]; then
#	BACKDIR=`cat /tmp/eucaback.dir`
#	if [ -d "$BACKDIR" ]; then
#	    echo /usr/share/eucalyptus/euca_upgrade --old $BACKDIR --new / --conf --keys
#	fi
#    fi
#fi


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
	if [ -e /etc/init.d/eucalyptus-cloud -a /etc/eucalyptus/eucalyptus.conf ];
	then 
		/etc/init.d/eucalyptus-cloud restart || true
	fi
fi


%preun walrus
if [ "$1" = "0" ];
then
	[ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable walrus
	if [ -e /etc/init.d/eucalyptus-cloud ];
	then 
		/etc/init.d/eucalyptus-cloud restart || true
	fi
fi

%preun sc
if [ "$1" = "0" ];
then
	[ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable sc
	if [ -e /etc/init.d/eucalyptus-cloud -a /etc/eucalyptus/eucalyptus.conf ];
	then 
		/etc/init.d/eucalyptus-cloud restart || true
	fi
fi

%preun common-java
if [ "$1" = "0" ];
then
    if [ -f /etc/eucalyptus/eucalyptus.conf ]; then
	/etc/init.d/eucalyptus-cloud stop
    fi
    chkconfig --del eucalyptus-cloud
    rm -f /var/lib/eucalyptus/services
fi

%preun cc
if [ "$1" = "0" ];
then
    if [ -f /etc/eucalyptus/eucalyptus.conf ]; then
	/etc/init.d/eucalyptus-cc cleanstop
    fi
    chkconfig --del eucalyptus-cc
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
    if [ -f /etc/eucalyptus/eucalyptus.conf ]; then
	/etc/init.d/eucalyptus-nc stop
    fi
    chkconfig --del eucalyptus-nc
%if %is_centos
    if [ -e /etc/sysconfig/system-config-securitylevel ];
    then
	sed -i '/^--port=8775/ d' /etc/sysconfig/system-config-securitylevel
    fi
%endif
fi

%changelog
* Fri Feb 12 2010 Eucalyptus Systems <support@open.eucalyptus.com>
- 1.6.2
- Thanks to Garrett Holmstrom and cloud@lists.fedoraproject.org for
  helping with the RPM pacakging
- Re-worked upgrade path for RPM install
- Re-worked spec file to honor DESTDIR

* Thu Nov 5 2009 Eucalyptus Systems <support@open.eucalyptus.com>
- 1.6.1
- install in / instead of /opt/eucalyptus

* Mon Jun 15 2009 eucalyptus systems <support@open.eucalyptus.com>
- 1.5.2

* Thu Apr 16 2009 mayhem group <support@open.eucalyptus.com>
- 1.5.1
- Elastic Block Store (EBS) support (volumes & snapshots) 
- Better Java installation checking
- New command-line administration: euca_conf -addcluster ... -addnode ...
- Non-root user deployment of Eucalyptus
- Binary packages for more distributions (Ubuntu et al) 
- Cloud registration with Rightscale (from admin's 'Credentials' tab)
- New configuration options for Walrus
- Better screening of usernames
- Fixed account confirmation glitches 

* Mon Jan  5 2009 mayhem group <support@open.eucalyptus.com>
- 1.4
- Added new networking subsystem that no longer depends on VDE
- Added support for elastic IP assignment and security using the 'MANAGED' 
  networking mode
- Added Walrus: a Amazon S3 interface compatible storage manager. Walrus
  handles storage of user data as well as filesystem images, kernels, and
  ramdisks.
- Support for new operations: reboot instance and get console output.
- Revamped logging throughout, with five levels a la log4j.

* Thu Aug 28 2008 mayhem group <support@open.eucalyptus.com>
- 1.3

* Mon Jul 28 2008 mayhem group <support@open.eucalyptus.com>
- 1.2
- Removed cloud, cluster controller and node controller and created their
  own packages.
- Added the possibility of installing Eucalyptus from RPMs (without ROCKS).
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
