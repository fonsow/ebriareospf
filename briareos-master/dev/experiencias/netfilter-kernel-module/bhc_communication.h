#include <linux/skbuff.h>

#define NETLINK_USER 31

int bhc_communication_init(void);
void bhc_communication_exit(void);
void bhc_communication_recv_msg(struct sk_buff *skb);

void send_to_userspace(int pipeline_id, const char* buff, unsigned int buff_len);
