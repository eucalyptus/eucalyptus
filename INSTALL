===================================
Building and Installing from Source

This is the Eucalyptus process for developers wishing to understand or
modify Eucalyptus source code. It allows them to obtain and build the
Eucalyptus source code into installable packages.

If you instead wish to evaluate Eucalyptus by deploying a small private
cloud on your own machine with a single command, see
http://www.eucalyptus.com/download to download and install the FastStart 
package.

If you instead wish to implement a Eucalyptus cloud on multiple systems with
full configurability, from pre-built component packages, refer to the
Eucalyptus documentation at https://docs.eucalyptus.com/eucalyptus/latest/#install-guide/eucalyptus.html 
to get started.
===================================

The process consists of:
1. Setting up your Linux environment,
2. Obtaining the source code and dependent github repositories,
3. Installing build dependencies,
4. Building the code from source into RPM packages,
5. Distributing the RPMs to other systems,
6. Installing the RPMs to the build system and other systems,
7. Configuring Eucalyptus, using the Eucalyptus Install Guide,
8. Rebuilding code as needed, and installing rebuilt artifacts.

***NOTE*** The following instructions are subject to change and 
may be INCOMPLETE. Contributions encouraged! :)

Eucalyptus only supports 64-bit installations on CentOS/RHEL 7.

These instructions were tested on the "Minimal" distribution of CentOS 7.

We will use the Eucalyptus RPM build process to build the source code,
install it to a certain directory tree, and create the RPM packages. Those
packages will be installed on all systems that will be part of the
Eucalyptus cloud, which can include the build system.


1. Set up the environment
=========================

On each system (build or others), add the following environment variables to
your .bashrc, or another file you can source before executing the
commands that use them.

Pick any directories you wish. Just don't make any nested in any of the
others.

#------------------------
# Get github repositories here
export EUCAGIT=~/euca/git

# Lay out the rpmbuild structure here, for sources, build outputs, and RPMs
export EUCARPM=~/euca/rpmbuild

# Distribute the rpmbuild outputs to other systems here
# Define this on the build system too
export EUCAPKGS=~/euca/pkgs
#------------------------

Log out and in again, or "source <this file>".

Create the directories if they don't already exist:

>>>>> Command 1a: 
# mkdir -p $EUCAGIT $EUCAPKGS $EUCARPM/SPECS $EUCARPM/SOURCES

Install the following tools and repos needed for building:

>>>>> Command 1b: 
# yum install git yum-utils wget \
    http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm

Install the Eucalyptus repository that will allow yum to find the packages
needed for building Eucalyptus that are not in the other repos. Point your
Web browser to:
http://downloads.eucalyptus.com/software/eucalyptus/nightly/devel

Drill down to find the latest version of the RPM for your desired Linux
platform, for "x86_64" (not "source"). As of this writing, the subdirs "7",
"7Server", and "7Workstation" all point to the same repositories. Look for
the latest version of:

eucalyptus-release-nightly-<...>.rpm

>>>>> Command 1c: 
# yum install <that Eucalyptus URL>

Similarly, install the euca2ools repository from:
http://downloads.eucalyptus.com/software/euca2ools/nightly/devel
Drill down to find the latest:
euca2ools-release-nightly-<...>.rpm

>>>>> Command 1d: 
# yum install <that euca2ools URL>

If you are setting up a non-build system to install Eucalyptus on, skip the
next build-only steps and proceed to Step 6 (Install Eucalyptus).


2. Get the source code (build system only)
======================

Get the Eucalyptus source code repository:

>>>>> Command 2a: 
# git clone https://github.com/eucalyptus/eucalyptus.git $EUCAGIT/eucalyptus

Get the repository containing the RPM build spec and a few other files
needed to build and install the source code:

>>>>> Command 2b: 
# git clone https://github.com/eucalyptus/eucalyptus-rpmspec.git $EUCAGIT/eucalyptus-rpmspec

Get the Eucalyptus-specific libraries needed to build and run:

>>>>> Command 2c: 
# git clone https://github.com/eucalyptus/eucalyptus-cloud-libs.git $EUCAGIT/eucalyptus-cloud-libs


3. Install build dependencies (build system only)
=============================

Follow these instructions to install the required build dependencies.

Install the general software development build dependencies:

