###################################################################
# Base Makefile for all MortBay make file
# Use 'make help' to see what it can do
###################################################################


ifndef MKFILEPATH
MKFILEPATH := $(JETTY_HOME)/etc
endif

ifndef JAVA_PACKAGES
JAVA_PACKAGES := /usr/local/java.packages
endif
ifndef JDK_HOME
JDK_HOME := /usr/local/jdk
endif

# What is the default root of our java source tree.
ifndef ROOTNAME
ROOTNAME := .
endif
#help var:	ROOTNAME	Root of java source tree. Defaults to .

# default
all:

include $(MKFILEPATH)/Recurse.mk


# Recursive targets.
RECURSIVE := rall rclean rtests install ridl rnative rnodebug ralljava
RECURSIVETARGETS := $(addsuffix .recurse,$(RECURSIVE))
.PHONY : $(RECURSIVE) $(RECURSIVETARGETS)
ifneq ($(SUBDIRS),)
$(RECURSIVE) : % : %.recurse
$(RECURSIVETARGETS) :
	@$(MAKE) -f $(MKFILEPATH)/Recurse.mk recurse TARGET=$(patsubst %.recurse,%,$@)
endif

JAVAC := ${JDK_HOME}/bin/javac
JAVA := ${JDK_HOME}/bin/java
JAVAH := ${JDK_HOME}/bin/javah
NATIVEOPTS := -I${JDK_HOME}/include -I${JDK_HOME}/include/solaris

SUFFIXES := $(SUFFIXES) .java .class
# Java files
#help var:	JAVACOPTIONS	Options passed to javac
%.class: %.java
	$(JAVAC) ${JAVACOPTIONS} $<
# Lex files
%.java: %.lex
	$(JAVA) JavaLex.JavaLex $<
	mv ${<}.java $@
# gram files
%Parser.java %Token.java : %.gram
	$(JAVA) sbktech.tools.jell.driver -tokenFile ${*}Token.java \
		-tokenPackage $(PACKAGE) -tokenClass ${*}Token \
		-parserFile ${*}Parser.java $<
# idl files
idl/%.gen : %.idl
	if [ ! -d idl ];then mkdir idl;fi
	idlgen -fclient -fserver -j $(CLASSROOT) -J $@ $<
	make -C idl -f $(MKFILEPATH)/Global.mk

# Determine where we are relative to the root.
LOOKINGFOR := $(ROOTNAME)
include $(MKFILEPATH)/FindPath.mk
CLASSROOT := ${RESULT}

# Fix class path for compiles
CLASSPATH := $(CLASSROOT):${CLASSPATH}
export CLASSPATH

# Generated files....
IDLFILES := $(filter-out $(EXCLUDE),$(wildcard *.idl))
GENIDL := $(patsubst %.idl,idl/%.gen,$(IDLFILES))
GENLEX := $(patsubst %.lex,%.java,$(filter-out $(EXCLUDE),$(wildcard *.lex)))
GRAMFILES := $(filter-out $(EXCLUDE),$(wildcard *.gram))
GENGRAM := $(patsubst %.gram,%Parser.java,$(GRAMFILES))
GENGRAM += $(patsubst %.gram,%Token.java,$(GRAMFILES))
# Full list of class files
NONGENJAVAFILES := $(filter-out $(EXCLUDE),$(wildcard *.java))
GENJAVAFILES := $(filter-out $(EXCLUDE),$(GENGRAM) $(GENLEX))
JAVAFILES := $(NONGENJAVAFILES) $(GENJAVAFILES)
NONGENCLASSES := $(patsubst %.java,%.class,$(NONGENJAVAFILES))
GENCLASSES := $(patsubst %.java,%.class,$(GENJAVAFILES))
CLASSES := $(patsubst %.java,%.class,$(JAVAFILES))

#help var:	EXCLUDE 	List of files/directories to ignore when
#help     			looking for .java, .lex, .gram, .idl files to
#help     			build or subdirs to recurse into

#help 
#help target:	all     	The default target - builds everything.
#help target:	rall    	Rescursively build everything.
all: $(CLASSES) $(GENIDL) $(GENLEX) $(GENGRAM)
rall : all

