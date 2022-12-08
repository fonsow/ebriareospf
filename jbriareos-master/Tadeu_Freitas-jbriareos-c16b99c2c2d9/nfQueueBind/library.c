#include "library.h"

#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <jni.h>

#include <linux/netfilter.h>
#include <libnetfilter_queue/libnetfilter_queue.h>


/** Packet buffer size */
#define BUFSIZE 4096   // 4KB

/** Whether running in verbose mode */
int verbose=0;

/** Structure for carrying JNI parameters  for the packet callback */
struct jni_packet_callback_data {
    JNIEnv* env; //  JNI environment
    jbyteArray byte_array; // buffer
    jobject ph_obj; // packet hendler
};

/** Converts a byte array into an hexadecimal string. */
char* toHex(unsigned char* data, int len, char* str)
{	char* hex = "0123456789abcdef";
    int k=0;
    int i;
    for (i=0; i<len; i++)
    {	if (i>0 && i%4==0) str[k++]=' ';
        char b=data[i];
        str[k++]=hex[((b&0xf0)>>4)];
        str[k++]=hex[(b&0x0f)];
    }
    str[k++]='\0';
    return str;
}


/** Computes Internet Checksum (RFC 1071) for "count" bytes beginning at location "buff". */
unsigned short checksum(unsigned short *buff, unsigned int count)
{	// compute the sum
    long sum = 0;
    for( ; count > 1; count -= 2) sum += *(unsigned short*)buff++;
    // add left-over byte, if any
    if (count > 0) sum += *(unsigned char*)buff;
    // fold 32-bit sum to 16 bits
    while (sum>>16) sum = (sum & 0xffff) + (sum >> 16);
    sum = ~sum;
    return sum;
}


/** Processes a packet.
  * @param env the JNI environment
  * @param byte_array buffer
  * @param packet_handler_obj the packet handler object
  * @param packet the packet to be processed
  * @param packet_len the packet length
  * @return the new packet length if the packet has to be accepted, or 0 if the packet has to be dropped */
size_t processPacket(JNIEnv* env, jbyteArray byte_array, jobject packet_handler_obj, unsigned char* packet, size_t packet_len)
{
    // mangle the packet here
    jclass cls=(*env)->GetObjectClass(env,packet_handler_obj);
    jmethodID mid=(*env)->GetMethodID(env,cls,"processPacket","([BI)I");

    void* temp=(*env)->GetPrimitiveArrayCritical(env,(jarray)byte_array,0);
    memcpy(temp,packet,packet_len);
    (*env)->ReleasePrimitiveArrayCritical(env,byte_array,temp,0);

    packet_len=(*env)->CallIntMethod(env,packet_handler_obj,mid,byte_array,packet_len);

    temp=(*env)->GetPrimitiveArrayCritical(env,(jarray)byte_array,0);
    memcpy(packet,temp,packet_len);
    (*env)->ReleasePrimitiveArrayCritical(env,byte_array,temp,0);

    return packet_len;
}


/*
 * Class:     NetfilterQueue
 * Method:    open
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_nemo_it_unipr_netsec_netfilter_NetfilterQueue_open
        (JNIEnv *env, jobject obj) {
    struct nfq_handle* h=nfq_open();
    if (!h) {
        printf("nfq_open failed\n");
        return 0;
    }

    // some tests
    if (nfq_unbind_pf(h,AF_INET)<0) {
        printf("nfq_unbind_pf AF_INET failed\n");
        nfq_close(h);
        return 0;
    }

    if (nfq_unbind_pf(h,AF_INET6)<0) {
        printf("nfq_unbind_pf AF_INET6 failed\n");
        nfq_close(h);
        return 0;
    }

    if (nfq_bind_pf(h,AF_INET)<0) {
        printf("nfq_bind_pf AF_INET failed\n");
        nfq_close(h);
        return 0;
    }

    if (nfq_bind_pf(h,AF_INET6)<0) {
        printf("nfq_bind_pf AF_INET6 failed\n");
        nfq_close(h);
        return 0;
    }

    return (jlong)h;
}


/*
 * Class:     NetfilterQueue
 * Method:    close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_nemo_it_unipr_netsec_netfilter_NetfilterQueue_close
        (JNIEnv *env, jobject obj, jlong handle)
{
    struct nfq_handle *h=(struct nfq_handle*)handle;
    nfq_close(h);
}


/** Netfilter callback for processing a single queued packet.  */
static int nfq_cb(struct nfq_q_handle *qh, struct nfgenmsg *msg, struct nfq_data *nfa, void *data)
{	struct nfqnl_msg_packet_hdr *ph=nfq_get_msg_packet_hdr(nfa);
    int id=ntohl(ph->packet_id);

    struct jni_packet_callback_data* jpc_data=(struct jni_packet_callback_data*)data;

    // process  the packet
    unsigned char *packet;
    int packet_len=nfq_get_payload(nfa,&packet);
    packet_len=processPacket(jpc_data->env,jpc_data->byte_array,jpc_data->ph_obj,packet,packet_len);

    if (packet_len>0) return nfq_set_verdict(qh,id,NF_ACCEPT,packet_len,packet);
    else return nfq_set_verdict(qh,id,NF_DROP,0,NULL);
}


/*
 * Class:     NetfilterQueue
 * Method:    run
 * Signature: (JILPacketHandler;)I
 */
JNIEXPORT jint JNICALL Java_nemo_it_unipr_netsec_netfilter_NetfilterQueue_run
        (JNIEnv* env, jobject obj, jlong handle, jint num, jobject ph_obj)
{
    struct nfq_handle *h=(struct nfq_handle*)handle;

    struct jni_packet_callback_data pc_data;
    pc_data.env=env;
    pc_data.byte_array=(*env)->NewByteArray(env,BUFSIZE);
    pc_data.ph_obj=ph_obj;

    struct nfq_q_handle *qh=nfq_create_queue(h,num,nfq_cb,&pc_data);
    if (!qh)
    {	printf("nfq_create_queue failed\n");
        nfq_close(h);
        return 1;
    }

    int status=nfq_set_mode(qh,NFQNL_COPY_PACKET,BUFSIZE);
    if (status<0)
    {	printf("nfq_set_mode NFQNL_COPY_PACKET failed\n");
        nfq_destroy_queue(qh);
        nfq_close(h);
        return 1;
    }

    char buf[BUFSIZE];
    int fd=nfq_fd(h);
    while (1)
    {	int rv_len=recv(fd,buf,sizeof(buf),0);
        if (rv_len>=0)
        {	if (verbose) printf("pkt received\n");
            nfq_handle_packet(h,buf,rv_len);
            continue;
        }
        if (errno==ENOBUFS)
        {	continue;
        }
        printf("recv failed\n");
        break;
    }
    nfq_destroy_queue(qh);
    nfq_close(h);
    return 1;
}



