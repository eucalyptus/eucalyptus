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
%global euca_hypervisor xen
%global euca_curl    libcurl4
%global euca_libcurl libcurl-devel
%global euca_bridge  br0
%global euca_java    java-sdk >= 1.6.0
%global euca_iscsi_client open-iscsi
%global euca_iscsi_server tgt
%global euca_build_req vlan
%global euca_fuse libfuse2
%global euca_which util-linux
%endif
%if %is_centos
%global euca_hypervisor xen
%global euca_bridge  xenbr0
%global euca_java    java-sdk >= 1.6.0
%global euca_iscsi_client iscsi-initiator-utils
%global euca_iscsi_server scsi-target-utils
%global euca_fuse fuse-libs
%global euca_which which
%endif
%if %is_fedora
%global euca_hypervisor kvm
%global euca_bridge  br0
%global euca_java    java-devel >= 1:1.6.0
%global euca_iscsi_client iscsi-initiator-utils
%global euca_iscsi_server scsi-target-utils
%global euca_fuse fuse-libs
%global euca_which which
%endif

Summary:       Elastic Utility Computing Architecture
Name:          eucalyptus-eee
Version:       2.1.0
Release:       0
License:       Eucalyptus EEE Software License
URL:           http://www.eucalyptus.com
Group:         Applications/System

# EEE fails to build with el5's gcj-based ant 1.6
BuildRequires: ant >= 1.7
BuildRequires: ant-nodeps >= 1.7
BuildRequires: axis2
BuildRequires: axis2c-devel >= 1.6.0
BuildRequires: libvirt-devel >= 0.6
BuildRequires: rampartc-devel >= 1.3.0
BuildRequires: swig
%if %{is_suse}
BuildRequires: xen-tools
%endif
# The bytecode encryption we use for EEE doesn't work with OpenJDK
BuildRequires: jdk
# The VMware code requires FUSE libs to link correstly
BuildRequires: %{euca_fuse}
BuildRequires: %{euca_iscsi_client}
BuildRequires: %{euca_libcurl}
Requires:      %{euca_build_req}
Requires:      %{euca_which}
Requires:      perl(Crypt::OpenSSL::RSA)
Requires:      perl(Crypt::OpenSSL::Random)
Requires:      sudo

%if %{is_centos}
Requires:      python26
Requires:      python26-boto
%else
Requires:      python
Requires:      python-boto >= 1.9b
%endif

BuildRoot:     %(mktemp -ud %{_tmppath}/%{name}-%{version}-%{release}-XXXXXX)

Source0:       %{name}-%{version}.tar.gz
# A version of WSDL2C.sh that respects standard classpaths
Source1:       euca-WSDL2C.sh
# The VMware VDDK may be non-redistributable.  We don't include it just in case.
Source2:       vmware-vix-disklib-distrib.tgz

%description
EUCALYPTUS is a service overlay that implements elastic computing
using existing resources. The goal of EUCALYPTUS is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon's EC2.

This package contains the common parts: you will need to install
either eucalyptus-cloud, eucalyptus-walrus, eucalyptus-sc,
eucalyptus-vmware-broker, eucalyptus-cc or eucalyptus-nc (or all of
them).

%package common-java
Summary:      Elastic Utility Computing Architecture - ws java stack
Requires:     %{name} = %{version}-%{release}
Requires:     lvm2
Requires:     jre
Group:        Applications/System

%description common-java
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the java WS stack.

%package walrus
Summary:      Elastic Utility Computing Architecture - walrus
Requires:     %{name}-common-java = %{version}-%{release}
Requires:     lvm2
Group:        Applications/System

%description walrus
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains storage component for your cloud: images and buckets
are handled by walrus. Tipically this package is installed alongside the
cloud controller.

%package sc
Summary:      Elastic Utility Computing Architecture - storage controller
Requires:     %{name}-common-java = %{version}-%{release}
Requires:     lvm2
Requires:     vblade
Requires:     %{euca_iscsi_server}
Group:        Applications/System