>>>>> Command 3a: 
# yum groupinstall development

Install the Eucalyptus-specific build dependencies:

>>>>> Command 3b: 
# yum-builddep --nogpgcheck eucalyptus
(The GPG keys are not kept up-to-date for nightly builds, hence the
--nogpgcheck.)


4. Build Eucalyptus (build system only)
===================

We run the rpmbuild command with the Eucalyptus build spec to build the
source code, install it into a destination file tree, and create binary RPMs
that can be installed on this system or any other system that has been
prepared using steps 1-3 above.

Remove any artifacts from the previous rpmbuild:

BE CAREFUL: Make sure $EUCARPM is defined!
>>>>> Command 4a: 
# if [ -n "$EUCARPM" ]; then rm -Rf $EUCARPM/*/*; fi

Link to the RPM spec file:

>>>>> Command 4b: 
# ln -fs $EUCAGIT/eucalyptus-rpmspec/eucalyptus.spec $EUCARPM/SPECS

Create the tarballs of eucalyptus and the eucalyptus-cloud-libs that
rpmbuild will un-tar and build. We also need the cloud libs installed
at /usr/share/eucalyptus for rpmbuild to find.

>>>>> Commands 4c: 
# cd $EUCAGIT && \
autoconf && \
tar -cvJf $EUCARPM/SOURCES/eucalyptus.tar.xz --exclude .git --exclude eucalyptus/clc/lib --exclude build-info.properties eucalyptus
>>>>> (end of commands)

>>>>> Commands 4d: 
# cd $EUCAGIT/eucalyptus-cloud-libs && \
tar -cvJf $EUCARPM/SOURCES/eucalyptus-cloud-libs.tar.xz *.jar licenses && \
autoconf && \
./configure --prefix=/usr && \
make clean && make && make install
>>>>> (end of commands)

Now rpmbuild will do all of the build and packaging, in one command:

>>>>> Command 4e:
# rpmbuild --define "_topdir $EUCARPM" --define 'tarball_basedir eucalyptus' --define 'cloud_lib_tarball eucalyptus-cloud-libs.tar.xz' -bb $EUCARPM/SPECS/eucalyptus.spec


5. Distribute the Build (build system only)
=======================

At this point, if you plan to use Eucalyptus on more than one system, you're
ready to push the software out to the other systems.

From the build system, copy the packages to the other systems' hostnames or
IP addresses:

>>>>> Commands 5a: # 
# rsync -ar $EUCARPM/RPMS/ root@{host-2}:$EUCAPKGS
# rsync -ar $EUCARPM/RPMS/ root@{host-3}:$EUCAPKGS
...
>>>>> (end of commands)


6. Install and Configure Eucalyptus (on all systems)
===================================

You can now install the Eucalyptus RPMs onto each system.
The Eucalyptus documentation can be found here:
https://docs.eucalyptus.com/

Follow the instructions in the Installation Guide for the following
sections:

- Plan Your Installation
- Configure Dependencies

After completing those sections, install the packages you have built.
Install ALL of the Eucalyptus RPMs even if all the components are not
needed, to prevent dependency problems with Eucalyptus packages that have
the same names but different version numbers in the eucalyptus-nightly
repository. You can still choose which components to configure and run on
each system.

If you are updating an existing deployment, stop all eucalyptus services
before re-installing packages.

