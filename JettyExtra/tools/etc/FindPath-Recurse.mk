# Makefile to recursively include itself until it finds a given directory.
# See FindPath.mk

PWD := $(patsubst %/,%,$(dir ${PWD}))
MYCDPATH := ${MYCDPATH}/..
PACKAGE := $(notdir $(PWD)).$(PACKAGE)
SRCPATH := $(notdir $(PWD))/$(SRCPATH)

#foo := $(shell echo PWD = ${PWD} notdir = $(notdir ${PWD}) 1>&2)

ifneq ($(notdir ${PWD}),${LOOKINGFOR})
include $(MKFILEPATH)/FindPath-Recurse.mk
endif