%description sc
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the storage controller part of eucalyptus which
handles the elastic blocks for a given cluster. Tipically you install it
alongside the cluster-controller.

%package cloud
Summary:      Elastic Utility Computing Architecture - cloud controller
Requires:     %{name}-common-java = %{version}-%{release}
Requires:     euca2ools-eee
Requires:     lvm2
%if %{is_centos}
Requires:     python26-boto
%else
Requires:     python-boto >= 1.9b
%endif
Group:        Applications/System

%description cloud
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cloud controller part of eucalyptus: the cloud
controller needs to be reachable by both the cluster controller and from
the cloud clients.

%package cc
Summary:      Elastic Utility Computing Architecture - cluster controller
Requires:     %{name}    = %{version}-%{release}
Requires:     %{name}-gl = %{version}-%{release}
Requires:     bridge-utils
##Requires:     euca-axis2c >= 1.6.0
##Requires:     euca-rampartc >= 1.3.0
Requires:     iptables
Requires:     %{euca_dhcp}
Requires:     %{euca_httpd}
Requires:     vtun
Group:        Applications/System

%description cc
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the cluster controller part of eucalyptus: it
handles multiple node controllers.

%package nc
Summary:      Elastic Utility Computing Architecture - node controller
Requires:     %{name}    = %{version}-%{release}
Requires:     %{name}-gl = %{version}-%{release}
Requires:     euca2ools-eee
##Requires:     euca-axis2c >= 1.6.0
##Requires:     euca-rampartc >= 1.3.0
Requires:     bridge-utils
Requires:     %{euca_curl}
Requires:     %{euca_httpd}
Requires:     %{euca_hypervisor}
Requires:     %{euca_iscsi_client}
Group:        Applications/System

%description nc
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the node controller part of eucalyptus: this is the
components that handles the instances.

%package gl
Summary:      Elastic Utility Computing Architecture - log service
Requires:     %{name} = %{version}
##Requires:     euca-axis2c >= 1.6.0
##Requires:     euca-rampartc >= 1.3.0
Requires:     %{euca_httpd}
Group:        Applications/System

%description gl
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the internal log service of eucalyptus.

%package broker
Summary:      Elastic Utility Computing Architecture - vmware broker
Requires:     %{name}-common-java = %{version}-%{release}
Requires:     %{name}-cc
Requires:     %{euca_httpd}
## XXX Where are the Requires and Provides for the VMware broker?
AutoReq:      no
Group:        Applications/System

%description broker
EUCALYPTUS is a service overlay that implements elastic
computing using existing resources. The goal of EUCALYPTUS is to allow
sites with existing clusters and server infrastructure to co-host an
elastic computing service that is interface-compatible with Amazon's EC2.

This package contains the broker needed to let EUCALYPTUS control a
VMware installation.

%prep
%setup -q
%setup -q -T -D -b 2

%build
# Export the classpath here so WSDL2C can find its stuff
export DESTDIR=$RPM_BUILD_ROOT
# Oracle JDK links to Java without using alternatives
export JAVA_HOME=/usr/java/latest
./configure --with-axis2=%{_datadir}/axis2-* --with-axis2c=%{_libdir}/axis2c --with-wsdl2c-sh=%{S:1} --enable-debug --prefix=/ --with-vddk=$RPM_BUILD_DIR/vmware-vix-disklib-distrib

pushd clc
# The CLC's build XML file looks for jar files in clc/lib, so symlink the
# system's copies of important jar files to that location.
make deps
popd

# Write builds logs to files so we can triage build failures.
# mock logs stdout and stderr, so this hackery will eventually go away.
(make 2>&1 1>&3 | tee err.log) 3>&1 1>&2 | tee out.log

%if %is_centos
for x in `/bin/ls clc/tools/src/euca-*`; do
    sed --in-place 's:#!/usr/bin/env python:#!%{_bindir}/python2.6:' $x
done
%endif

%install
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}
make install DESTDIR=$RPM_BUILD_ROOT

