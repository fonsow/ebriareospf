#include "tcp_tracer.h"

#include <bcc/proto.h>
#include <linux/pkt_cls.h>

int xdp(struct xdp_md *ctx) {
  void *data = (void *)(long)ctx->data;
  void *data_end = (void *)(long)ctx->data_end;

  if (is_icmp_ping_request(data, data_end)) {
    struct iphdr *iph = data + sizeof(struct ethhdr);
    struct icmphdr *icmp = data + sizeof(struct ethhdr) + sizeof(struct iphdr);
    bpf_trace_printk("[xdp] ICMP request for %x type %x DROPPED\n", iph->daddr,
                     icmp->type);
    return XDP_PASS;
  }

  return XDP_PASS;
}

int tc(struct __sk_buff *skb) {
  //bpf_trace_printk("[tc] ingress got packet\n");

  void *data = (void *)(long)skb->data;
  void *data_end = (void *)(long)skb->data_end;

  if (is_icmp_ping_request(data, data_end)) {
    struct iphdr *iph = data + sizeof(struct ethhdr);
    struct icmphdr *icmp = data + sizeof(struct ethhdr) + sizeof(struct iphdr);
    bpf_trace_printk("[tc] ICMP request for %x type %x\n", iph->daddr,
                     icmp->type);
    return TC_ACT_OK;
  }
  return TC_ACT_OK;
}
