#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.
#

# Usage:
#   configure   [--with-axis2c=<dir>]
#               [--with-axis2=<dir>]
#               [--with-libvirt=<dir>]
#               [--with-interface-theme=<name>]
#               [--enable-appliance-store]
#               [--enable-debug]

AC_PREREQ(2.61)
AC_INIT(Eucalyptus,
        m4_esyscmd([cat VERSION| tr -d '\n']),
        support@open.eucalyptus.com)
AC_CONFIG_SRCDIR([wsdl/eucalyptus_nc.wsdl])
AC_CONFIG_HEADER([util/eucalyptus-config.h])
AC_PREFIX_DEFAULT([/opt/eucalyptus/])
AC_USE_SYSTEM_EXTENSIONS
AC_CANONICAL_BUILD
EUCA_VERSION="`cat VERSION`"

# let's figure out where is the source tree
if test ${srcdir} = "." ; then
        TOP=`pwd`
else
        TOP=${srcdir}
fi

# variables we'll need later on
APACHE2_MODULE_DIR=""
AXIS2C_HOME="${AXIS2C_HOME}"
AXIS2_HOME="${AXIS2_HOME}"
AXIS2C_SERVICES=""
LIBVIRT_HOME="${LIBVIRT_HOME}"
VDDK_HOME="${VDDK_HOME}"
INTERFACE_THEME="eucalyptus"
EXTENSIONS=""
ANT=""
JAVA=""
WSDL2C=""
java_min_version="1.6.0"
ant_min_version="1.6.5"

# these are for large files (>2GB)
LDFLAGS="`getconf LFS64_LDFLAGS` `getconf LFS_LDFLAGS` $LDFLAGS"
LIBS="`getconf LFS64_LIBS` `getconf LFS_LIBS` $LIBS"
CFLAGS="`getconf LFS64_CFLAGS` `getconf LFS_CFLAGS`"

# compile options
CFLAGS="${CFLAGS} -Wall -Wno-unused-variable -fPIC -DHAVE_CONFIG_H -std=gnu99"
INCLUDES="${INCLUDES} -I. -I.. -Igenerated -I${TOP}/storage -I${TOP}/node -I${TOP}/util -I${TOP}/net"

# Arguments checking
AC_ARG_WITH(apache2-module-dir,
        [  --with-apache2-module-dir=<dir>      where Apache modules are installed],
        [APACHE2_MODULE_DIR="${withval}"])
AC_ARG_WITH(axis2,
        [  --with-axis2=<dir>                   where axis2 is installed],
        [AXIS2_HOME="${withval}"])
AC_ARG_WITH(axis2c,
        [  --with-axis2c=<dir>                  where axis2c is installed],
        [AXIS2C_HOME="${withval}"])
AC_ARG_WITH(axis2c-services,
        [  --with-axis2c-services=<dir>         where axis2c services are installed],
        [AXIS2C_SERVICES="${withval}"])
AC_ARG_WITH(libvirt,
        [  --with-libvirt=<dir>                 where libvirt is installed],
        [LIBVIRT_HOME="${withval}"])
AC_ARG_WITH(wsdl2c-sh,
        [  --with-wsdl2c-sh=<file>              where WSDL2C.sh is installed],
        [WSDL2C="${withval}"])
