#Point this to your JDK 1.2 root directory
JDK=c:\jdk1.3

#Executable Name
EXE=jettysvc.exe

all: $(EXE)

CFLAGS=-MT -I$(JDK)\include -I$(JDK)\include\win32

.c.obj:
    cl -c $(CFLAGS) $*.c

jettysvc.obj: jettysvc.c service.h

service.obj: service.c service.h

parseargs.obj: parseargs.c parseargs.h

registry.obj: registry.c registry.h

jettysvc_objs=jettysvc.obj service.obj parseargs.obj registry.obj

jettysvc_libs=advapi32.lib shell32.lib $(JDK)\lib\jvm.lib

$(EXE): $(jettysvc_objs)
    link -OUT:$(EXE) $(jettysvc_objs) $(jettysvc_libs)

