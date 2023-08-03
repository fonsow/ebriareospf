#include "tcp_tracer.h"

#include <bcc/proto.h>
#include <linux/pkt_cls.h>

struct data_t{
  //ETH
  unsigned int type:16;
  /*IP
  unsigned char version:4;           // byte 0
  unsigned char header_len:4;
  unsigned char type_of_service;
  unsigned short total_len;
  unsigned short identification; // byte 4
  unsigned short ffo_unused:1;
  unsigned short df:1;
  unsigned short mf:1;
  unsigned short foffset:13;
  unsigned char time_to_live;             // byte 8
  unsigned char next_protocol;
  unsigned short hchecksum;
  unsigned int src_ip;            // byte 12
  unsigned int dst_ip; 
  //TCP
  unsigned short  src_port;   // byte 0
  unsigned short  dst_port;
  unsigned int    seq_num;    // byte 4
  unsigned int    ack_num;    // byte 8
  unsigned char   offset:4;    // byte 12
  unsigned char   reserved:4;
  unsigned char   flag_cwr:1;
  unsigned char   flag_ece:1;
  unsigned char   flag_urg:1;
  unsigned char   flag_ack:1;
  unsigned char   flag_psh:1;
  unsigned char   flag_rst:1;
  unsigned char   flag_syn:1;
  unsigned char   flag_fin:1;
  unsigned short  rcv_wnd:1;
  unsigned short  cksum;      // byte 16
  unsigned short  urg_ptr;*/
};

BPF_PERF_OUTPUT(packets);

int xdp(struct xdp_md *ctx) {
  void *data = (void *)(long)ctx->data;
  void *data_end = (void *)(long)ctx->data_end;
  struct data_t packet = {};
  //bpf_trace_printk("tamos ai2");
  if (is_tcp_packet(data, data_end)) {
    //bpf_trace_printk("tamos ai");
    //eth
    struct ethhdr *eth = data;
    packet.type = eth->h_proto;
    //ip
    struct iphdr *iph = data + sizeof(struct ethhdr);
    //packet.version = iph->version;
    //tcp
    struct tcphdr *tcp = data + sizeof(struct ethhdr) + sizeof(struct iphdr);
    
    packets.perf_submit(ctx, &packet, sizeof(packet));
  }

  return XDP_PASS;
}