%clean
[ ${RPM_BUILD_ROOT} != "/" ] && rm -rf ${RPM_BUILD_ROOT}

%files
%doc LICENSE INSTALL README CHANGELOG
/etc/bash_completion.d/euca_conf
/etc/eucalyptus/eucalyptus.conf
/etc/eucalyptus/eucalyptus-version
/etc/eucalyptus/httpd.conf
/usr/lib/eucalyptus/euca_mountwrap
/usr/lib/eucalyptus/euca_rootwrap
/usr/sbin/euca-add-group-membership
/usr/sbin/euca-add-user
/usr/sbin/euca-add-user-group
/usr/sbin/euca_admin
/usr/sbin/euca_conf
/usr/sbin/euca-convert-volumes
/usr/sbin/euca-delete-user
/usr/sbin/euca-delete-user-group
/usr/sbin/euca-deregister-arbitrator
/usr/sbin/euca-deregister-cluster
/usr/sbin/euca-deregister-storage-controller
/usr/sbin/euca-deregister-walrus
/usr/sbin/euca-describe-arbitrators
/usr/sbin/euca-describe-clusters
/usr/sbin/euca-describe-components
/usr/sbin/euca-describe-nodes
/usr/sbin/euca-describe-properties
/usr/sbin/euca-describe-storage-controllers
/usr/sbin/euca-describe-user-groups
/usr/sbin/euca-describe-users
/usr/sbin/euca-describe-walruses
/usr/sbin/euca-get-credentials
/usr/sbin/euca-grant-zone-permission
/usr/sbin/euca_killall
/usr/sbin/euca-modify-cluster
/usr/sbin/euca-modify-property
/usr/sbin/euca-modify-storage-controller
/usr/sbin/euca-modify-walrus
/usr/sbin/euca-register-arbitrator
/usr/sbin/euca-register-cluster
/usr/sbin/euca-register-storage-controller
/usr/sbin/euca-register-walrus
/usr/sbin/euca-remove-group-membership
/usr/sbin/euca-revoke-zone-permission
/usr/sbin/euca_sync_key
/usr/share/eucalyptus/add_key.pl
/usr/share/eucalyptus/connect_iscsitarget.pl
/usr/share/eucalyptus/disconnect_iscsitarget.pl
/usr/share/eucalyptus/doc
/usr/share/eucalyptus/euca_ipt
/usr/share/eucalyptus/euca_upgrade
/usr/share/eucalyptus/floppy
/usr/share/eucalyptus/get_iscsitarget.pl
/usr/share/eucalyptus/populate_arp.pl
/usr/share/eucalyptus/udev/55-openiscsi.rules
/usr/share/eucalyptus/udev/iscsidev-centos.sh
/usr/share/eucalyptus/udev/iscsidev-opensuse.sh
/usr/share/eucalyptus/udev/iscsidev-ubuntu.sh
/usr/share/eucalyptus/udev/README
/var/lib/eucalyptus/keys
/var/log/eucalyptus
/var/run/eucalyptus


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
/usr/share/eucalyptus/connect_iscsitarget_sc.pl
/usr/share/eucalyptus/disconnect_iscsitarget_sc.pl

%files cc
%{_libdir}/axis2c/services/EucalyptusCC
/etc/init.d/eucalyptus-cc
/etc/eucalyptus/vtunall.conf.template
/usr/share/eucalyptus/dynserv.pl

%files nc
/usr/share/eucalyptus/detach.pl
/usr/share/eucalyptus/gen_kvm_libvirt_xml
/usr/share/eucalyptus/gen_libvirt_xml
/usr/share/eucalyptus/getstats.pl
/usr/share/eucalyptus/get_sys_info
/usr/share/eucalyptus/get_xen_info
/usr/share/eucalyptus/partition2disk
/usr/sbin/eucanetd
/usr/sbin/euca_test_nc
%{_libdir}/axis2c/services/EucalyptusNC
/etc/init.d/eucalyptus-nc

%files gl
%{_libdir}/axis2c/services/EucalyptusGL

