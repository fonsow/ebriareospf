/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class nemo_it_unipr_netsec_netfilter_NetfilterQueue */

#ifndef _Included_nemo_it_unipr_netsec_netfilter_NetfilterQueue
#define _Included_nemo_it_unipr_netsec_netfilter_NetfilterQueue
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     nemo_it_unipr_netsec_netfilter_NetfilterQueue
 * Method:    open
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_nemo_it_unipr_netsec_netfilter_NetfilterQueue_open
        (JNIEnv *, jobject);

/*
 * Class:     nemo_it_unipr_netsec_netfilter_NetfilterQueue
 * Method:    close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_nemo_it_unipr_netsec_netfilter_NetfilterQueue_close
(JNIEnv *, jobject, jlong);

/*
 * Class:     nemo_it_unipr_netsec_netfilter_NetfilterQueue
 * Method:    run
 * Signature: (JILnemo/it/unipr/netsec/netfilter/PacketHandler;)I
 */
JNIEXPORT jint JNICALL Java_nemo_it_unipr_netsec_netfilter_NetfilterQueue_run
        (JNIEnv *, jobject, jlong, jint, jobject);

#ifdef __cplusplus
}
#endif
#endif