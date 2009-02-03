#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.
#
# $Id: configure.ac,v 1.4 2008-12-24 18:28:07 nurmi Exp $

# Usage:
#   configure   [--with-axis2c=<dir>]
#		[--with-axis2=<dir>]
#		[--with-libvirt=<dir>]
#               [--enable-debug]

AC_PREREQ(2.61)
AC_INIT(Eucalyptus, 
	m4_esyscmd([cat VERSION| tr -d '\n']), 
	support@eucalyptus.cs.ucsb.edu)
AC_CONFIG_SRCDIR([wsdl/eucalyptus_nc.wsdl])
AC_CONFIG_HEADER([util/config.h])
AC_PREFIX_DEFAULT([/opt/eucalyptus/])
AC_USE_SYSTEM_EXTENSIONS
VERSION="`cat VERSION`"

# let's figure out where is the source tree
if test ${srcdir} = "." ; then
	TOP=`pwd`
else
	TOP=${srcdir}
fi

# variables we'll need later on 
AXIS2C_HOME="${AXIS2C_HOME}"
AXIS2_HOME="${AXIS2_HOME}"
LIBVIRT_HOME="${LIBVIRT_HOME}"
ANT=""
JAVA=""
java_min_version="1.6.0"
ant_min_version="1.6.5"

# these are for large files (>2GB)
LDFLAGS="`getconf LFS64_LDFLAGS` $LDFLAGS"
LIBS="`getconf LFS64_LIBS` $LIBS"
CFLAGS="`getconf LFS64_CFLAGS`"

# compile options
CFLAGS="${CFLAGS} -Wall -Wno-unused-variable -fPIC -DHAVE_CONFIG_H"
INCLUDES="${INCLUDES} -I. -I.. -Igenerated -I${TOP}/storage -I${TOP}/node -I${TOP}/util -I${TOP}/net"

# Arguments checking
AC_ARG_WITH(axis2c, 
        [  --with-axis2c=<dir>     where axis2c is installed],
        [AXIS2C_HOME="${withval}"])
AC_ARG_WITH(axis2, 
        [  --with-axis2=<dir>      where axis2 is installed],
        [AXIS2_HOME="${withval}"])
AC_ARG_WITH(libvirt, 
        [  --with-libvirt=<dir>    where libvirt is installed],
        [LIBVIRT_HOME="${withval}"])
AC_ARG_ENABLE(debug,
        [  --enable-debug          include debugging info when compiling],
                [if test "${enableval}" != "no"; then
                        CFLAGS="$CFLAGS -g -DDEBUG"
                fi])

# Fix the paths for includes and libraries
if test -n "${AXIS2C_HOME}" ; then
	if test -d "${AXIS2C_HOME}"/include ; then
		for x in `ls ${AXIS2C_HOME}/include`; do 
			INCLUDES="${INCLUDES} -I${AXIS2C_HOME}/include/$x"
		done
		LIBS="-L${AXIS2C_HOME}/lib ${LIBS}"
	fi
fi
if test -n "${LIBVIRT_HOME}" ; then
	if test -d "${LIBVIRT_HOME}"/include ; then
		INCLUDES="${INCLUDES} -I${LIBVIRT_HOME}/include"
		LIBS="-L${LIBVIRT_HOME}/lib ${LIBS}"
	fi
fi
	

# Checks for programs.
AC_PROG_AWK
AC_PROG_CC
AC_PROG_INSTALL
AC_PROG_MAKE_SET

# we need JAVA_HOME 
if test -z "$JAVA_HOME" ; then
	AC_MSG_ERROR([JAVA_HOME is not defined!])
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
export JAVA_HOME
ant_version=`$ANT -version 2>&1 | grep "Ant version" | \
	sed -e 's/.*Ant version \([[0-9.]]*\).*/\1/'`
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
AC_CHECK_LIB([omxmlsec],[main],,AC_MSG_ERROR([Cannot find libomxmlsec!]))
AC_CHECK_LIB([oxstokens],[main],,AC_MSG_ERROR([Cannot find liboxstokens!]))
AC_CHECK_LIB([pthread],[main])
AC_CHECK_LIB([rt],[main])
AC_CHECK_LIB([virt],[main],true,AC_MSG_ERROR([Cannot find libvirt!]))
AC_CHECK_LIB([curl],[main],true,AC_MSG_ERROR([Cannot find libcurl!]))

# Checks for header files.
AC_HEADER_DIRENT
AC_HEADER_STDC
AC_HEADER_SYS_WAIT
AC_CHECK_HEADERS([fcntl.h limits.h stdint.h stdlib.h string.h strings.h sys/ioctl.h unistd.h sys/vfs.h])
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
		eucalyptus-${VERSION}-1.spec:eucalyptus.spec.in 
		tools/eucalyptus:tools/eucalyptus.in])

AC_DEFINE_UNQUOTED(VERSION, "$VERSION")

AC_SUBST(VERSION)
AC_SUBST(TOP)
AC_SUBST(ANT)
AC_SUBST(JAVA_HOME)
AC_SUBST(AXIS2C_HOME)
AC_SUBST(AXIS2_HOME)
AC_SUBST(LIBVIRT_HOME)
AC_SUBST(CFLAGS)
AC_SUBST(LDFLAGS)
AC_SUBST(LIBS)
AC_SUBST(INCLUDES)



AC_OUTPUT