# generate the idl first...
$(CLASSES) : $(GENIDL)

# generate any parsers before lexers...
ifneq ($(GENGRAM),)
ifneq ($(GENLEX),)
$(GENLEX) : $(GENGRAM)
endif
endif
# Generate generated code before normal compiles.
ifneq ($(NONGENCLASSES),)
ifneq ($(GENJAVAFILES),)
$(NONGENCLASSES) : $(GENJAVAFILES)
endif
endif

#help 
#help target:	idl     	Just build any idl interfaces
#help target:	ridl    	Recursively build all idl interfaces
idl : $(GENIDL)
.PHONY : idl
ridl : idl

# Running tests...
#help 
#help var:	TESTS   	List of files containing test harnesses. If
#help     			end with .java, will be run with java,
#help     			else will be executed.
#help target:	tests   	Run all test harnesses
#help target:	rtests  	Recursively run all tests
rtests : tests
.PHONY : tests

ifneq (${TESTS},)
#help var:	JAVAOPTIONS	If defined, passed to java
export JAVAOPTIONS

JTESTS := $(filter %.java,${TESTS})
SHTESTS := $(filter-out %.java,${TESTS})
JTESTPHONIES := $(patsubst %.java, %.test,$(JTESTS))
SHTESTPHONIES := $(addsuffix .test,$(SHTESTS))
tests : $(JTESTPHONIES) $(SHTESTPHONIES)
.PHONY : $(JTESTPHONIES) $(SHTESTPHONIES)
$(SHTESTPHONIES) : all
	$(patsubst %.test,%,$@)
$(JTESTPHONIES) : all
	$(JAVA) $(JAVAOPTIONS) $(patsubst %.test,$(PACKAGE).%,$@)

else
tests : all
endif

# Building native methods
#help 
#help var:	NATIVE  	List of java files containing native methods
#help var:	NATIVECLASSES	List of java classes to build c stub files
#help     	        	from. These classes do not have to be in the
#help     	        	current directory, simply in the CLASSPATH
#help     	        	somewhere. 
#help var:	NATIVEINC	List of java files to build c include files
#help var:	NATIVECLASSINCS	List of java classes to build c include files
#help     	        	from. These classes do not have to be in the
#help     	        	current directory, simply in the CLASSPATH
#help     	        	somewhere. 
#help var:	NATIVELIB	Stem name of native library to build
#help target:	native  	Build native libs in this directory. This
#help     	        	builds the library lib$(NATIVELIB).so
#help     	        	(on solaris!) from all the .cc and .c
#help     	        	files in the current directory, first
#help     	        	building headers from all of the $(NATIVE)
#help     	        	classes.
#help target:	rnative 	Recursive native
#help 
#help var:	NSNATIVE  	If defined, causes native libraries to be
#help     			built as Netscape plugins instead of suns JVM
#help     			native libraries. Variables NATIVEINC NATIVE
#help     			and NATIVELIB used as normal
ifneq ($(NATIVE)$(NATIVEINC),)
NATIVESRCCLASSES := $(addprefix $(PACKAGE).,$(patsubst %.java,%,$(NATIVE)))
NATIVESRCINCCLASSES := \
	$(addprefix $(PACKAGE).,$(patsubst %.java,%,$(NATIVEINC)))
NATIVESRCINCS := \
	$(addsuffix .h,$(subst .,_,$(NATIVESRCCLASSES) $(NATIVESRCINCCLASSES)))
NATIVESRCSTUBS := \
	$(addsuffix .c,$(subst .,_,$(NATIVESRCCLASSES)))
$(NATIVESRCINCS) : $(subst .,_,$(PACKAGE))_%.h : %.java
	$(JAVAH) $(addprefix $(PACKAGE).,$(patsubst %.java,%,$^))
	@touch $@
$(NATIVESRCSTUBS) : $(subst .,_,$(PACKAGE))_%.c : %.java
	$(JAVAH) -stubs $(addprefix $(PACKAGE).,$(patsubst %.java,%,$^))
	@touch $@
