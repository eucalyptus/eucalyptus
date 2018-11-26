# Copyright (c) 2009-2017 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

%{!?python_sitelib: %global python_sitelib %(%{__python} -c "from distutils.sysconfig import get_python_lib; print get_python_lib()")}

# Meant to be used with ''rpmbuild -bc''
# You must also install coverity-analysis.
%bcond_with coverity

%{!?version: %define version 5.0}

Summary:       Eucalyptus cloud platform
Name:          eucalyptus
Version:       %{version}
Release:       0%{?build_id:.%build_id}%{?dist}
License:       GPLv3
URL:           https://eucalyptus.cloud/

BuildRequires: ant >= 1.7
BuildRequires: ant-apache-regexp
BuildRequires: apache-ivy
BuildRequires: axis2-adb-codegen
BuildRequires: axis2-codegen
BuildRequires: axis2c-devel >= 1.6.0
BuildRequires: curl-devel
BuildRequires: eucalyptus-java-deps
BuildRequires: gengetopt
BuildRequires: java-1.8.0-openjdk-devel >= 1:1.8.0
BuildRequires: jpackage-utils
BuildRequires: json-c-devel
BuildRequires: libuuid-devel
BuildRequires: libvirt-devel >= 2.0.0
BuildRequires: libxml2-devel
BuildRequires: libxslt-devel
BuildRequires: m2crypto
BuildRequires: openssl-devel
BuildRequires: python-devel
BuildRequires: python-setuptools
BuildRequires: rampartc-devel >= 1.3.0
BuildRequires: swig
BuildRequires: systemd
BuildRequires: xalan-j2
BuildRequires: xalan-j2-xsltc
BuildRequires: /usr/bin/awk

Requires(pre): shadow-utils

Requires:      eucalyptus-selinux

Source0:       %{tarball_basedir}.tar.xz

%description
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains bits that are shared by all Eucalyptus components
and is not particularly useful on its own -- to get a usable cloud you
will need to install Eucalyptus services as well.


%package axis2c-common
Summary:      Eucalyptus cloud platform - Axis2/C shared components

Requires:     %{name} = %{version}-%{release}
Requires:     eucalyptus-selinux
Requires:     httpd
Requires:     perl(Digest::MD5)
Requires:     perl(MIME::Base64)

%description axis2c-common
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains shared components used by all eucalyptus services
that are based on Axis2/C.


%package blockdev-utils
Summary:      Eucalyptus cloud platform - shared block device utilities

Requires:     %{name} = %{version}-%{release}
Requires:     eucalyptus-selinux
Requires:     libselinux-python
Requires:     perl(Crypt::OpenSSL::RSA)
Requires:     perl(Crypt::OpenSSL::Random)
Requires:     perl(MIME::Base64)
Requires:     /usr/bin/which

%description blockdev-utils
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains shared components used by all eucalyptus services
that connect to iSCSI targets.


%package common-java
Summary:      Eucalyptus cloud platform - ws java stack
Requires:     %{name} = %{version}-%{release}
Requires:     %{name}-common-java-libs = %{version}-%{release}
Requires:     eucalyptus-selinux
Requires:     lvm2
Requires:     /usr/bin/which
%{?systemd_requires}

Provides:     %{name}-java-services = %{version}-%{release}

%description common-java
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the common-java files.


%package common-java-libs
Summary:      Eucalyptus cloud platform - ws java stack libs

Requires:     eucalyptus-java-deps >= 4.4
Requires:     eucalyptus-selinux
Requires:     jpackage-utils
Requires:     java-1.8.0-openjdk >= 1:1.8.0

Provides:     %{name}-java-libs = %{version}-%{release}

%description common-java-libs
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the java WS stack.


%package walrus
Summary:      Eucalyptus cloud platform - walrus

Requires:     %{name}             = %{version}-%{release}
Requires:     %{name}-common-java = %{version}-%{release}
Requires:     eucalyptus-selinux
Requires:     lvm2

%description walrus
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains storage component for your cloud: images and buckets
are handled by walrus. Typically this package is installed alongside the
cloud controller.


%package sc
Summary:      Eucalyptus cloud platform - storage controller

Requires:     %{name} = %{version}-%{release}
Requires:     %{name}-blockdev-utils = %{version}-%{release}
Requires:     %{name}-common-java = %{version}-%{release}
Requires:     device-mapper-multipath
Requires:     eucalyptus-selinux
Requires:     iscsi-initiator-utils
Requires:     librados2%{?_isa}
Requires:     librbd1%{?_isa}
Requires:     lvm2
Requires:     scsi-target-utils
Requires:     /usr/bin/rbd

Provides:     eucalyptus-storage = %{version}-%{release}

%description sc
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the storage controller part of eucalyptus, which
handles the elastic blocks for a given cluster. Typically you install it
alongside the cluster controller.