For the build system (if you're installing onto it):

>>>>> Command 6a: 
# yum install $EUCARPM/RPMS/noarch/* $EUCARPM/RPMS/x86_64/*
Note: If you have rebuilt packages per Step 7 below, the package versions
will likely be the same, so "yum install" will do nothing. To force the
new packages to be updated, you must re-install them:
# yum reinstall $EUCARPM/RPMS/noarch/* $EUCARPM/RPMS/x86_64/*

For all other systems:

>>>>> Command 6a: 
# yum install $EUCAPKGS/noarch/* $EUCAPKGS/x86_64/*
Or, if you are updating packages per above:
# yum reinstall $EUCAPKGS/noarch/* $EUCAPKGS/x86_64/*

The yum command may install many other Linux packages on which the
Eucalyptus packages depend.

Continue following the instructions from the Installation Guide, starting at
the "Install Eucalyptus Release Packages" section (NOT the "Install Nightly
Release Packages" section).

Skip the "yum install" commands for what you've already installed:
1. Skip installing the package repositories from downloads.eucalyptus.com.
2. Skip installing these packages, because you installed them already:
     eucalyptus-node
     eucalyptus-cloud
     eucalyptus-cluster
     eucalyptus-sc 
     eucalyptus-walrus

NOTE: There are other "yum install" commands and other steps in the
"Install Eucalyptus Release Packages" sections that you still need to
perform, besides installing Eucalyptus packages, such as removing the
default libvirt network (virsh commands), and "yum install"s of
eucalyptus-service-image and eucaconsole.

Continue the Installation Guide through the "Configure the Runtime
Environment" section.

Your cloud should now be configured and running!


7. Rebuild Eucalyptus (build system only)
=====================

NOTE: Before replacing files on installed systems, stop any Eucalyptus
services or processes that may be using them. Then restart them after the
files have been replaced. If you are unclear what to stop and restart,
refer to the Installation Guide sections "Shutdown Services" and
"Restart Eucalyptus Services".

NOTE: The following steps require that the above rpmbuild process be
performed at least once, and the resulting RPMs be installed at least once
onto any system you intend to update with rebuilt artifacts (including the
build system). 

To rebuild code after changing it, you can either:

1. Repeat step 4, using rpmbuild. Then, you can then either:

  1a. Copy the RPM packages you've changed to the installed systems, and
      re-install them.

  1b. Or, copy individual jars or executables (or other built artifacts)
      from the $EUCARPM/BUILD directory tree to the installed systems,
      replacing those files.

2. Or, you can build the code using "make", without rpmbuild. Then, you can
copy individual jars or executables (or other built artifacts) built by
"make" to the installed systems, replacing those files.

Non-rpmbuild rebuilds (option 2)
---------------------

If you choose this option, add the following environment variable to your
.bashrc, or another file you can source before executing the commands that
use them.

export EUCALYPTUS=/

Do not use any other directory besides "/", or neither "make" nor "rpmbuild"
will work. Future "rpmbuild"s will continue to work as long as it's defined
this way.

Run the "configure" scripts to prepare the system to build:

>>>>> Commands 8a: 
# cd $EUCAGIT/eucalyptus-cloud-libs
# autoconf
# ./configure --prefix=/usr
# cd $EUCAGIT/eucalyptus
# autoconf
# ./configure --prefix=$EUCALYPTUS \
--disable-bundled-jars \
--with-apache2-module-dir=/usr/lib64/httpd/modules \
--with-axis2==/usr/share/axis2-* \
--with-axis2c=/usr/lib64/axis2c \
--with-db-home=/usr

Run the "make" commands below. The "make clean" deletes the artifacts from
any previous build. The "make" builds the artifacts, in the
$EUCAGIT/eucalyptus tree. The "make install" of eucalyptus-cloud-libs
is required for the eucalyptus "make" to find the cloud libraries at 
/usr/share/eucalyptus, because --disable-bundled-jars was supplied to
the configure command. Without it, the "make" of eucalyptus will download
the "master" branch of the cloud libs from github, regardless of the branch
you're building, which would be incorrect if you're not building "master".

>>>>> Command 8b: 
# cd $EUCAGIT/eucalyptus-cloud-libs
# make clean && make && make install
# cd $EUCAGIT/eucalyptus
# make clean && make

The next "make install" command copies the built artifacts into their runtime
destination directories on this build system, overwriting any files already
installed by package-based installs.

If your build system is also an installed system as part of your running
cloud, the following step will update all necessary files. If your build
system is not part of your cloud, skip this step.

If you only want to update a few artifact files (such as a .jar file) onto
your installed systems, you can skip the following step, and instead copy
those files from the $EUCAGIT/eucalyptus-cloud-libs and/or
$EUCAGIT/eucalyptus trees to their proper destination directories on the
installed systems.

>>>>> Commands 8c: (optional)
# cd $EUCAGIT/eucalyptus
# make install
>>>>> (end of commands)

NOTE: If you do a non-rpmbuild (option 2), and later wish to do an rpmbuild
(option 1), remove all make artifacts from the non-rpmbuild before doing the
rpmbuild:

>>>>> Commands 8d: 
# cd $EUCAGIT/eucalyptus
# make distclean
>>>>> (end of commands)

(End)