%files broker
/usr/share/eucalyptus/euca_vmware
/usr/lib/eucalyptus/euca_imager
/usr/lib/eucalyptus/_euca_imager

%pre
if [ "$1" = "2" ];
then
    # let's see where we installed
    EUCADIRS="/"
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
         etc/init.d/eucalyptus-cc cleanstop
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

    if [ -e etc/eucalyptus/eucalyptus.conf ]; then
        sed -i "s/DISABLE_EBS=.*/#DISABLE_EBS=\"N\"/" etc/eucalyptus/eucalyptus.conf
    fi
    sed -i "s/Defaults.*requiretty/#Defaults requiretty/" /etc/sudoers

    tar cf -  $EUCABACKUPS 2>/dev/null | tar xf - 2>/dev/null
    cd $EUCADIR
fi

%post
# set up udev iscsi config
mkdir -p /etc/udev/rules.d/
cp /usr/share/eucalyptus/udev/55-openiscsi.rules /etc/udev/rules.d/
mkdir -p /etc/udev/scripts/
%if %is_suse
        cp /usr/share/eucalyptus/udev/iscsidev-opensuse.sh /etc/udev/scripts/iscsidev.sh
    udevadm control --reload-rules
%endif
%if %is_centos
        cp /usr/share/eucalyptus/udev/iscsidev-centos.sh /etc/udev/scripts/iscsidev.sh
        udevcontrol reload_rules
        sed -i "s/node\.startup.*/node\.startup\ = manual/" /etc/iscsi/iscsid.conf
        sed -i "s/Defaults.*requiretty/#Defaults requiretty/" /etc/sudoers
%endif
cat <<EOF >> /etc/sudoers
eucalyptus ALL=NOPASSWD: /usr/share/eucalyptus/connect_iscsitarget_sc.pl
eucalyptus ALL=NOPASSWD: /usr/share/eucalyptus/disconnect_iscsitarget_sc.pl
eucalyptus ALL=NOPASSWD: /usr/share/eucalyptus/connect_iscsitarget.pl
eucalyptus ALL=NOPASSWD: /usr/share/eucalyptus/disconnect_iscsitarget.pl
eucalyptus ALL=NOPASSWD: /usr/share/eucalyptus/get_iscsitarget.pl
eucalyptus ALL=NOPASSWD: /usr/sbin/tgtadm
EOF
chmod +x /etc/udev/scripts/iscsidev.sh

# set java home to location of SunJDK for EEE
sed -i 's#.*CLOUD_OPTS=.*#CLOUD_OPTS="--java-home=/usr/java/latest"#' /etc/eucalyptus/eucalyptus.conf

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
    adduser -U --system eucalyptus
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
#        /usr/share/eucalyptus/euca_upgrade --old $BACKDIR --new / --conf --keys
        echo "**NOTICE** If you are upgrading to EEE, you will need to perform the upgrade process manually.  Your previous Eucalyptus data is backed up in $BACKDIR."
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
#        /usr/share/eucalyptus/euca_upgrade --old $BACKDIR --new / --db
        echo "**NOTICE** If you are upgrading to EEE, you will need to perform the upgrade process manually.  Your previous Eucalyptus data is backed up in $BACKDIR."
        /usr/sbin/euca_conf -setup
    fi
    fi
fi
chkconfig --add eucalyptus-cloud
/usr/sbin/euca_conf -setup

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
/usr/bin/killall -9 vblade >/dev/null 2>&1
if [ -e /etc/init.d/tgtd ]; then
    chkconfig --add tgtd
    /etc/init.d/tgtd start
fi
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
if [ -e /etc/init.d/libvirtd ]; then
    chkconfig --add libvirtd
    /etc/init.d/libvirtd restart
