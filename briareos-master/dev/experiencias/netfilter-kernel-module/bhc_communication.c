#include <linux/kernel.h>
#include <net/sock.h>
#include <linux/netlink.h>

#include "bhc_communication.h"

struct sock *nl_sk = NULL;
char *msg = "";
int msg_size = 0;
char* recv_data = NULL;

void bhc_communication_nl_recv_msg(struct sk_buff *skb) {
  struct nlmsghdr *nlh;
  struct sk_buff *skb_out;
  int pid;
  int result;


  nlh = (struct nlmsghdr*)skb->data;
  recv_data = (char*)nlmsg_data(nlh);
  pid = nlh->nlmsg_pid;

  skb_out = nlmsg_new(msg_size,0);

  if (!skb_out) {
      printk(KERN_ERR "Failed to allocate new skb\n");
      return;
  }

  nlh = nlmsg_put(skb_out, 0, 0, NLMSG_DONE, msg_size, 0);

  NETLINK_CB(skb_out).dst_group = 0; //not in mcast group
  memcpy(nlmsg_data(nlh), msg, msg_size);

  result = nlmsg_unicast(nl_sk,skb_out,pid);

  recv_data = NULL;
  if (result < 0) {
    printk(KERN_INFO "Error while sending back to user\n");
  }
}

int bhc_communication_init(void) {
  printk("Entering: %s\n",__FUNCTION__);
  //This is for kernel >= 3.6
  struct netlink_kernel_cfg cfg = {
      .input = bhc_communication_nl_recv_msg,
  };

  nl_sk = netlink_kernel_create(&init_net, NETLINK_USER, &cfg);

  //nl_sk = netlink_kernel_create(&init_net, NETLINK_USER, 0, bhc_communication_nl_recv_msg,NULL,THIS_MODULE);

  if (!nl_sk) {
      printk(KERN_ALERT "Error creating socket.\n");
      return 1;
  }
  return 0;
}

void bhc_communication_exit(void) {
  netlink_kernel_release(nl_sk);
}

void send_to_userspace(int pipeline_id, const char* buff, unsigned int buff_len) {
	printk(KERN_INFO "[BHC | Pipeline Communication] Pipeline ID: %d, Data: %s, Data Length: %u\n", pipeline_id, buff, buff_len);
  msg = buff;
  msg_size = buff_len;
  while (!recv_data) {
  }
  printk(KERN_INFO "%s", recv_data);
}
