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
  //TCP corrige nos docs para usar info do github de linux
  u16 src_port;
  u16 dst_port;
  u32 seq_nr;
  u32 ack_seq;
  u8 res1;
  u8 doff;
  u8 fin;
  u8 syn;
  u8 rst;
  u8 psh;
  u8 ack;
  u8 urg;
  u8 ece;
  u8 cwr;
  u16 window;
  u16 check;
  u16 urg_ptr;
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
    //tcp
    struct tcphdr *tcp = data + sizeof(struct ethhdr) + sizeof(struct iphdr);
    packet.src_port = tcp->source;
    packet.dst_port = tcp->dest;
    packet.seq_nr = tcp->seq;
    packet.ack_seq = tcp->ack_seq;
    packet.res1 = tcp->res1;
    packet.doff = tcp->doff;
    packet.fin = tcp->fin;
    packet.syn = tcp->syn;
    packet.rst = tcp->rst;
    packet.psh = tcp->psh;
    packet.ack = tcp->ack;
    packet.urg = tcp->urg;
    packet.ece = tcp->ece;
    packet.window = tcp->window;
    packet.check = tcp->check;
    packet.urg_ptr = tcp->urg_ptr;

    packets.perf_submit(ctx, &packet, sizeof(packet));
  }

  return XDP_PASS;
}