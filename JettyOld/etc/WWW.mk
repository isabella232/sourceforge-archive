ifndef INSTALL_DIR
INSTALL_DIR	:= /home/mos/web-test
endif
INSTALL		:= /usr/ucb/install
MUNGE		:= /home/mos/bin/munge_links.pl


# PATH_FROM_ROOT - where in the web heirarchy this tree should be installed
# default undefined - means as is - i.e it will be placed in it's current dir
# tree position directly under the webs erver root
# Unless overriden  from aove - we leave it undefined and install the files as
# they are under the webserver root. 

ifdef PATH_FROM_ROOT
path_from_root_arg := $(PATH_FROM_ROOT)
else
path_from_root_arg := "NULL"
endif

# IMAGE_ROOT - the subdir from the webserver root/path_from_root
# where the images will go. Set to images, but user may over ride if
# they like
ifndef IMAGE_ROOT
IMAGE_ROOT	= images
endif

# Allow the user to define the gifs to install if they wish
ifndef UU_GIFS
UU_GIFS := $(wildcard *.gif.uu)
endif
GIFS := $(patsubst %.uu,%,$(UU_GIFS))

ifndef UU_JPEGS
UU_JPEGS := $(wildcard *.jpg.uu)
endif
GIFS := $(patsubst %.uu,%,$(UU_GIFS))
JPEGS := $(patsubst %.uu,%,$(UU_JPEGS))

ifneq ($(GIFS),)
$(GIFS) :  %.gif : %.gif.uu
	uudecode $@.uu
#
gifs : $(GIFS)
endif

ifneq ($(JPEGS),)
$(JPEGS) : %.jpg : %.jpg.uu
	uudecode $@.uu
#
jpegs : $(JPEGS)
endif
#
#
# rule for installing access control files
ACCESS_CONTROL_FILE = $(wildcard .htaccess)
ROBOT_FILE = $(wildcard robot.txt)

# change to web and  web
OWNER           = mos
GROUP           = mos
MODE            = 0755
#
#
#
include /usr/local/etc/Recurse.mk

# Recursive targets.
RECURSIVE := rclean rinstall
RECURSIVETARGETS := $(addsuffix .recurse,$(RECURSIVE))
.PHONY : $(RECURSIVE) $(RECURSIVETARGETS)
ifneq ($(SUBDIRS),)
$(RECURSIVE) : % : %.recurse
$(RECURSIVETARGETS) :
	@$(MAKE) -f /usr/local/etc/Recurse.mk recurse TARGET=$(patsubst %.recurse,%,$@)
endif

rinstall : install

# LOOKINGFOR := ca, au, or common
# this must ne defined in the relative subtrees
#
include /usr/local/etc/FindPath.mk
WEB-ROOT := ${RESULT}
#
# now get rid of the au, ca, common bit
SRCPATH := $(subst au,,$(SRCPATH))
SRCPATH := $(subst ca,,$(SRCPATH))
SRCPATH := $(subst common,,$(SRCPATH))

.PHONY: install install.test

# Set up the IMAGE files and HTML files to install if they are not
# set explicitely by the user. Note that the GIFS and JPEGS vars define the
# image files, see above
ifndef IMAGE_FILES
IMAGE_FILES := $(GIFS) $(JPEGS)
endif

ifndef HTML_FILES
HTML_FILES := $(wildcard *.html)
endif

INSTALLED_HTML_DIR := $(INSTALL_DIR)
INSTALLED_IMAGE_DIR := $(INSTALL_DIR)

ifeq ($(PATH_FROM_ROOT),)
# STAGE1 := $(INSTALLED_HTML_DIR)/$(PATH_FROM_ROOT)
INSTALLED_IMAGE_DIR := $(INSTALLED_IMAGE_DIR)/$(IMAGE_ROOT)
else
INSTALLED_HTML_DIR := $(INSTALLED_HTML_DIR)/$(PATH_FROM_ROOT)
INSTALLED_IMAGE_DIR := $(INSTALLED_IMAGE_DIR)/$(PATH_FROM_ROOT)/$(IMAGE_ROOT)
endif