rnative : native
NATIVECLASSINCSH = $(addsuffix .h,$(subst .,_,$(NATIVECLASSINCS)))
NATIVECLASSESSTUBS = $(addsuffix .c,$(subst .,_,$(NATIVECLASSES)))
$(NATIVECLASSINCSH) :
	$(JAVAH) $(subst _,.,$(subst .h,,$@))
$(NATIVECLASSESSTUBS) :
	$(JAVAH) -stubs $(subst _,.,$(subst .c,,$@))
# If NATIVE is defined, try and build the headers
native : $(NATIVESRCINCS)
all : native
NATIVEGENFILES := $(NATIVECLASSESSTUBS) $(NATIVECLASSINCSH) $(NATIVESRCINCS) $(NATIVESRCSTUBS)
clean ::
	$(RM) $(NATIVEGENFILES)
endif
ifneq ($(NATIVELIB),)
ARCH := $(shell uname -p)
# Be careful - sometimes the stubs get built but need to be excluded...
NATIVELIBSRCC := $(patsubst %.c,$(ARCH)/%.o,$(filter-out $(EXCLUDE) $(NATIVECLASSESSTUBS) $(NATIVESTUBS),$(wildcard *.c))) \
		$(patsubst %.c,$(ARCH)/%.o,$(filter-out $(EXCLUDE),$(NATIVESTUBS) $(NATIVECLASSESSTUBS)))
NATIVELIBSRCCC := $(patsubst %.cc,$(ARCH)/%.o,$(filter-out $(EXCLUDE),$(wildcard *.cc)))
NATIVELIBSRC := $(NATIVELIBSRCCC) $(NATIVELIBSRCC)
ifneq ($(NATIVELIBSRC),)
CFLAGS += -fpic $(NATIVEOPTS)
CC := gcc
CXXFLAGS += $(NATIVEOPTS)
CXX := g++
$(NATIVELIBSRC) : $(NATIVEGENFILES) $(ARCH)
$(ARCH) :
	mkdir $@
ifneq ($(NATIVELIBSRCC),)
$(NATIVELIBSRCC) : $(ARCH)/%.o : %.c
	$(CC) -c -o $@ $(CPPFLAGS) $(CFLAGS) $<
endif
ifneq ($(NATIVELIBSRCCC),)
$(NATIVELIBSRCCC) : $(ARCH)/%.o : %.cc
	$(CXX) -c -o $@ $(CPPFLAGS) $(CXXFLAGS) $<
endif

NATIVELIBNAME = $(ARCH)/lib$(NATIVELIB).so
# If NATIVELIB is defined and there are some sources, build the library as
# well. The sources depend on the generated headers as well.
native : $(NATIVELIBNAME)
clean ::
	$(RM) -r $(ARCH)
all : native
$(NATIVELIBNAME) : $(NATIVELIBSRC)
	$(CC) -G -shared -o $@ $^ $(LDFLAGS) $(LDLIBS)
endif
endif
ifdef NSNATIVE
include $(MKFILEPATH)/NSPlugin.mk
endif

# Installing files - INSTALLROOT and INSTALLFILES can be set in users makefile
# 		     or environment
#help 
#help var:	INSTALLROOT	Root directory where files should be
#help     			installed to - defaults to $CLASSROOT/..
#help var:	BINFILES	Files to install in bin dir
#help var:	LIBFILES	Files to install in lib dir : defaults to all
#help     	        	native libraries.
#help var:	CFGFILES	Config files to install: defaults to *.cfg
#help var:	CLASSFILES	Class files to install - defaults to all class
#help     			files once a make all has been completed.
#help var:      EXCLUDECLASSES  Do not install these classes
#help var:	INSTALLPATH	Path relative to INSTALLROOT to install
#help     			INSTALLFILES to.
#help var:	INSTALLFILES	Extra files to install
#help target:	install-dir 	install CLASSFILES to 
#help                           INSTALLROOT/classes/SRCPATH.
#help     	        	Also installs BINFILES to INSTALLROOT/bin and
#help     	        	LIBFILES to INSTALLROOT/lib, and INSTALLFILES
#help      			to INSTALLROOT/INSTALLPATH. Recursive
#help target:	install  	recursive install-dir

