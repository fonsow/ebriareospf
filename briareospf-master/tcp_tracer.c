#include "tcp_tracer.h"
#include <linux/bpf.h>
#include <linux/in.h>
#include <bcc/proto.h>
#include <linux/pkt_cls.h>

struct data_t{
  //ETH
  u16 type;
  //MACS é pointless, são sp os do router - pc
  unsigned char src_mac[ETH_ALEN];
	unsigned char dst_mac[ETH_ALEN];
  //IP
  u8 header_len;
  u8 version;
  u8 type_of_service;
  u32 total_len;
  u16 identification; // byte 4
  u16 foffset;
  u8 time_to_live;             // byte 8
  u8 next_protocol;
  u16 hchecksum;
  u32 src_ip;          // byte 12
  u32 dst_ip; //sp o do pc
  /*TCP
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
  if (is_tcp_packet(data, data_end)) {
    //eth
    struct ethhdr *eth = data;
    //bpf_trace_printk("tamos ai lol %p, %p", eth->h_source, eth->h_dest);
    packet.type = eth->h_proto;
    __builtin_memcpy(packet.src_mac, eth->h_source, ETH_ALEN);
    __builtin_memcpy(packet.dst_mac, eth->h_dest, ETH_ALEN);
    //ip
    struct iphdr *iph = data + sizeof(struct ethhdr);
    packet.src_ip = iph->saddr;
    packet.dst_ip = iph->daddr;
    packet.version = iph->version;
    packet.header_len = iph->ihl;
    packet.type_of_service = iph->tos;
    packet.total_len = iph->tot_len;
    packet.identification = iph->id;
    packet.foffset = iph->frag_off;
    packet.time_to_live = iph->ttl;
    packet.next_protocol = iph->protocol;
    packet.hchecksum = iph->check;

    //bpf_trace_printk("tamos ai lol2 %u", packet.total_len);
    /*
    unsigned short identification; // byte 4
    unsigned short ffo_unused:1;
    unsigned short df:1;
    unsigned short mf:1;
    unsigned short foffset:13;
    unsigned char time_to_live;             // byte 8
    unsigned char next_protocol;
    unsigned short hchecksum;
    */
    //packet.version = iph->version;
    //tcp
    struct tcphdr *tcp = data + sizeof(struct ethhdr) + sizeof(struct iphdr);
    
    packets.perf_submit(ctx, &packet, sizeof(packet));
  }

  return XDP_PASS;
}