#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.
#

# Usage:
#   configure   [--with-axis2c=<dir>]
#		[--with-axis2=<dir>]
#		[--with-libvirt=<dir>]
#		[--with-interface-theme=<name>]
#		[--enable-appliance-store]
#               [--enable-debug]

AC_PREREQ(2.61)
AC_INIT(Eucalyptus,
	m4_esyscmd([cat VERSION| tr -d '\n']),
	support@open.eucalyptus.com)
AC_CONFIG_SRCDIR([wsdl/eucalyptus_nc.wsdl])
AC_CONFIG_HEADER([util/eucalyptus-config.h])
AC_PREFIX_DEFAULT([/opt/eucalyptus/])
AC_USE_SYSTEM_EXTENSIONS
EUCA_VERSION="`cat VERSION`"

# let's figure out where is the source tree
if test ${srcdir} = "." ; then
	TOP=`pwd`
else
	TOP=${srcdir}
fi

# variables we'll need later on
AXIS2C_HOME="${AXIS2C_HOME}"
AXIS2_HOME="${AXIS2_HOME}"
AXIS2C_SERVICES=""
LIBVIRT_HOME="${LIBVIRT_HOME}"
INTERFACE_THEME="eucalyptus"
EXTENSIONS=""
ANT=""
JAVA=""
WSDL2C=""
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
        [  --with-axis2c=<dir>                  where axis2c is installed],
        [AXIS2C_HOME="${withval}"])
AC_ARG_WITH(axis2c-services,
        [  --with-axis2c-services=<dir>         where axis2c services are installed],
        [AXIS2C_SERVICES="${withval}"])
AC_ARG_WITH(axis2,
        [  --with-axis2=<dir>                   where axis2 is installed],
        [AXIS2_HOME="${withval}"])
AC_ARG_WITH(libvirt,
        [  --with-libvirt=<dir>                 where libvirt is installed],
        [LIBVIRT_HOME="${withval}"])
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

# Fix the paths for includes and libraries
if test -n "${AXIS2C_HOME}" ; then
	if test -d "${AXIS2C_HOME}"/include ; then
		for x in `ls ${AXIS2C_HOME}/include`; do
			INCLUDES="${INCLUDES} -I${AXIS2C_HOME}/include/$x"
		done
		LIBS="-L${AXIS2C_HOME}/lib ${LIBS}"
	fi
	if test -d ${AXIS2C_HOME}/modules/rampart ; then
		LIBS="-L${AXIS2C_HOME}/modules/rampart ${LIBS}"
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
AC_PATH_PROG(RM, rm)

# let's search for WSDL2C.sh
# we cannot use AC_PROG_PATH because WSDL2C.sh is not executable
#AC_PATH_PROG([WSDL2C], WSDL2C.sh,,$AXIS2C_HOME/bin/tools/wsdl2c:$PATH)
WSDL2C=$AXIS2C_HOME/bin/tools/wsdl2c/WSDL2C.sh
ALT_WSDL2C=$AXIS2C_HOME/WSDL2C.sh
AC_CHECK_FILE($WSDL2C,[HAVE_WSDL2C=yes])
AC_CHECK_FILE($ALT_WSDL2C,[HAVE_ALT_WSDL2C=yes])
# check if we found either of the possible WSDL2Cs
if test -z "$HAVE_WSDL2C$HAVE_ALT_WSDL2C" ; then
    AC_MSG_ERROR([Cannot find WSDL2C.sh!])
fi
# made it here: we found one! -- set WSDL2C if the default location is wrong
if test -n "$HAVE_ALT_WSDL2C" ; then
    WSDL2C=$ALT_WSDL2C
fi


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

# now let's test we have jni.h
if test ! -e $JAVA_HOME/include/jni.h ; then
	AC_MSG_ERROR([Cannot find jni.h in $JAVA_HOME: do you have a JDK installed?])
fi

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
AC_CHECK_LIB([pthread],[main])
AC_CHECK_LIB([rt],[main])
AC_CHECK_LIB([virt],[main],true,AC_MSG_ERROR([Cannot find libvirt!]))
AC_CHECK_LIB([curl],[main],true,AC_MSG_ERROR([Cannot find libcurl!]))
AC_CHECK_LIB([z],[inflate])
AC_CHECK_LIB([cap],[cap_from_name],,AC_MSG_WARN([Cannot find sufficiently recent libcap will not use it]))

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
		tools/eucalyptus-cc:tools/eucalyptus-cc.in
		tools/eucalyptus-nc:tools/eucalyptus-nc.in
		tools/euca_conf:tools/euca_conf.in
		tools/eucalyptus-cloud:tools/eucalyptus-cloud.in])

AC_DEFINE_UNQUOTED(EUCA_VERSION, "$EUCA_VERSION")

AC_SUBST(EUCA_VERSION)
AC_SUBST(TOP)
AC_SUBST(ANT)
AC_SUBST(RM)
AC_SUBST(WSDL2C)
AC_SUBST(JAVA_HOME)
AC_SUBST(AXIS2C_HOME)
AC_SUBST(AXIS2C_SERVICES)
AC_SUBST(AXIS2_HOME)
AC_SUBST(LIBVIRT_HOME)
AC_SUBST(CFLAGS)
AC_SUBST(LDFLAGS)
AC_SUBST(LIBS)
AC_SUBST(INCLUDES)
AC_SUBST(EXTENSIONS)
AC_SUBST(INTERFACE_THEME)



AC_OUTPUT
