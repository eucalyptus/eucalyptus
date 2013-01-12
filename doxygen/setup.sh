#!/bin/bash
#
# This script setups the basic configuration necessary to run doxygen
# on the code. This also test and build the configuration based on the
# installed doxygen version.
#

PrintError()
{
	echo "[$2] $0: $3";
	exit $1;
}

Print()
{
	echo "[$1] $0: $2";
}

projects=( "eucalyptus" "node" "cluster" );

path=`cd ..;pwd`;
cfgfile="";
doxygen=`which doxygen 2> /dev/null`;
if [ -z "$doxygen" ]; then
	PrintError 0 $LINENO "DOXYGEN not installed on this system.";
fi

version=`$doxygen --version`;
Print $LINENO "Found DOXYGEN $version installed on this system.";

havedot="NO";
dotpath=`which dot 2> /dev/null`;
if [ ! -z "$dotpath" ]; then
	havedot="YES";
fi

Aliases="TODO=\"\todo\" FIXME=\"\todo\" fixme=\"\todo\""
EucaSettings="PROJECT_NAME           = "; 
EucaSettings=$(printf "$EucaSettings\nSTRIP_FROM_PATH        = $path ");
EucaSettings=$(printf "$EucaSettings\nSTRIP_FROM_INC_PATH    =  ");
EucaSettings=$(printf "$EucaSettings\nCREATE_SUBDIRS         = YES ");
EucaSettings=$(printf "$EucaSettings\nCASE_SENSE_NAMES       = NO ");
EucaSettings=$(printf "$EucaSettings\nJAVADOC_AUTOBRIEF      = YES ");
EucaSettings=$(printf "$EucaSettings\nQT_AUTOBRIEF           = YES ");
EucaSettings=$(printf "$EucaSettings\nMULTILINE_CPP_IS_BRIEF = YES ");
EucaSettings=$(printf "$EucaSettings\nSEPARATE_MEMBER_PAGES  = YES ");
EucaSettings=$(printf "$EucaSettings\nTAB_SIZE               = 4 ");
EucaSettings=$(printf "$EucaSettings\nOPTIMIZE_OUTPUT_FOR_C  = YES ");
EucaSettings=$(printf "$EucaSettings\nBUILTIN_STL_SUPPORT    = YES ");
EucaSettings=$(printf "$EucaSettings\nEXTRACT_ALL            = YES ");
EucaSettings=$(printf "$EucaSettings\nEXTRACT_PRIVATE        = YES ");
EucaSettings=$(printf "$EucaSettings\nEXTRACT_STATIC         = YES ");
EucaSettings=$(printf "$EucaSettings\nEXTRACT_LOCAL_METHODS  = YES ");
EucaSettings=$(printf "$EucaSettings\nINTERNAL_DOCS          = YES ");
EucaSettings=$(printf "$EucaSettings\nSOURCE_BROWSER         = YES ");
EucaSettings=$(printf "$EucaSettings\nSTRIP_CODE_COMMENTS    = NO ");
EucaSettings=$(printf "$EucaSettings\nREFERENCED_BY_RELATION = YES ");
EucaSettings=$(printf "$EucaSettings\nREFERENCES_RELATION    = YES ");
EucaSettings=$(printf "$EucaSettings\nHTML_DYNAMIC_SECTIONS  = YES ");
EucaSettings=$(printf "$EucaSettings\nGENERATE_DOCSET        = YES ");
EucaSettings=$(printf "$EucaSettings\nGENERATE_QHP           = YES ");
EucaSettings=$(printf "$EucaSettings\nQHP_NAMESPACE          = org.doxygen.Project ");
EucaSettings=$(printf "$EucaSettings\nGENERATE_TREEVIEW      = YES ");
EucaSettings=$(printf "$EucaSettings\nGENERATE_LATEX         = NO ");
EucaSettings=$(printf "$EucaSettings\nCLASS_DIAGRAMS         = NO ");
EucaSettings=$(printf "$EucaSettings\nUML_LOOK               = YES ");
EucaSettings=$(printf "$EucaSettings\nCALL_GRAPH             = YES ");
EucaSettings=$(printf "$EucaSettings\nCALLER_GRAPH           = YES ");
EucaSettings=$(printf "$EucaSettings\nDOT_TRANSPARENT        = YES ");
EucaSettings=$(printf "$EucaSettings\nDOT_MULTI_TARGETS      = YES ");
EucaSettings=$(printf "$EucaSettings\nDOT_FONTNAME           = Helvetica ");
EucaSettings=$(printf "$EucaSettings\nDOT_GRAPH_MAX_NODES    = 500 ");
EucaSettings=$(printf "$EucaSettings\nALPHABETICAL_INDEX     = YES ");
EucaSettings=$(printf "$EucaSettings\nHTML_TIMESTAMP         = YES ");
EucaSettings=$(printf "$EucaSettings\nPAPER_TYPE             = a4 ");
EucaSettings=$(printf "$EucaSettings\nHAVE_DOT               = $havedot ");
EucaSettings=$(printf "$EucaSettings\nDOT_PATH               = $dotpath ");