fi
chkconfig --add eucalyptus-nc
sed -i "s/.*NC_BUNDLE_UPLOAD_PATH=.*/NC_BUNDLE_UPLOAD_PATH=\"\/usr\/bin\/euca-bundle-upload\"/" /etc/eucalyptus/eucalyptus.conf
sed -i "s/.*NC_CHECK_BUCKET_PATH=.*/NC_CHECK_BUCKET_PATH=\"\/usr\/bin\/euca-check-bucket\"/" /etc/eucalyptus/eucalyptus.conf
sed -i "s/.*NC_DELETE_BUNDLE_PATH=.*/NC_DELETE_BUNDLE_PATH=\"\/usr\/bin\/euca-delete-bundle\"/" /etc/eucalyptus/eucalyptus.conf
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
%if %is_suse
if [ -e /etc/PolicyKit/PolicyKit.conf ];
then
    if ! grep eucalyptus /etc/PolicyKit/PolicyKit.conf > /dev/null ;
    then
        sed -i '/<config version/ a <match action="org.libvirt.unix.manage">\n   <match user="eucalyptus">\n      <return result="yes"/>\n   </match>\n</match>' /etc/PolicyKit/PolicyKit.conf
    fi
fi
%endif

%post broker
/usr/sbin/euca_conf --enable vmwarebroker
if [ -e /etc/init.d/eucalyptus-cc -a -e /etc/eucalyptus/eucalyptus.conf ]; then
    sed -i "s/NC_SERVICE=.*/NC_SERVICE=\"\/services\/VMwareBroker\"/" /etc/eucalyptus/eucalyptus.conf
    sed -i "s/NC_PORT=.*/NC_PORT=\"8773\"/" /etc/eucalyptus/eucalyptus.conf
    echo DISABLE_ISCSI=\"N\" >> /etc/eucalyptus/eucalyptus.conf
    /etc/init.d/eucalyptus-cc cleanrestart
fi

%postun
# in case of removal let's try to clean up the best we can
if [ "$1" = "0" ];
then
    rm -rf /var/log/eucalyptus
    rm -rf /etc/eucalyptus/http-*
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
    if [ -e /etc/init.d/eucalyptus-cloud -a -e /etc/eucalyptus/eucalyptus.conf ];
    then
        /etc/init.d/eucalyptus-cloud restart || true
    fi
fi


%preun walrus
if [ "$1" = "0" ];
then
    [ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable walrus
    if [ -e /etc/init.d/eucalyptus-cloud -a -e /etc/eucalyptus/eucalyptus.conf ];
    then
        /etc/init.d/eucalyptus-cloud restart || true
    fi
fi

%preun sc
if [ "$1" = "0" ];
then
    [ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable sc
    if [ -e /etc/init.d/eucalyptus-cloud -a -e /etc/eucalyptus/eucalyptus.conf ];
    then
        /etc/init.d/eucalyptus-cloud restart || true
    fi
fi

%preun broker
if [ "$1" = "0" ];
then
    [ -x /usr/sbin/euca_conf ] && /usr/sbin/euca_conf --disable vmwarebroker
    if [ -e /etc/init.d/eucalyptus-cloud -a -e /etc/eucalyptus/eucalyptus.conf ];
    then
        /etc/init.d/eucalyptus-cloud restart || true
    fi
    if [ -e /etc/init.d/eucalyptus-cc -a -e /etc/eucalyptus/eucalyptus.conf ]; then
        sed -i "s/NC_SERVICE=.*/NC_SERVICE=\"\/axis2\/services\/EucalyptusNC\"/" /etc/eucalyptus/eucalyptus.conf
        sed -i "s/NC_PORT=.*/NC_PORT=\"8775\"/" /etc/eucalyptus/eucalyptus.conf
        /etc/init.d/eucalyptus-cc cleanrestart
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
* Wed Jan 12 2011 Eucalyptus Release Engineering <support@eucalyptus.com> - 2.1.0-1
- Version 2.1 of Eucalyptus Enterprise Cloud

* Tue Jun  1 2010 Eucalyptus Release Engineering <support@eucalyptus.com> - 2.0.0-1
- Version 2.0 of Eucalyptus Enterprise Cloud
  - Windows VM Support
  - User/Group Management
  - SAN Integration
  - VMWare Hypervisor Support