AC_ARG_WITH(vddk,
        [  --with-vddk=<dir>                    where VMware's VDDK is installed],
        [VDDK_HOME="${withval}"])
AC_ARG_WITH(interface-theme,
        [  --with-interface-theme=<name>        the theme to be used],
        [INTERFACE_THEME="${withval}"])
AC_ARG_ENABLE(appliance-store,
        [  --enable-appliance-store             enable the store tab],
        [if test "${enableval}" != "no"; then
                EXTENSIONS="store"
        fi])

AC_ARG_ENABLE(debug,
        [  --enable-debug                       include debugging info when compiling],
        [if test "${enableval}" != "no"; then
                CFLAGS="$CFLAGS -g -DDEBUG"
        fi])


# If we didn't specify the services directory, let's use the default
if test -z "$AXIS2C_SERVICES" ; then
        AXIS2C_SERVICES="${AXIS2C_HOME}/services"
fi

# If we didn't specify the location of Apache modules, assume mod_axis2
# resides within AXIS2C_HOME
if test -z "$APACHE2_MODULE_DIR"; then
        APACHE2_MODULE_DIR="${AXIS2C_HOME}/lib"
fi

AM_PATH_PYTHON([2.5])
AC_MSG_CHECKING([whether $PYTHON version is < 3.0])
AM_PYTHON_CHECK_VERSION([$PYTHON], [3.0],
                        [AC_MSG_RESULT([no])
                         AC_MSG_ERROR([Python >= 3.0 is not supported])],
                        [AC_MSG_RESULT([yes])])

# Fix the paths for includes and libraries
if test -n "${AXIS2C_HOME}" ; then
        if test -d "${AXIS2C_HOME}"/include ; then
                for x in ${AXIS2C_HOME}/include/axis2-* ${AXIS2C_HOME}/include/rampart-*; do
                        INCLUDES="${INCLUDES} -I$x"
                done
        fi
        if test -d "${AXIS2C_HOME}"/lib ; then
                LDFLAGS="-L${AXIS2C_HOME}/lib ${LDFLAGS}"
        fi
        if test -d "${AXIS2C_HOME}"/modules/rampart ; then
                LDFLAGS="-L${AXIS2C_HOME}/modules/rampart ${LDFLAGS}"
        fi
fi
if test -n "${LIBVIRT_HOME}" ; then
        if test -d "${LIBVIRT_HOME}"/include ; then
                INCLUDES="${INCLUDES} -I${LIBVIRT_HOME}/include"
                LDFLAGS="-L${LIBVIRT_HOME}/lib ${LDFLAGS}"
        fi
fi
if test -n "${VDDK_HOME}" ; then
        if test -d "${VDDK_HOME}"/include/ ; then
                # Using tarball direct from VMware
                INCLUDES="${INCLUDES} -I${VDDK_HOME}/include/"
                AS_IF([test "x$build_cpu" = "xx86_64"], [LDFLAGS="-L${VDDK_HOME}/lib64 ${LDFLAGS}"],
                      [LDFLAGS="-L${VDDK_HOME}/lib32 ${LDFLAGS}"])
        elif test -d "${VDDK_HOME}"/lib/vmware-vix-disklib/include/ ; then
                # Using installed tarball
                INCLUDES="${INCLUDES} -I${VDDK_HOME}/lib/vmware-vix-disklib/include/"
                LDFLAGS="-L${VDDK_HOME}/lib ${LDFLAGS}"
        fi
fi


# Checks for programs.
AC_PROG_AWK
AC_PROG_CC
AC_PROG_INSTALL
AC_PROG_MAKE_SET
AC_PATH_PROG(RM, rm)

if test -z "$WSDL2C"; then
        # let's search for WSDL2C.sh
        AC_PATH_PROG([WSDL2C], WSDL2C.sh,,${AXIS2C_HOME}/bin/tools/wsdl2c:$PATH)
fi
## HACK:  Sometimes WSDL2C.sh isn't executable for some reason.  We call sh on
## it directly, so look for a non-executable file just in case.
if test -z "$WSDL2C" && test -f ${AXIS2C_HOME}/bin/tools/wsdl2c/WSDL2C.sh; then
        WSDL2C=${AXIS2C_HOME}/bin/tools/wsdl2c/WSDL2C.sh
fi
# check if we found WSDL2C.sh
if test -z "$WSDL2C" ; then
        AC_MSG_ERROR([Cannot find WSDL2C.sh!])
fi

# we need JAVA_HOME
if test -z "$JAVA_HOME" ; then
        AC_PATH_PROG([JAVAC], javac,, $PATH)
        if test -z "$JAVAC" ; then
                AC_MSG_ERROR([JAVA_HOME is not defined and javac was not found in PATH])
        else
                JAVA_HOME=$( readlink -f ${JAVAC} | rev | cut -d/ -f3- | rev )
        fi
fi

AC_PATH_PROG([ANT], ant,,$ANT_HOME/bin:$PATH)
AC_PATH_PROG([JAVA], java,,$JAVA_HOME/bin)

# Check the version of java and ant
if test -z "$ANT" ; then
        AC_MSG_ERROR([Cannot find ant!])
fi
if test -z "$JAVA" ; then
        AC_MSG_ERROR([Cannot find java!])
fi
java_version=`$JAVA -version 2>&1 | grep "java version" | \
        sed -e 's/.*java version "\(.*\)".*/\1/'`
goodversion=`expr $java_version ">=" $java_min_version`
if test $goodversion -eq 0; then
        AC_MSG_ERROR([Eucalyptus needs at least JDK version $java_min_version])
fi

# now let's test we have jni.h
if test ! -e $JAVA_HOME/include/jni.h ; then
        AC_MSG_ERROR([Cannot find jni.h in $JAVA_HOME: do you have a JDK installed?])
fi

ant_version=`$ANT -version 2>&1 | grep "Ant.* version" | \
        sed -e 's/.*Ant.* version \([[0-9.]]*\).*/\1/'`
goodversion=`expr $ant_version ">=" $ant_min_version`
if test $goodversion -eq 0; then
        AC_MSG_ERROR([Eucalyptus needs at least ANT version $ant_min_version])
fi
# some version of ant picks up the wrong java
java_version=`$ANT -diagnostics 2>&1 | grep ^java.version | \
        sed -e 's/java.* \([[0-9.]]*\).*/\1/'`
goodversion=`expr $java_version ">=" $java_min_version`
if test $goodversion -eq 0; then
        AC_MSG_ERROR([ANT is using the wrong java (version less than $java_min_version)])
fi


# Checks for libraries.
# FIXME: Replace `main' with a function in the library
AC_CHECK_LIB([axis2_axiom],[main],,AC_MSG_ERROR([Cannot find axis2 libs!]))
AC_CHECK_LIB([axis2_engine],[main],,AC_MSG_ERROR([Cannot find axis2 libs!]))
AC_CHECK_LIB([axis2_http_common],[main],,AC_MSG_ERROR([Cannot find axis2 libs!]))
AC_CHECK_LIB([axis2_http_receiver],[main],,AC_MSG_ERROR([Cannot find axis2 libs!]))
AC_CHECK_LIB([axis2_http_sender],[main],,AC_MSG_ERROR([Cannot find axis2 libs!]))
AC_CHECK_LIB([guththila],[main],,AC_MSG_ERROR([Cannot find libguththila!]))
AC_CHECK_LIB([axis2_parser],[axiom_xml_reader_init],,AC_MSG_ERROR([Cannot find axis2 libs!]))
AC_CHECK_LIB([axutil],[main],,AC_MSG_ERROR([Cannot find libaxutil]))
AC_CHECK_LIB([m],[main])
AC_CHECK_LIB([mod_rampart],[main],,AC_MSG_ERROR([Cannot find libmod_rampart]))
AC_CHECK_LIB([neethi],[main],,AC_MSG_ERROR([Cannot find libneethi!]))
AC_CHECK_LIB([pthread],[main])
AC_CHECK_LIB([rt],[main])
AC_CHECK_LIB([virt],[main],true,AC_MSG_ERROR([Cannot find libvirt!]))
AC_CHECK_LIB([curl],[main],true,AC_MSG_ERROR([Cannot find libcurl!]))
AC_CHECK_LIB([z],[inflate])
AC_CHECK_LIB([cap],[cap_from_name],,AC_MSG_WARN([Cannot find sufficiently recent libcap will not use it]))
AC_CHECK_LIB([xml2],[xmlFree],,AC_MSG_ERROR([Cannot find libxml2!]))

# Checks for header files.
AC_HEADER_DIRENT
AC_HEADER_STDC
AC_HEADER_SYS_WAIT
AC_CHECK_HEADERS([fcntl.h limits.h stdint.h stdlib.h string.h strings.h sys/ioctl.h unistd.h sys/vfs.h zlib.h])
AC_CHECK_HEADER([curl/curl.h],,AC_MSG_ERROR([[Cannot find curl/curl.h]]))
dnl AC_CHECK_HEADER([curl/easy.h],,AC_MSG_ERROR([[Cannot find curl/easy.h]]))

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_TYPE_MODE_T
AC_TYPE_PID_T
AC_TYPE_SIZE_T
AC_TYPE_SSIZE_T
AC_HEADER_TIME
AC_STRUCT_TM
AC_TYPE_UINT32_T

# Checks for functions.
AC_FUNC_CLOSEDIR_VOID
AC_FUNC_FORK
AC_FUNC_LSTAT
AC_FUNC_LSTAT_FOLLOWS_SLASHED_SYMLINK
AC_FUNC_MALLOC
AC_FUNC_MEMCMP
AC_FUNC_MKTIME
AC_FUNC_MMAP
AC_FUNC_REALLOC
AC_FUNC_SELECT_ARGTYPES
AC_FUNC_STAT
AC_FUNC_STRNLEN
AC_FUNC_VPRINTF
AC_CHECK_FUNCS([bzero dup2 ftruncate gettimeofday mkdir pow select strchr strdup strerror strncasecmp strstr rmdir])

# Time to substitute and generate the files
AC_CONFIG_FILES([Makedefs
                 clc/eucadmin/bin/euca_conf:clc/eucadmin/bin/euca_conf.in
                 clc/eucadmin/setup.cfg.template:clc/eucadmin/setup.cfg.template.in
                 clc/eucadmin/eucadmin/__init__.py:clc/eucadmin/eucadmin/__init__.py.in
                 clc/modules/bootstrap/src/main/native/arguments.ggo:clc/modules/bootstrap/src/main/native/arguments.ggo.in
                 tools/eucalyptus-cc:tools/eucalyptus-cc.in
                 tools/eucalyptus-cloud:tools/eucalyptus-cloud.in
                 tools/eucalyptus-nc:tools/eucalyptus-nc.in])

AC_DEFINE_UNQUOTED([EUCA_VERSION], "$EUCA_VERSION", [Version of Eucalyptus])

AC_SUBST(EUCA_VERSION)
AC_SUBST(TOP)
AC_SUBST(ANT)
AC_SUBST(RM)
AC_SUBST(WSDL2C)
AC_SUBST(JAVA_HOME)
AC_SUBST(APACHE2_MODULE_DIR)
AC_SUBST(AXIS2C_HOME)
AC_SUBST(AXIS2C_SERVICES)
AC_SUBST(AXIS2_HOME)
AC_SUBST(LIBVIRT_HOME)
AC_SUBST(VDDK_HOME)
AC_SUBST(CFLAGS)
AC_SUBST(LDFLAGS)
AC_SUBST(LIBS)
AC_SUBST(INCLUDES)
AC_SUBST(EXTENSIONS)
AC_SUBST(INTERFACE_THEME)
AC_SUBST(PYTHON)



AC_OUTPUT
