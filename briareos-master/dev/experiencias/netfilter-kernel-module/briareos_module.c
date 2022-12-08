#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/netfilter.h>
#include <linux/netfilter_ipv4.h>
#include <linux/ip.h>
#include <linux/tcp.h>

#include "bhc_communication.h"

MODULE_AUTHOR("Andre Baptista");
MODULE_DESCRIPTION("Briareos Host Component Interceptor");
MODULE_LICENSE("GPL");
MODULE_VERSION("0.1");

static struct nf_hook_ops nf_incoming_ops;
static struct nf_hook_ops nf_outgoing_ops;

/*Function incoming_traffic_hook*/
struct iphdr* incoming_ip_header;
struct tcphdr* incoming_tcph;
char* incoming_data;

unsigned int incoming_traffic_hook(unsigned int hooknum, struct sk_buff *skb, const struct net_device *in, const struct net_device *out, int (*okfn)(struct sk_buff *)) {
  if(!skb) {
    return NF_ACCEPT;
  }

  incoming_ip_header = (struct iphdr *)skb_network_header(skb);

  if (incoming_ip_header->protocol == IPPROTO_UDP) {
    printk(KERN_INFO "[BHC | Incoming] Accepting UDP packet\n");
  }
  else if (incoming_ip_header->protocol == IPPROTO_TCP) {
    printk(KERN_INFO "[BHC | Incoming] Accepting TCP packet\n");
    incoming_tcph = (struct tcphdr *)((__u32 *)incoming_ip_header + incoming_ip_header->ihl);
    incoming_data = (char *)((unsigned char *)incoming_tcph + (incoming_tcph->doff * 4));

    send_to_userspace(0, "incoming test data", 18);
    return NF_ACCEPT;
    printk(KERN_INFO "[BHC | Incoming] TCP packet src port: %hu, dest port: %hu\n", ntohs(incoming_tcph->source), ntohs(incoming_tcph->dest));
  }
  return NF_ACCEPT;
}

/*Function outgoing_traffic_hook*/
struct iphdr* outgoing_ip_header;
struct tcphdr* outgoing_tcph;
char* outgoing_data;

unsigned int outgoing_traffic_hook(unsigned int hooknum, struct sk_buff *skb, const struct net_device *in, const struct net_device *out, int (*okfn)(struct sk_buff *)) {
  //send_to_userspace(OUTPUT_PIPELINE, "outgoing test data", 18);
  return NF_ACCEPT;
  if(!skb) {
    return NF_ACCEPT;
  }

  outgoing_ip_header = (struct iphdr *)skb_network_header(skb);

  if (outgoing_ip_header->protocol == IPPROTO_UDP) {
    printk(KERN_INFO "[BHC | Outgoing] Accepting UDP packet\n");
  }
  else if (outgoing_ip_header->protocol == IPPROTO_TCP) {
    printk(KERN_INFO "[BHC | Outgoing] Accepting TCP packet\n");
    outgoing_tcph = (struct tcphdr *)((__u32 *)outgoing_ip_header + outgoing_ip_header->ihl);
    outgoing_data = (char *)((unsigned char *)outgoing_tcph + (outgoing_tcph->doff * 4));
    printk(KERN_INFO "[BHC | Outgoing] TCP packet src port: %hu, dest port: %hu\n", ntohs(outgoing_tcph->source), ntohs(outgoing_tcph->dest));
  }
  return NF_ACCEPT;
}

/*Init*/
void initIncomingHook(void) {
  printk(KERN_INFO "[BHC] Initializing incoming traffic interceptor\n");
  nf_incoming_ops.hook = incoming_traffic_hook;
  nf_incoming_ops.hooknum = NF_INET_PRE_ROUTING;
  nf_incoming_ops.pf = PF_INET; //IPV4
  nf_incoming_ops.priority = NF_IP_PRI_FIRST;
  nf_register_hook(&nf_incoming_ops);
}

void initOutgoingHook(void) {
  printk(KERN_INFO "[BHC] Initializing outgoing traffic interceptor\n");
  nf_outgoing_ops.hook = outgoing_traffic_hook;
  nf_outgoing_ops.hooknum = NF_INET_LOCAL_OUT;
  nf_outgoing_ops.pf = PF_INET; //IPV4
  nf_outgoing_ops.priority = NF_IP_PRI_FIRST;
  nf_register_hook(&nf_outgoing_ops);
}

int init_module() {
  bhc_communication_init();
  initIncomingHook();
  initOutgoingHook();
  printk(KERN_INFO "[BHC] Initialization complete.\n");
  return 0;
}

/*Cleanup*/
void cleanup_module() {
  bhc_communication_exit();
  nf_unregister_hook(&nf_incoming_ops);
  nf_unregister_hook(&nf_outgoing_ops);
  printk(KERN_INFO "[BHC] Exiting\n");
}
