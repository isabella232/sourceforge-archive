gcc -I%JAVA_HOME%\include -I%JAVA_HOME%\include\win32 -L. -D_CRTAPI1=__cdecl ..\service.c ..\registry.c ..\parseargs.c ..\jettysvc.c -ljvm -o ..\jettysvc.exe