ifneq ($(SRCPATH),)
INSTALLED_HTML_DIR := $(INSTALLED_HTML_DIR)/$(SRCPATH)
INSTALLED_IMAGE_DIR := $(INSTALLED_IMAGE_DIR)/$(SRCPATH)
endif
# Now get rid of any double or triple slashes that may be in out install
# paths because of a null PATH_FROM_ROOT or a null (SRCPATH)
# yes it would be nice to use the var substitution function - but it was
# broken at the time : gmake 3.74
INSTALLED_HTML_DIR := $(subst //,/,$(subst ///,/,$(INSTALLED_HTML_DIR)))
INSTALLED_IMAGE_DIR := $(subst //,/,$(subst ///,/,$(INSTALLED_IMAGE_DIR)))

INSTALLED_HTML_FILES := $(addprefix $(INSTALLED_HTML_DIR)/,$(HTML_FILES))
INSTALLED_IMAGE_FILES := $(addprefix $(INSTALLED_IMAGE_DIR)/,$(IMAGE_FILES))

# Now get rid of any double or triple slashes that may have snuck in because
# we are at the root of the tree and SRCPATH (from the recursive make is
# our own dir
INSTALLED_HTML_FILES := $(subst //,/,$(INSTALLED_HTML_FILES))
INSTALLED_IMAGE_FILES := $(subst //,/,$(INSTALLED_IMAGE_FILES))

ifneq ($(ACCESS_CONTROL_FILE),)
INSTALLED_ACCESS_CONTROL_FILE := $(INSTALLED_HTML_DIR)/$(ACCESS_CONTROL_FILE)
#
$(INSTALLED_HTML_DIR)/.htaccess : .htaccess
	-if [ ! -d $(INSTALLED_HTML_DIR) ]; \
        then \
          $(INSTALL) -d -m $(MODE) -o $(OWNER) -g $(GROUP) $(INSTALLED_HTML_DIR);\
        fi
	$(INSTALL) -m $(MODE) -o $(OWNER) -g $(GROUP) $(notdir $@) $(INSTALLED_HTML_DIR);
endif
#
#
ifneq ($(ROBOT_FILE),)
INSTALLED_ROBOT_FILE := $(INSTALLED_HTML_DIR)/$(ROBOT_FILE)
#
$(INSTALLED_HTML_DIR)/robot.txt : robot.txt
	-if [ ! -d $(INSTALLED_HTML_DIR) ]; \
        then \
          $(INSTALL) -d -m $(MODE) -o $(OWNER) -g $(GROUP) $(INSTALLED_HTML_DIR);\
        fi
	$(INSTALL) -m $(MODE) -o $(OWNER) -g $(GROUP) $(notdir $@) $(INSTALLED_HTML_DIR);
endif



$(INSTALLED_HTML_FILES) : $(INSTALLED_HTML_DIR)/%.html : %.html
	-if [ ! -d $(INSTALLED_HTML_DIR) ]; \
        then \
          $(INSTALL) -d -m $(MODE) -o $(OWNER) -g $(GROUP) $(INSTALLED_HTML_DIR);\
        fi
	$(MUNGE) -I $(IMAGE_ROOT) -b $(path_from_root_arg) < $(notdir $@) > $(INSTALLED_HTML_DIR)/$(notdir $@)


$(INSTALLED_IMAGE_FILES) : $(INSTALLED_IMAGE_DIR)/% : %
	-if [ ! -d $(INSTALLED_IMAGE_DIR) ]; \
        then \
          $(INSTALL) -d -o $(OWNER) -g $(GROUP) -m $(MODE) $(INSTALLED_IMAGE_DIR);\
        fi
	$(INSTALL) -o $(OWNER) -g $(GROUP) -m $(MODE) $(notdir $@) $(INSTALLED_IMAGE_DIR)
#
install : $(INSTALLED_HTML_FILES) $(INSTALLED_IMAGE_FILES) $(INSTALLED_ACCESS_CONTROL_FILE) $(INSTALLED_ROBOT_FILE)



install.test :
	@echo "Path from root      : $(PATH_FROM_ROOT)"
	@echo "Install dir         : $(INSTALL_DIR)"
	@echo "SrcPath             : $(SRCPATH)"
	@echo "HTML install dir    : $(INSTALLED_HTML_DIR)"
	@echo "HTMLs to install    : $(INSTALLED_HTML_FILES)"
	@echo "GIF install dir     : $(INSTALLED_IMAGE_DIR)"
	@echo "GIFS to install     : $(INSTALLED_IMAGE_FILES)"
	@echo "Access Control File : $(INSTALLED_ACCESS_CONTROL_FILE)"
	@echo "Robot file          : $(INSTALLED_ROBOT_FILE)"