ifndef INSTALLROOT
# If INSTALLROOT not defined, install the files to the root of the src tree
# and make them writeable.
INSTALLROOT := $(CLASSROOT)/..
endif
define installFile
	@if [ ! -d `dirname '$@'` ];\
	  then mkdir -p `dirname '$@'`; \
	fi
	@$(RM) '$@'
	cp '$<' '$@'
	@chmod +w '$@'
endef

ifndef CLASSFILES
CLASSFILES := $(wildcard *.class idl/*.class)
endif

ifdef EXCLUDECLASSES
CLASSFILES := $(filter-out $(EXCLUDECLASSES) , $(CLASSFILES))
endif 

ifndef LIBFILES
LIBFILES := $(NATIVELIBNAME)
endif

# If something to install, install it relative to the INSTALLROOT
CLASSINSTALLDIR := $(INSTALLROOT)/classes/$(SRCPATH)
CLASSINSTALLEDFILES := $(addprefix $(CLASSINSTALLDIR)/,$(CLASSFILES))
BININSTALLEDFILES := $(addprefix $(INSTALLROOT)/bin/,$(BINFILES))
ifndef NSNATIVE
LIBINSTALLDIR := $(INSTALLROOT)/lib/
else
LIBINSTALLDIR := $(INSTALLROOT)/lib/plugins/
endif
LIBINSTALLEDFILES := $(addprefix $(LIBINSTALLDIR),$(LIBFILES))
CFGINSTALLEDFILES := $(addprefix $(INSTALLROOT)/etc/,$(CFGFILES))
OTHERINSTALLEDFILES := $(addprefix $(INSTALLROOT)/$(INSTALLPATH)/,$(INSTALLFILES))
ALLINSTALLEDFILES := $(CLASSINSTALLEDFILES) $(BININSTALLEDFILES) \
		     $(LIBINSTALLEDFILES) $(CFGINSTALLEDFILES) \
		     $(OTHERINSTALLEDFILES)
ifneq ($(ALLINSTALLEDFILES),)
install : install-dir
.PHONY : install-dir
install-dir : $(ALLINSTALLEDFILES)

$(CLASSINSTALLEDFILES) : $(CLASSINSTALLDIR)/% : %
$(BININSTALLEDFILES) : $(INSTALLROOT)/bin/% : %
$(LIBINSTALLEDFILES) : $(LIBINSTALLDIR)% : %
$(CFGINSTALLEDFILES) : $(INSTALLROOT)/etc/% : %
$(OTHERINSTALLEDFILES) : $(INSTALLROOT)/$(INSTALLPATH)/% : %
$(ALLINSTALLEDFILES) :
	$(installFile)
endif

# uudecoding
#help var:	UUFILES		List of uuencode files to decode
#help target:	uufiles		Decode all UUFILES - make all also does this
ifdef UUFILES
UNUUFILES := $(patsubst %.uu,%,$(UUFILES))
all : uufiles
.PHONY : uufiles
uufiles : $(UNUUFILES)
$(UNUUFILES) : % : %.uu
	cd $(@D) && uudecode $(@F).uu
endif

# help target
help :
	@grep -h '^#help' $(MKFILEPATH)/*.mk | sed -e 's/^\#help //' -e 's/^\#help//'

# removing debug statements
#help 
#help target:	nodebug 	Remove debug from non-checked out java files
#help target:	rnodebug	Recursive nodebug
ifneq ($(JAVAFILES),)
.PHONY : nodebug
rnodebug : nodebug
nodebug :
	removeDebug.pl $(JAVAFILES)
endif

# cleaning up
#help 
#help target:	clean   	Remove traces of build
#help target:	rclean  	Recursive clean
rclean : clean
clean ::
	/bin/rm -f idl/* *.class *.o *.so ${GENLEX} ${GENGRAM} \
		${NATIVEINCS} ${NATIVESTUBS} ${UNUUFILES}
	if [ -d idl ];then /bin/rmdir idl;fi

ralljava: alljava
alljava:
	$(JAVAC) ${JAVACOPTIONS} *.java