ProjectSpec="";
ToEvaluate="";
if [ ${version:0:3} == "1.8" ]; then
	layoutfile="html_includes/layoutFileName.xml";
	
	ProjectSpec=$(printf "PROJECT_BRIEF          = a4 ");
	ProjectSpec=$(printf "$ProjectSpec\nPROJECT_LOGO           = ./html_includes/eucalyptus_logo.png ");
	ProjectSpec=$(printf "$ProjectSpec\nHTML_COLORSTYLE_HUE    = 227 ");
	ProjectSpec=$(printf "$ProjectSpec\nHTML_COLORSTYLE_SAT    = 255 ");
	ProjectSpec=$(printf "$ProjectSpec\nHTML_COLORSTYLE_GAMMA  = 115 ");
	ProjectSpec=$(printf "$ProjectSpec\nGENERATE_ECLIPSEHELP   = YES ");
	ProjectSpec=$(printf "$ProjectSpec\nSERVER_BASED_SEARCH    = YES ");
	ProjectSpec=$(printf "$ProjectSpec\nFORCE_LOCAL_INCLUDES   = YES ");
	ProjectSpec=$(printf "$ProjectSpec\nLAYOUT_FILE            = $layoutfile ");
		
	ToEvaluate=$(printf "TCL_SUBST              =  ");
	ToEvaluate=$(printf "$ToEvaluate\nMARKDOWN_SUPPORT       = YES ");
	ToEvaluate=$(printf "$ToEvaluate\nAUTOLINK_SUPPORT       = YES ");
	ToEvaluate=$(printf "$ToEvaluate\nINLINE_GROUPED_CLASSES = NO ");
	ToEvaluate=$(printf "$ToEvaluate\nINLINE_SIMPLE_STRUCTS  = NO ");
	ToEvaluate=$(printf "$ToEvaluate\nSYMBOL_CACHE_SIZE      = 0 ");
	ToEvaluate=$(printf "$ToEvaluate\nLOOKUP_CACHE_SIZE      = 0 ");
	ToEvaluate=$(printf "$ToEvaluate\nEXTRACT_PACKAGE        = NO ");
	ToEvaluate=$(printf "$ToEvaluate\nSTRICT_PROTO_MATCHING  = NO ");
	ToEvaluate=$(printf "$ToEvaluate\nCITE_BIB_FILES         =  ");
	ToEvaluate=$(printf "$ToEvaluate\nFILTER_SOURCE_PATTERNS =  ");
	ToEvaluate=$(printf "$ToEvaluate\nHTML_EXTRA_STYLESHEET  = ");
	ToEvaluate=$(printf "$ToEvaluate\nHTML_EXTRA_FILES       =  ");
	ToEvaluate=$(printf "$ToEvaluate\nHTML_INDEX_NUM_ENTRIES = 100 ");
	ToEvaluate=$(printf "$ToEvaluate\nFORMULA_TRANSPARENT    = YES ");
	ToEvaluate=$(printf "$ToEvaluate\nUSE_MATHJAX            = NO ");
	ToEvaluate=$(printf "$ToEvaluate\nDOT_NUM_THREADS        = 0 ");
	ToEvaluate=$(printf "$ToEvaluate\nINTERACTIVE_SVG        = NO ");
elif [ ${version:0:3} == "1.6" ]; then 
	layoutfile="html_includes/layoutFileName_1_6.xml";

	ProjectSpec=$(printf "SHOW_DIRECTORIES       = YES ");
	ProjectSpec=$(printf "$ProjectSpec\nLAYOUT_FILE            = $layoutfile ");

	ToEvaluate=$(printf "HTML_ALIGN_MEMBERS     = YES ");
	ToEvaluate=$(printf "$ToEvaluate\nUSE_INLINE_TREES       = NO ");
fi

for project in "${projects[@]}"
do
	Print $LINENO "Building DOXYGEN configuration verion $version for the '$project' project.";
	
	inputs="";
	if [ $project == "eucalyptus" ]; then
		inputs="extra_pages/doxygen.txt extra_pages/cluster_project.txt extra_pages/node_project.txt extra_pages/readme.txt extra_pages/changelog.txt extra_pages/install.txt extra_pages/license.txt ../cluster ../node ../gatherlog ../net ../storage ../util";
	elif [ $project == "node" ]; then
		inputs="extra_pages/doxygen.txt extra_pages/node_project.txt extra_pages/readme.txt extra_pages/changelog.txt extra_pages/install.txt extra_pages/license.txt ../node ../net ../storage ../util";
	elif [ $project == "cluster" ]; then
		inputs="extra_pages/doxygen.txt extra_pages/cluster_project.txt extra_pages/readme.txt extra_pages/changelog.txt extra_pages/install.txt extra_pages/license.txt ../cluster ../net ../storage ../util";
	fi
	
	cfgfile=$project".doxyfile";
	doxygen -g $cfgfile 1> /dev/null 2> /dev/null;
	echo "" >> $cfgfile;
	echo "#" >> $cfgfile;
	echo "# Eucalyptus Modified Configuration" >> $cfgfile;
	echo "#" >> $cfgfile;
	echo "" >> $cfgfile;
	echo "INPUT                  = $inputs " >> $cfgfile;
	echo "ALIASES                = $Aliases " >> $cfgfile;
	echo "OUTPUT_DIRECTORY       = ./$project" >> $cfgfile;
	echo "$EucaSettings" >> $cfgfile;
	
	echo "" >> $cfgfile;
	echo "#" >> $cfgfile;
	echo "# Eucalyptus DOXYGEN Version Specific Configuration" >> $cfgfile;
	echo "#" >> $cfgfile;
	echo "" >> $cfgfile;
	echo "$ProjectSpec" >> $cfgfile;
	
	echo "" >> $cfgfile;
	echo "#" >> $cfgfile;
	echo "# Eucalyptus DOXYGEN Configuration To Evaluate" >> $cfgfile;
	echo "#" >> $cfgfile;
	echo "" >> $cfgfile;
	echo "$ToEvaluate" >> $cfgfile;
done