%package cloud
Summary:      Eucalyptus cloud platform - cloud controller

Requires:     %{name}                     = %{version}-%{release}
Requires:     %{name}-common-java%{?_isa} = %{version}-%{release}
# Change this to Recommends in RHEL 8
Requires:     %{name}-admin-tools         = %{version}-%{release}
Requires:     eucalyptus-selinux
Requires:     euca2ools >= 2.0
Requires:     eucanetd = %{version}-%{release}
Requires:     libselinux-python
Requires:     lvm2
Requires:     perl(Getopt::Long)
Requires:     postgresql
Requires:     postgresql-server
Requires:     rsync

%description cloud
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the cloud controller part of eucalyptus. The cloud
controller needs to be reachable by both the cluster controller and from
the cloud clients.


%package cc
Summary:      Eucalyptus cloud platform - cluster controller

Requires:     %{name} = %{version}-%{release}
Requires:     %{name}-common-java = %{version}-%{release}
Requires:     eucalyptus-selinux
Requires:     rsync
Requires:     /usr/bin/which

Provides:     eucalyptus-cluster = %{version}-%{release}

%description cc
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the cluster controller part of eucalyptus. It
handles a group of node controllers.


%package cc-native
Summary:      Eucalyptus cloud platform - native cluster controller

Requires:     %{name} = %{version}-%{release}
Requires:     %{name}-axis2c-common = %{version}-%{release}
Requires:     %{name}-cc = %{version}-%{release}
Requires:     bridge-utils
Requires:     dhcp >= 4.1.1-33.P1
Requires:     eucalyptus-selinux
Requires:     httpd
Requires:     iproute
Requires:     iptables
Requires:     iputils
Requires:     libselinux-python
Requires:     rsync
Requires:     /usr/bin/which
%{?systemd_requires}

Provides:     eucalyptus-cluster = %{version}-%{release}

%description cc-native
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the native cluster controller service. It
handles a group of node controllers as an alternative to the Java
cluster controller.


%package nc
Summary:      Eucalyptus cloud platform - node controller

Requires:     %{name} = %{version}-%{release}
Requires:     %{name}-axis2c-common = %{version}-%{release}
Requires:     %{name}-blockdev-utils = %{version}-%{release}
Requires:     %{name}-imaging-toolkit = %{version}-%{release}
Requires:     bridge-utils
Requires:     device-mapper
Requires:     device-mapper-multipath
Requires:     euca2ools >= 3.2
Requires:     eucalyptus-selinux > 0.2
Requires:     eucanetd = %{version}-%{release}
Requires:     httpd
Requires:     iscsi-initiator-utils
# Ceph support requires librados2, librbd1, and *also* qemu-kvm-rhev.
Requires:     librados2%{?_isa}
Requires:     librbd1%{?_isa}
Requires:     libvirt >= 2.0.0
Requires:     libvirt-python
Requires:     perl(Sys::Virt)
Requires:     perl(Time::HiRes)
Requires:     perl(XML::Simple)
Requires:     qemu-kvm
Requires:     qemu-system-x86
# The next six come from storage/diskutil.c, which shells out to lots of stuff.
Requires:     coreutils
Requires:     curl
Requires:     e2fsprogs
Requires:     file
Requires:     parted
Requires:     vconfig
Requires:     util-linux
Requires:     /usr/bin/which
%{?systemd_requires}

Provides:     eucalyptus-node = %{version}-%{release}

%description nc
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the node controller part of eucalyptus. This
component handles instances.


%package admin-tools
Summary:      Eucalyptus cloud platform - admin CLI tools
# A patched version of python's gzip is included, so we add the Python license
License:      BSD and Python

Requires:     euca2ools >= 3.2
Requires:     eucalyptus-selinux
Requires:     python-boto >= 2.1
Requires:     python-prettytable
Requires:     python-requestbuilder >= 0.4
Requires:     python-requests
Requires:     python-six

BuildArch:    noarch

%description admin-tools
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains command line tools necessary for managing a
Eucalyptus cloud.


%package -n eucanetd
Summary:        Eucalyptus cloud platform - edge networking daemon
License:        GPLv3

Requires:       dhcp >= 4.1.1-33.P1
Requires:       ebtables
Requires:       eucalyptus-selinux > 0.2
Requires:       ipset
Requires:       iptables
# nginx 1.9.13 added perl as a loadable module (EUCA-12734)
Requires:       nginx >= 1.9.13
Requires:       /usr/bin/which
%{?systemd_requires}

%description -n eucanetd
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains the daemon that controls the edge networking mode.
To use edge networking mode, all node controllers must have this package
installed.


%package imaging-toolkit
Summary:      Eucalyptus cloud platform - image manipulation tookit
License:      ASL 2.0

