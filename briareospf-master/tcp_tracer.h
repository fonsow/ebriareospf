#include <linux/icmp.h>
#include <linux/if_ether.h>
#include <linux/ip.h>
#include <linux/tcp.h>

static __always_inline unsigned short is_icmp_ping_request(void *data,
                                                           void *data_end) {
  struct ethhdr *eth = data;
  if (data + sizeof(struct ethhdr) > data_end)
    return 0;

  if (bpf_ntohs(eth->h_proto) != ETH_P_IP)
    return 0;

  struct iphdr *iph = data + sizeof(struct ethhdr);
  if (data + sizeof(struct ethhdr) + sizeof(struct iphdr) > data_end)
    return 0;

  if (iph->protocol != 0x01)
    // We're only interested in ICMP packets
    return 0;

  struct icmphdr *icmp = data + sizeof(struct ethhdr) + sizeof(struct iphdr);
  if (data + sizeof(struct ethhdr) + sizeof(struct iphdr) +
          sizeof(struct icmphdr) >
      data_end)
    return 0;

  return (icmp->type == 8);
}

static __always_inline unsigned short is_tcp_packet(void *data, void *data_end){
  struct ethhdr *eth = data;
  if (data + sizeof(struct ethhdr) > data_end)
    return 0;

  if (bpf_ntohs(eth->h_proto) != ETH_P_IP)
    return 0;

  struct iphdr *iph = data + sizeof(struct ethhdr);
  if (data + sizeof(struct ethhdr) + sizeof(struct iphdr) > data_end)
    return 0;

  if (iph->protocol != IPPROTO_TCP)
    // We're only interested in TCP packets
    return 0;

  struct tcphdr *tcp = data + sizeof(struct ethhdr) + sizeof(struct iphdr);
  if (data + sizeof(struct ethhdr) + sizeof(struct iphdr) + sizeof(struct tcphdr) > data_end)
    return 0;

  return 1;
}


#define IP_SRC_OFF (ETH_HLEN + offsetof(struct iphdr, saddr))
#define IP_DST_OFF (ETH_HLEN + offsetof(struct iphdr, daddr))
