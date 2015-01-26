SUMMARY = "The ADAPTIVE Communication Environment (ACE)"
HOMEPAGE="http://www.cs.wustl.edu/~schmidt/ACE.html"
SECTION = "DOC"
LICENSE = "GPLv2"

DEPENDS += "ace-native libtool openssl ncurses zlib libtirpc"
DEPENDS_class-native = ""

LIC_FILES_CHKSUM  = "file://COPYING;beginline=4;endline=5;md5=16bac2a73c8d1169090ba367801a14a1"

SRC_URI = "ftp://download.dre.vanderbilt.edu/previous_versions/ACE-src-${PV}.tar.bz2 \
        file://ACE.mwc \
"

SRC_URI[md5sum] = "e7bd1e3487d054628a1bcfa9708d85f3"
SRC_URI[sha256sum] = "772e0075b986d7c82e4a32d96a130492a3cebb32eb5b18444f9a5612c5169b5b"

S = "${WORKDIR}/ACE_wrappers"
inherit autotools-brokensep pkgconfig

CONFIGUREOPTS=" --build=${BUILD_SYS} \
--host=${HOST_SYS} \
--target=${TARGET_SYS} \
--prefix=${prefix} \
--exec_prefix=${exec_prefix} \
--bindir=${bindir} \
--sbindir=${sbindir} \
--libexecdir=${libexecdir} \
--datadir=${datadir} \
--sysconfdir=${sysconfdir} \
--sharedstatedir=${sharedstatedir} \
--localstatedir=${localstatedir} \
--libdir=${libdir} \
--includedir=${includedir} \
--oldincludedir=${oldincludedir} \
--infodir=${infodir} \
--mandir=${mandir} \
--disable-silent-rules "

CLEANBROKEN = "1"
LEAD_SONAME = "libACE-[0-9.]*.so"

do_configure () {
    export ACE_ROOT=${S}
    export MPC_ROOT=${S}/MPC
    export LD_LIBRARY_PATH=${S}/lib
    export TAO_ROOT=$ACE_ROOT/TAO
    export CIAO_ROOT=$TAO_ROOT/CIAO
    export DANCE_ROOT=$TAO_ROOT/DAnCE

    echo "" > ${S}/ace/config.h

    cat >>${S}/ace/config.h << EOF
#include "ace/config-linux.h"
EOF

    cat >${S}/include/makeinclude/platform_macros.GNU <<EOF
optimize = 0
include include/makeinclude/platform_linux.GNU
EOF

    cp ${WORKDIR}/ACE.mwc ${S}/.
    cd ${S}
    ./bin/mwc.pl -type automake -features debug=1,ssl=1,cidl=0,thread=1 ACE.mwc
    echo "AC_CONFIG_MACRO_DIRS([m4])" >> ${S}/configure.ac
    sed -i 's/AM_CONDITIONAL(BUILD_TESTS, true)/AM_CONDITIONAL(BUILD_TESTS, false)/' ${S}/configure.ac
    sed -i 's/AM_CONDITIONAL(BUILD_EXAMPLES, true)/AM_CONDITIONAL(BUILD_EXAMPLES, false)/' ${S}/configure.ac

    libtoolize --force
    aclocal
    automake --force-missing --add-missing
    autoconf
    sed -i 's/-rpath \$libdir//' configure
    ./configure ${CONFIGUREOPTS} ${EXTRA_OECONF}
}

do_compile () {
    # copy gpref from the native ace build
    if [ "${PN}" != "ace-native" ]; then
        cp ${STAGING_BINDIR_NATIVE}/ace-${PV}/ace_gperf ${S}/bin/.
    fi

    export ACE_ROOT=${S}
    export SSL_ROOT=${STAGING_BASE_DIR}
    export MPC_ROOT=${S}/MPC
    export LD_LIBRARY_PATH=${S}/lib
    make LDFLAGS="${LDFLAGS} -lrt -ldl -lpthread -L${STAGING_LIBDIR}" ACE_ROOT=${S}
}

do_compile_class-native () {
    export ACE_ROOT=${S}
    export SSL_ROOT=${STAGING_INCDIR}
    export MPC_ROOT=${S}/MPC
    export LD_LIBRARY_PATH=${S}/lib
    sed -i 's/DIST_SUBDIRS =.*/DIST_SUBDIRS = \. ace apps bin/' ${S}/Makefile
    sed -i 's/websvcs examples performance-tests tests.*//' ${S}/Makefile
    sed -i 's/DIST_SUBDIRS =.*/DIST_SUBDIRS = \. src/' ${S}/apps/gperf/Makefile

    make LDFLAGS="${LDFLAGS} -lrt -ldl -lpthread" ACE_ROOT=${S} 
    cd apps/gperf
    make LDFLAGS="${LDFLAGS} -lrt -ldl -lpthread" ACE_ROOT=${S} 
}

do_install () {
    #INSTLIBS=`ls ace/.lib/libACE*.so.*`
    sed -i 's/ace_wchar.inl//' ace/Makefile
    make install DESTDIR=${D}
}

do_install_class-native () {
    install -d ${D}${bindir}/ace-${PV}
    install -m 755 apps/gperf/src/.libs/ace_gperf ${D}${bindir}/ace-${PV}/
}

BBCLASSEXTEND = "native"
