###################################################################
# Base Makefile for system admin perl scripts and libraries 
#
# Configurables - things that may be overriddden by the user locally in 
# each makefile. All entries in tye followings ection have sensible
# defaults, but may be overridden if needed. Names are self explainatory
#
###########################################################
#
#
INSTALL := /usr/ucb/install
#
# allow for overrides if they are wanted
#
ifndef MANEXT
MANEXT := 8
endif
#
ifndef USER
OWNER := root
endif
#
ifndef GROUP
GROUP = sysadmin
endif
#
ifndef DIRMODE
MODE = 755
endif
##
ifndef DIRMODE
DIRMODE = 755
endif
#
#
# Will only catch the .pl files but our install rule will work with shell
# files as well - so if the user wants they can use the perl mk file for 
# shell as well .....
ifndef SRC
SRC= $(wildcard *.pl)
endif

ifndef LIB
LIB := $(wildcard *.pm)
endif

ifndef MAN
MANSRC := $(wildcard *.pl)  $(wildcard *.pm)  $(wildcard *.pod)
MAN:= $(patsubst %.pl, %.$(MANEXT), $(MANSRC))
MAN:= $(patsubst %.pod, %.$(MANEXT), $(MAN))
MAN:= $(patsubst %.pm, %.$(MANEXT), $(MAN))
endif

#######################################################################
# This builds all the stuff as packages
# simple - asumes that all the files ina package are in the current directory.
# This is OK for the type of stuff we are building - modular sys admin scripts
# Should be fixed up later on
#
# sort of replaces most of the rest of this makefile, but is more for keeping
# the remote  sites in sync. Packages are nice for that, and we don't have to 
# worry about having the source in two places that way
#
# If they have not defined the files which are in packages, assume that
# all of them go in.
# The user Must define the PKGS which they wish to have built, along with
# the config files which define the package
# They should be named packagename_prototype, and packagename_pkginfo
# copyright is optional, and can just be a shared copyright file
#
# PKGS := 

ifndef PKG_FILES
PKG_FILES = $(SRC) $(LIBS) $(MAN)
endif

#######################################################################
# This is the basic straight install stuff. Used at master site for
# convenience - but not really anywhere else.
#######################################################################

ifndef INSTALL_DIR
INSTALL_DIR := /usr/iss
endif
#
ifndef BINDIR
BINDIR := bin
endif
#
ifndef LIBDIR
LIBDIR := lib/perl
endif
#
ifndef SUBDIRS
SUBDIRS = $(strip $(patsubst %/,%,$(filter-out RCS/ ./,$(filter %/,$(shell /bin/ls -pd *)))))
endif

#
##########################################################
# Rules and directives
##########################################################
#
.PHONY : install install.pl install.man install.pm
#
#
%.$(MANEXT) : %.pod
	build_man.pl $<

%.$(MANEXT) : %.pm
	build_man.pl $<

%.$(MANEXT) : %.pl
	build_man.pl $<

$(INSTALL_DIR)/$(BINDIR)/% : %
	 -if [ ! -d $(INSTALL_DIR)/$(BINDIR) ]; \
        then \
          $(INSTALL) -d -o $(OWNER) -g $(GROUP) -m $(DIRMODE) \
		$(INSTALL_DIR)/$(BINDIR);\
        fi
	$(INSTALL) -o $(OWNER) -g $(GROUP) -m 0775 $(notdir $@) $(INSTALL_DIR)/$(BINDIR)
#
#
$(INSTALL_DIR)/$(LIBDIR)/%.pm : %.pm
	-if [ ! -d $(INSTALL_DIR)/$(LIBDIR) ]; \
        then \
          $(INSTALL) -d -o $(OWNER) -g $(GROUP) -m $(DIRMODE) \
		$(INSTALL_DIR)/$(LIBDIR);\
        fi
	$(INSTALL) -o $(OWNER) -g $(GROUP) -m 0775 $(notdir $@) $(INSTALL_DIR)/$(LIBDIR)
#
#
$(INSTALL_DIR)/man/man$(MANEXT)/%.$(MANEXT) : %.$(MANEXT)
	-if [ ! -d $(INSTALL_DIR)/man/man$(MANEXT) ]; \
        then \
          $(INSTALL) -d -o $(OWNER) -g $(GROUP) -m $(DIRMODE) \
		$(INSTALL_DIR)/man/man$(MANEXT);\
        fi
	$(INSTALL) -o $(OWNER) -g $(GROUP) -m 0644 $(notdir $@) $(INSTALL_DIR)/man/man$(MANEXT)
#
#
man : $(MAN)
#
#
INSTALLED_SRC := $(addprefix $(INSTALL_DIR)/$(BINDIR)/,$(SRC))
#
INSTALLED_LIB := $(addprefix $(INSTALL_DIR)/$(LIBDIR)/,$(LIB))
#
INSTALLED_MAN := $(addprefix $(INSTALL_DIR)/man/man$(MANEXT)/,$(MAN))
#
install.pl : $(INSTALLED_SRC)
#
install.pm : $(INSTALLED_LIB)
#
install.man : $(INSTALLED_MAN)

# install.html :

install : install.man install.pl install.pm recurse
#
#
#
package : $(PKG_FILES)
	if [ ! -d pkg ]; \
	then \
	  mkdir pkg; \
	fi
	for i in $(PKGS);do \
	  ln -s pkginfo_$$i pkginfo; \
	  ln -s prototype_$$i prototype ; \
	pkgmk -o -d pkg; \
	rm pkginfo prototype; \
	done
#
# remove the generated man pages and the packages
#
clean :
	/bin/rm -rf *.8 pkg