Requires:     %{name} = %{version}-%{release}
# This includes both things under tools/imaging and storage.
Requires:     euca2ools >= 3.1
Requires:     eucalyptus-selinux
Requires:     pv
Requires:     python-lxml
Requires:     python-requests
# The next seven come from storage/diskutil.c, which shells
# out to lots of stuff
Requires:     coreutils
Requires:     e2fsprogs
Requires:     file
Requires:     parted
Requires:     util-linux

%description imaging-toolkit
Eucalyptus is a service overlay that implements elastic computing
using existing resources. The goal of Eucalyptus is to allow sites
with existing clusters and server infrastructure to co-host an elastic
computing service that is interface-compatible with Amazon AWS.

This package contains a toolkit used internally by Eucalyptus to download
and upload virtual machine images and to convert them between formats.


%prep
%setup -q -n %{tarball_basedir}

# Filter unwanted perl provides
cat << \EOF > %{name}-prov
#!/bin/sh
%{__perl_provides} $* |\
sed -e '/perl(disconnect_iscsitarget_main.pl)/d' \
    -e '/perl(connect_iscsitarget_main.pl)/d' \
    -e '/perl(iscsitarget_common.pl)/d'
EOF

%global __perl_provides %{_builddir}/%{tarball_basedir}/%{name}-prov
chmod +x %{__perl_provides}

# Filter unwanted perl requires
cat << \EOF > %{name}-req
#!/bin/sh
%{__perl_requires} $* |\
sed -e '/perl(disconnect_iscsitarget_main.pl)/d' \
    -e '/perl(connect_iscsitarget_main.pl)/d' \
    -e '/perl(iscsitarget_common.pl)/d'
EOF

%global __perl_requires %{_builddir}/%{tarball_basedir}/%{name}-req
chmod +x %{__perl_requires}


%build
export CFLAGS="%{optflags}"
export JAVA_HOME='/usr/lib/jvm/java-1.8.0' && export JAVA='$JAVA_HOME/jre/bin/java'

# Eucalyptus does not assign the usual meaning to prefix and other standard
# configure variables, so we can't realistically use %%configure.
./configure \
    --prefix=/ \
    --disable-bundled-jars \
    --enable-debug \
    --with-apache2-module-dir=%{_libdir}/httpd/modules \
    --with-axis2=%{_datadir}/axis2-* \
    --with-axis2c=%{axis2c_home} \
    --with-db-home=%{_prefix} \
    --with-extra-version=%{release}

%if %{with coverity}
# Meant to be used with rpmbuild -bc
%{coverity_analysis_dir}/bin/cov-build --dir .coverity-build make
%else
make %{?_smp_mflags}
%endif


%install
make install DESTDIR=$RPM_BUILD_ROOT

