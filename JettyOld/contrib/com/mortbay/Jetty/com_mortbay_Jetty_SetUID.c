#include <jni.h>
#include "com_mortbay_Jetty_SetUID.h"
#include <unistd.h>

JNIEXPORT void JNICALL Java_com_mortbay_Jetty_SetUID_doSetUID
    (JNIEnv * env, jclass class, jint uid)
{
    setuid(uid);
}
