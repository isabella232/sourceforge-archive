ifndef SUBDIRS
ALLSUBDIRS := $(strip $(patsubst %/,%,$(filter-out idl/ CVS/ RCS/ ./,$(filter %/,$(shell /bin/ls -Fd *)))))
OKSUBDIRS := $(filter-out $(EXCLUDE),$(ALLSUBDIRS))
SUBDIRS := $(filter $(patsubst %/Makefile,%,$(wildcard $(addsuffix /Makefile,$(ALLSUBDIRS)))),$(OKSUBDIRS))
NOMKSUBDIRS := $(filter-out $(patsubst %/Makefile,%,$(wildcard $(addsuffix /Makefile,$(ALLSUBDIRS)))),$(OKSUBDIRS))
else
ifndef SUBDIRCHECK
NOSUBDIRS := $(filter-out $(wildcard $(SUBDIRS)),$(SUBDIRS))
NOMKSUBDIRS := $(filter-out $(NOSUBDIRS) $(patsubst %/Makefile,%,$(wildcard $(addsuffix /Makefile,$(SUBDIRS)))),$(SUBDIRS))
SUBDIRS := $(patsubst %/Makefile,%,$(wildcard $(addsuffix /Makefile,$(SUBDIRS))))
SUBDIRCHECK := done
endif
endif
#help var:	SUBDIRS 	List of dirs to recurse into - defaults to
#help     			all bar idl, ISS, RCS and any listed in
#help     			var:EXCLUDE

ifneq ($(TARGET),)
# Stuff to do if TARGET is defined, i.e. we are recursing...
unexport SUBDIRS
unexport NOMKSUBDIRS
unexport NOSUBDIRS
unexport SUBDIRCHECK

.PHONY : $(SUBDIRS) $(NOMKSUBDIRS) $(NOSUBDIRS) recurse

recurse : $(SUBDIRS)
recurse : $(NOMKSUBDIRS)
recurse : $(NOSUBDIRS)

$(SUBDIRS) :
	@echo "$(MAKE) -C $@ $(TARGET)"
	@$(MAKE) -C $@ TARGET= $(TARGET)
$(NOMKSUBDIRS) :
	@echo "$(shell tput bold)WARNING: $@ has no Makefile!$(shell tput rmso)"
$(NOSUBDIRS) :
	@echo "$(shell tput bold)WARNING: SUBDIR $@ does not exist!$(shell tput rmso)"
else
# Make sure the recursive make will see it if the user defined it.
export SUBDIRS
export NOMKSUBDIRS
export NOSUBDIRS
export SUBDIRCHECK
endif