# Store admin tool config files
mkdir -p $RPM_BUILD_ROOT/%{_sysconfdir}/eucalyptus-admin
cp -Rp admin-tools/conf/* $RPM_BUILD_ROOT/%{_sysconfdir}/eucalyptus-admin


%files
%license LICENSE
%doc INSTALL README.md

%attr(-,eucalyptus,eucalyptus) %dir /etc/eucalyptus
%config(noreplace) /etc/eucalyptus/eucalyptus.conf
/etc/eucalyptus/eucalyptus-version
/etc/eucalyptus/faults/
# This is currently used for CC and NC httpd logs.
/etc/logrotate.d/eucalyptus
%attr(-,root,eucalyptus) %dir /usr/lib/eucalyptus
%attr(4750,root,eucalyptus) /usr/lib/eucalyptus/euca_rootwrap
%attr(-,root,eucalyptus) %dir /usr/libexec/eucalyptus
%{_tmpfilesdir}/eucalyptus.conf
/usr/sbin/euca-generate-fault
%dir /usr/share/eucalyptus
%dir /usr/share/eucalyptus/lib
%doc /usr/share/eucalyptus/doc/
/usr/share/eucalyptus/faults/
/usr/share/eucalyptus/status/
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus/keys
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus/upgrade
%attr(-,eucalyptus,eucalyptus) %dir /var/log/eucalyptus
%attr(-,eucalyptus,eucalyptus) %dir /var/run/eucalyptus
%attr(-,eucalyptus,eucalyptus-status) %dir /var/run/eucalyptus/status


%files axis2c-common
# CC and NC
/etc/eucalyptus/httpd.conf
/usr/share/eucalyptus/policies
/usr/share/eucalyptus/euca_ipt
/usr/share/eucalyptus/floppy
/usr/share/eucalyptus/populate_arp.pl
%{axis2c_home}/services/EucalyptusGL/


%files blockdev-utils
# SC and NC
%{_udevrulesdir}/55-eucalyptus-openiscsi.rules
%{_libexecdir}/eucalyptus/check-iscsi-target-name
/usr/share/eucalyptus/connect_iscsitarget.pl
/usr/share/eucalyptus/connect_iscsitarget_main.pl
/usr/share/eucalyptus/connect_iscsitarget_sc.pl
/usr/share/eucalyptus/disconnect_iscsitarget.pl
/usr/share/eucalyptus/disconnect_iscsitarget_main.pl
/usr/share/eucalyptus/disconnect_iscsitarget_sc.pl
/usr/share/eucalyptus/get_iscsitarget.pl
/usr/share/eucalyptus/iscsitarget_common.pl


%files common-java
# cloud.d contains random stuff used by every Java component.  Most of it
# probably belongs in /usr/share, but moving it will be painful.
# https://eucalyptus.atlassian.net/browse/EUCA-11002
%dir /etc/eucalyptus/cloud.d
%dir /etc/eucalyptus/cloud.d/elb-security-policy
%config(noreplace) /etc/eucalyptus/cloud.d/elb-security-policy/*
/etc/eucalyptus/cloud.d/scripts/
%{_libexecdir}/eucalyptus/euca-upgrade
/usr/sbin/eucalyptus-cloud
%ghost /var/lib/eucalyptus/services
%attr(-,eucalyptus,eucalyptus) /var/lib/eucalyptus/webapps/
%{_sysctldir}/70-eucalyptus-cloud.conf
%{_unitdir}/eucalyptus-cloud.service
%{_unitdir}/eucalyptus-cloud-upgrade.service
/usr/lib/eucalyptus/eucalyptus-cloud.vmoptions

%files common-java-libs
/usr/share/eucalyptus/lib/*jar*


%files cloud
/usr/sbin/euca-lictool
/usr/sbin/clcadmin-*
/usr/share/eucalyptus/lic_default
/usr/share/eucalyptus/lic_template
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus/db


%files walrus
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus/bukkits


%files sc
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus/volumes


%files cc
/usr/sbin/clusteradmin-*
%{_unitdir}/eucalyptus-cc.service
%{_unitdir}/eucalyptus-cluster.service


%files cc-native
%{axis2c_home}/services/EucalyptusCC/
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus/CC
/usr/lib/eucalyptus/shutdownCC
/usr/sbin/eucalyptus-cluster
/usr/share/eucalyptus/dynserv.pl
%{_unitdir}/eucalyptus-cluster-native.service
%{_unitdir}/eucalyptus-cloud.service.d/eucalyptus-cloud-cluster-native.conf

%files nc
%doc tools/nc-hooks
%config(noreplace) /etc/eucalyptus/libvirt.xsl
%dir /etc/eucalyptus/nc-hooks
/etc/eucalyptus/nc-hooks/example.sh
%{axis2c_home}/services/EucalyptusNC/
%attr(-,eucalyptus,eucalyptus) %dir /var/lib/eucalyptus/instances
%{_libexecdir}/eucalyptus/nodeadmin-manage-volume-connections
%dir /etc/libvirt/hooks
/etc/libvirt/hooks/qemu
/usr/sbin/euca_test_nc
/usr/sbin/eucalyptus-node
/usr/share/eucalyptus/authorize-migration-keys
/usr/share/eucalyptus/config-no-polkit
/usr/share/eucalyptus/detach.pl
/usr/share/eucalyptus/gen_kvm_libvirt_xml
/usr/share/eucalyptus/gen_libvirt_xml
/usr/share/eucalyptus/generate-migration-keys.sh
/usr/share/eucalyptus/getstats.pl
/usr/share/eucalyptus/get_bundle
/usr/share/eucalyptus/get_sys_info
/usr/share/eucalyptus/get_xen_info
/usr/share/eucalyptus/partition2disk
/usr/lib/modules-load.d/70-eucalyptus-node.conf
%{_unitdir}/eucalyptus-nc.service
%{_unitdir}/eucalyptus-node.service
%{_unitdir}/eucalyptus-node-keygen.service


%files admin-tools
%{python_sitelib}/eucalyptus_admin*
%{_bindir}/euctl
%{_bindir}/euserv-*
%{_mandir}/man1/euctl.1*
%{_mandir}/man1/euserv-*.1*
%{_mandir}/man7/eucalyptus-admin.7*
%dir %{_sysconfdir}/eucalyptus-admin
%dir %{_sysconfdir}/eucalyptus-admin/conf.d
%config(noreplace) %{_sysconfdir}/eucalyptus-admin/eucalyptus-admin.ini
%config(noreplace) %{_sysconfdir}/eucalyptus-admin/conf.d/localhost.ini


%files -n eucanetd
%{_libexecdir}/eucalyptus/announce-arp
%{_sbindir}/eucanetd
%attr(-,eucalyptus,eucalyptus) /var/run/eucalyptus/net
/usr/share/eucalyptus/nginx_md.conf
/usr/share/eucalyptus/nginx_proxy.conf
/usr/lib/modules-load.d/70-eucanetd.conf
%{_sysctldir}/70-eucanetd.conf
%{_unitdir}/eucanetd.service
%{_unitdir}/eucanetd-dhcpd*.service
%{_unitdir}/eucanetd-nginx.service

%files imaging-toolkit
%{_libexecdir}/eucalyptus/euca-run-workflow
%{python_sitelib}/eucatoolkit*


%pre
getent group eucalyptus >/dev/null || groupadd -r eucalyptus
getent group eucalyptus-status >/dev/null || groupadd -r eucalyptus-status
getent passwd eucalyptus >/dev/null || \
    useradd -r -g eucalyptus -G eucalyptus-status -d /var/lib/eucalyptus \
    -s /sbin/nologin -c 'Eucalyptus cloud' eucalyptus || :

# This must go in the same package as /etc/eucalyptus/eucalyptus-version to
# ensure /etc/eucalyptus/.upgrade is correct.
if [ "$1" = 2 ]; then
    # Back up the previous installation's jars since they are required for
    # upgrade (EUCA-633)
    BACKUPDIR="/var/lib/eucalyptus/upgrade/eucalyptus.backup.`date +%%s`"
    mkdir -p "$BACKUPDIR"
    EUCABACKUPS=""
    for i in /var/lib/eucalyptus/keys/ /var/lib/eucalyptus/db/ /var/lib/eucalyptus/services /etc/eucalyptus/eucalyptus.conf /etc/eucalyptus/eucalyptus-version /usr/share/eucalyptus/; do
        if [ -e $i ]; then
            EUCABACKUPS="$EUCABACKUPS $i"
        fi
    done

    OLD_EUCA_VERSION=`cat /etc/eucalyptus/eucalyptus-version`
    echo "# This file was automatically generated by Eucalyptus packaging." > /etc/eucalyptus/.upgrade
    echo "$OLD_EUCA_VERSION:$BACKUPDIR" >> /etc/eucalyptus/.upgrade

    tar cf - $EUCABACKUPS 2>/dev/null | tar xf - -C "$BACKUPDIR" 2>/dev/null
fi
exit 0

%post common-java
%systemd_post eucalyptus-cloud.service
%sysctl_apply 70-eucalyptus-cloud.conf || :

%post cc-native
%systemd_post eucalyptus-cluster-native.service

%post nc
%systemd_post eucalyptus-node.service
# The stock policykit policy for libvirt allows members of the libvirt
# group to connect to the system instance without authenticating
getent group libvirt >/dev/null || groupadd -r libvirt
usermod -a -G libvirt eucalyptus || :
/usr/lib/systemd/systemd-modules-load || :

%post -n eucanetd
%systemd_post eucanetd.service
/usr/lib/systemd/systemd-modules-load || :
%sysctl_apply 70-eucanetd.conf || :

%preun common-java
%systemd_preun eucalyptus-cloud.service

%preun cc-native
%systemd_preun eucalyptus-cluster-native.service

%preun nc
%systemd_preun eucalyptus-node.service

%preun -n eucanetd
%systemd_preun eucanetd.service
/usr/lib/systemd/systemd-modules-load || :

%postun common-java
%systemd_postun eucalyptus-cloud.service

%postun cc-native
%systemd_postun eucalyptus-cluster-native.service

%postun nc
%systemd_postun eucalyptus-node.service

%postun -n eucanetd
%systemd_postun eucanetd.service


%changelog
* Fri Sep  7 2018 Steve Jones <steve.jones@appscale.com> - 5.0
- Remove eureport-* tools and euca_conf stub

* Wed Jul 25 2018 Steve Jones <steve.jones@appscale.com> - 5.0
- Add cc-native rpm
- Remove PopulateSnapPoints.groovy script
- Package eucalyptus-cloud default vmoptions

* Thu May 10 2018 Steve Jones <steve.jones@appscale.com> - 4.4.4
- Update libvirt requirement to 2.0.0+

* Fri Mar  9 2018 Steve Jones <steve.jones@appscale.com> - 4.4.3
- Build now handles rpm version

* Fri Aug  4 2017 Garrett Holmstrom <gholms@dxc.com> - 4.4.2
- Version bump (4.4.2)

* Thu Jul 27 2017 Matt Bacchi <mbacchi@hpe.com> - 4.4.2
- Added config-no-polkit script (EUCA-13359)

* Fri Apr 21 2017 Garrett Holmstrom <gholms@dxc.com> - 4.4.1
- Added forward-compat Provides to eucalyptus-common-java*

* Thu Apr 13 2017 Garrett Holmstrom <gholms@dxc.com> - 4.4.1
- Version bump (4.4.1)

* Mon Feb 13 2017 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Add eucalyptus-selinux dependency to eucalyptus package (EUCA-13225)

* Fri Feb 10 2017 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Removed eucanetd dependency from the cc package (EUCA-13212)

* Tue Jan 31 2017 Lincoln Thomas <lincoln.thomas@hpe.com> - 4.4.0
- Install new PopulateSnapPoints.groovy script (EUCA-13122)

* Fri Jan 20 2017 Garrett Holmstrom <gholms@fedoraproject.org> - 4.4.0
- Moved euca-upgrade script to the same package as its systemd unit (EUCA-13139)
- Bumped minimum eucalyptus-java-deps version

* Tue Jan 17 2017 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Removed 01_pg_kernel_params and /etc/eucalyptus/cloud.d/init.d (EUCA-12644)

* Fri Jan 13 2017 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Bumped eucanetd and nc's eucalyptus-selinux minimum version to 0.2 (EUCA-12424)

* Fri Jan  6 2017 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Added /etc/eucalyptus/faults (EUCA-12391)
- Let makefiles handle creation of /var/lib/eucalyptus/* (EUCA-508)
- Removed nodeadmin-(un)pack scripts (EUCA-13042)

* Thu Jan  5 2017 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Removed file extension from authorize-migration-keys

* Thu Jan  5 2017 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- authorize-migration-keys is now a python script (EUCA-12883)
- Avoid packaging .pyc/.pyo byte compiled files in NC package (EUCA-12883)

* Thu Dec 22 2016 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Add new eucanetd service files (EUCA-12424)

* Fri Dec 16 2016 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Moved iscsidev.sh to $libexecdir/check-iscsi-target-name (EUCA-2414, EUCA-12646)
- Moved udev rules to /lib/udev/rules.d (EUCA-12645)

* Tue Dec  6 2016 Matt Bacchi <mbacchi@hpe.com> - 4.3.1
- Run systemd-modules-load in nc package post scriptlet (EUCA-12983)

* Fri Dec  2 2016 Matt Bacchi <mbacchi@hpe.com> - 4.3.1
- Added /etc/libvirt/hooks/qemu to nc package (EUCA-12594)

* Tue Nov 29 2016 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Change README to README.md

* Tue Nov 15 2016 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Added rbd dependency to the sc package (EUCA-12941)

* Fri Nov 11 2016 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Removed vtun dependency and vtunall template (EUCA-12755)

* Wed Nov  9 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.1
- Added "coverity" build option

* Fri Nov  4 2016 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Added nginx_md.conf (EUCA-12893)

* Tue Nov  1 2016 Matt Bacchi <mbacchi@hpe.com> - 4.4.0
- Removed getstats_net.pl (EUCA-12864)

* Thu Oct 27 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.1
- Bumped minimum eucalyptus-java-deps version (EUCA-12885)

* Wed Oct 12 2016 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Version bump (4.4.0)

* Wed Oct 12 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.1
- Version bump (4.3.1)

* Fri Sep  9 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0.1
- Added nginx >= 1.9.13 dependency to eucanetd package (EUCA-12734)

* Tue Aug 30 2016 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Removed create-loop-devices script
- Removed conntrack_kernel_params script

* Tue Aug 30 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Switched to building against packaged dependencies (EUCA-10666)
- Fixed common-java package's post script on el7
- BuildRequired ant-apache-regexp

* Thu Aug 25 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0.1
- Version bump (4.3.0.1)
- Moved /var/lib/eucalyptus backup script to cloud package

* Mon Aug 15 2016 Garrett Holmstrom <gholms@hpe.com> - 4.4.0
- Removed RHEL 6 support
- Moved euca-upgrade to cloud package
- Removed useless defattr statements
- Tagged license files as such
- Switched to building against packaged dependencies (EUCA-10666)

* Mon Aug 15 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Dropped eucalyptus-selinux dependency from admin-tools package

* Wed Jun 29 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Added nodeadmin-manage-volume-connections (EUCA-12514)

* Thu Jun  9 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Made eucalyptus-cloud pull in admin tools for convenience (EUCA-12429)

* Thu May  5 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Added node support scripts (EUCA-12285)

* Fri Apr  8 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Removed old admin tools, except for eureport-*
- eucalyptus-admin-tools may now be installed standalone

* Mon Apr  4 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Added dependencies on eucalyptus-selinux on el7

* Tue Mar 29 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Removed much of /etc/eucalyptus/cloud.d (EUCA-12005)

* Thu Mar 24 2016 Garrett Holmstrom <gholms@hpe.com> - 4.3.0
- Cleaned up calls to systemd macros

* Thu Mar 24 2016 Vasiliy Kochergin <vasya@hpe.com> - 4.3.0
- Don't install euca_mountwrap

* Mon Mar 21 2016 Garrett Holmstrom <gholms@hpe.com> - 4.2.2
- Added eucalyptus-admin(7) man page

* Mon Mar 21 2016 Matt Bacchi <mbacchi@hpe.com> - 4.3.0
- add --with-java-home to configure command for java 1.8 support

* Tue Mar 15 2016 Vasiliy Kochergin <vasya@hpe.com> - 4.3.0
- Switched to Java 1.8

* Thu Mar 10 2016 Garrett Holmstrom <gholms@hpe.com> - 4.2.2
- Added eucalyptus user to eucalyptus-status group (EUCA-12108)

* Mon Feb 29 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.2
- Version bump (4.2.2)

* Thu Feb 18 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Removed /etc/eucalyptus/cloud.d/www

* Wed Feb 17 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Added qemu-kvm dependency to nc package (fixes /dev/kvm permissions)

* Tue Feb 16 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Removed euca-get-credentials

* Wed Feb 10 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Added compatibility symlinks for eucalyptus-cluster/node systemd units

* Tue Feb  9 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Don't install euca-imager
- Added seabios dependency to nc package (EUCA-12003)
- Provide eucalyptus-node and eucalyptus-cluster

* Mon Feb  8 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Removed old cruft
- Started loading sysctl values where needed on RHEL 7
- Started loading modules where needed on RHEL 7

* Wed Feb  3 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Added systemd scriptlets
- Added systemd files
- Switched to stock eucalyptus.conf defaults
- Switched to eucalyptus-provided euca-WSDL2C.sh
- Added main executables for CC and NC
- Stopped tracking temporary CC and NC httpd config files

* Thu Jan 21 2016 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Depend on unversioned postgresql packages for RHEL 7

* Tue Nov 17 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Fixed BuildRequires for RHEL 7

* Tue Nov  3 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.1
- Version bump (4.2.1)

* Fri Oct 30 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.3.0
- Version bump (4.3.0)

* Tue Sep 22 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Added /var/run/eucalyptus/net to eucanetd package (EUCA-11411)

* Mon Sep 21 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Pulled in python-requestbuilder >= 0.4 to fix unsigned redirects (EUCA-11378)

* Tue Sep  8 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Remove vmware admin tools

* Tue Jul 28 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Added pv dep to imaging-toolkit package

* Thu Jul 16 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Bumped python-requestbuilder dep to >= 0.3.2

* Mon Jun 29 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Added more new admin tools
- Added /etc/eucalyptus-admin

* Mon Jun 22 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Added ELB security policies (EUCA-10985)

* Tue Apr 14 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.1
- Added announce-arp support script for eucanetd (EUCA-10741)

* Thu Apr  9 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Version bump (4.2.0)

* Tue Apr  7 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.2.0
- Removed pre-4.0 Requires/Provides/Obsoletes
- Removed postgresql91 dependencies (only needed for 4.0 -> 4.1 upgrades)
- Removed pre-el6 leftovers
- Added first batch of new admin, support scripts

* Mon Mar 23 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.1
- Added libuuid-devel build dependency

* Mon Mar  9 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.1
- Dropped euca-install-service-image (EUCA-10369)

* Tue Jan 20 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Made eucalyptus-cc depend on eucanetd for conntrack_kernel_params (EUCA-10405)

* Thu Jan 15 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Added sample NC hooks as doc files (EUCA-9680)

* Tue Jan 13 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Moved conntrack_kernel_params to eucanetd package (EUCA-10314)

* Mon Jan 12 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Fixed typo in old db-home configure script option (EUCA-10319)

* Tue Jan  6 2015 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Reversed eucanetd -> eucalyptus-nc dependency (EUCA-10219)
- Removed unused font dependency
- Added postgresql91 bits for upgrades from 4.0.x (EUCA-10150)

* Fri Dec 19 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Added euca-install-service-image to admin tools (EUCA-10201)
- Added service-images.yml for euca-install-service-image (EUCA-10202)
- Added PyYAML and euca2ools dependencies to support euca-install-service-image

* Tue Dec  2 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Added /usr/share/eucalyptus/status

* Mon Nov  3 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Added librados2 and librbd1 deps to sc and nc packages (EUCA-10099)
- Dropped drbd

* Wed Oct  8 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Added nginx_proxy.conf to eucanetd package

* Fri Oct  3 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Bumped nc's euca2ools dependency to >= 3.2
- Removed postgresql 9.1 dependencies (EUCA-9703)

* Fri Sep  5 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0
- Added eucalyptus-status group (EUCA-9958)
- Added /var/run/eucalyptus/status dir (EUCA-9958)

* Fri Sep  5 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.2
- Version bump (4.0.2)

* Tue Jul 22 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0-0
- Added build-time dependency on json-c-devel

* Thu Jun 19 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.1.0-0
- Version bump (4.1.0)
- Added dependency on postgresql92 (EUCA-9700)

* Tue Jun 17 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.1-0
- Switched to monolithic source tarball naming

* Fri Jun 13 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.1-0
- Moved httpd-cc.conf and httpd-nc.conf to /var/run/eucalyptus
- Dropped osg package (EUCA-9468)
- Moved WS-Security client policies to /usr/share/eucalyptus/policies (EUCA-8706)

* Fri May 16 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Ensure openssl allows for credential download

* Thu May  8 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Dropped most new admin tool executables (EUCA-9064)

* Tue Apr 29 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Added euca-(de)register service and euca-describe-service-types

* Fri Apr  4 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Dropped unused validation stuff (EUCA-8569)
- Dropped old admin web UI stuff (EUCA-8616)

* Thu Feb 27 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Added euca-run-workflow

* Thu Feb 20 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Added new eucalyptus-imaging-toolkit subpackage
- Switched to stock dhcpd package (EUCA-6869)
- Renamed -eucanet subpackage to eucanetd (EUCA-8768)

* Fri Feb 14 2014 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Add new admin tool executables

* Thu Nov 21 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Update java requires and build requires for RHEL 6.5 support

* Tue Nov 19 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Add get_bundle tool

* Mon Nov 11 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 4.0.0-0
- Update to version 4.0.0

* Thu Oct 31 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.1-0
- Add logrotate for CC/NC

* Thu Oct 17 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- nc sub-package now requires euca2ools 3.0.2 or later

* Fri Oct 04 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- Add eucalyptus-backup-restore

* Tue Sep 24 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- Added iptables-preload example to CC package

* Tue Sep 10 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- Remove console sub-package

* Wed Aug 28 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- Add eucanetd tech preview

* Wed Jul 17 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.3.0-0
- Require postgresql >= 9.1.9

* Fri Jul  5 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- Added files for SAN common stuff

* Tue Jul  2 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- Dropped RHEL 5 support

* Mon Jun 24 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.4.0-0
- Updated to 3.4.0

* Thu Jun 20 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.3.1-0
- Version bump

* Thu Jun 20 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.3.0.1-0
- Version bump

* Tue Mar 19 2013 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.3.0-0
- remove velocity Requires

* Fri Nov 30 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added sample iscsid.conf docfile

* Tue Nov 27 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added eucaconsole user and group
- Change ownership for console package files and directories
- Added /var/run/eucalyptus-console directory for writing pidfile

* Mon Nov 19 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added sample multipath.conf docfile

* Tue Nov 13 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Reload udev rules in postun instead of preun

* Wed Oct 31 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- User Console python package changed from server => eucaconsole

* Fri Oct 26 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Updated eucadmin license tag

* Wed Oct 24 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Merged spec file content for Eucalyptus Console

* Tue Oct 16 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added temporary fix for jasperreports jar removal
- Removed /etc/eucalyptus/cloud.d/reports directory

* Tue Oct 16 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added iproute dependency to the cc package

* Mon Oct 15 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added getstats_net.pl to the CC subpackage

* Wed Oct 10 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Moved DASManager to the sc package

* Mon Oct  8 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added udev rules for drbd
- Reload udev rules on install and uninstall

* Sat Oct  6 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added eureport-delete-data to files section

* Thu Oct  4 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added device-mapper-multipath dependencies to sc and nc packages
- Added missing perl requires
- Added udev rules for multipath support

* Mon Sep 24 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Change ownership on /etc/eucalyptus to eucalyptus:eucalyptus
  This is a temporary fix for issue BROKER-9

* Fri Sep 21 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Updated reporting CLI tool names
- Added new perl dependencies for reporting

* Wed Sep 12 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Split java-common into java-common and java-common-libs

* Tue Sep  4 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added report generation tool

* Thu Aug 23 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Added fault message dir

* Mon Jul 30 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.2.0-0
- Version bump

* Fri Jun  1 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.1-0
- Moved 01_pg_kernel_params script to -cloud package

* Wed May 30 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.1-0
- Dropped now-nonexistent volume management scripts

* Tue May 29 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.1-0
- Treat eucalyptus.conf like a config file

* Fri May 25 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.1-0
- Depend on bc so the eucalyptus-cloud init script works

* Wed May 23 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.1-0
- Fixed bundled lib tarball explosion
- Swapped in configure --with-db-home
- Added extra version info
- Cleaned up extraneous build stuff

* Wed May 16 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.1-0
- Dropped old udev reload

* Fri May 11 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.1-0
- Depend on postgres, not mysql

* Mon Mar 19 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.0.1-2
- Added iSCSI client dependency to SC package

* Thu Mar 15 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.0.1-1
- Update to Eucalyptus 3.0.1 RC 1

* Wed Feb  8 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.0.0-3
- Update to Eucalyptus 3.0.0 RC 3

* Tue Feb  7 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.0.0-2
- Update to Eucalyptus 3.0.0 RC 2

* Thu Feb  2 2012 Eucalyptus Release Engineering <support@eucalyptus.com> - 3.0.0-1
- Update to Eucalyptus 3.0.0 RC 1

* Tue Jun  1 2010 Eucalyptus Release Engineering <support@eucalyptus.com> - 2.0.0-1
- Version 2.0 of Eucalyptus Enterprise Cloud
  - Windows VM Support
  - User/Group Management
  - SAN Integration
  - VMWare Hypervisor Support
