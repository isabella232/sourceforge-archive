# Makefile to locate the ..'s necessary to go back to a given directory

#help var:	SRCPATH 	set by global make. Path from root of src tree
#help var:	CLASSROOT	set by global make. Path to root of src tree
#help     			(e.g ../..)
#help var:	PACKAGE 	set by global make. Package for current dir


PWD := $(shell pwd)
MYCDPATH := ..
PACKAGE := $(notdir $(PWD))
SRCPATH := $(notdir $(PWD))

ifndef LOOKINGFOR
LOOKINGFOR := .
endif

ifeq ($(LOOKINGFOR),.)
RESULT := $(PWD)
else
ifneq ($(notdir ${PWD}),${LOOKINGFOR})
include $(MKFILEPATH)/FindPath-Recurse.mk
endif
RESULT := ${MYCDPATH}
endif
