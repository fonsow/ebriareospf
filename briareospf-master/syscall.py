import bcc

# Define the BPF program
bpf_text = """
#include <uapi/linux/ptrace.h>
#include <linux/sched.h>
#include <uapi/linux/bpf.h>
#define SYSCALL_NAME_LEN 100

struct syscall_data_t {
    char syscall_name[SYSCALL_NAME_LEN];
    u64 timestamp;
};

BPF_HASH(syscall_order_map, u32, struct syscall_data_t);

int trace_syscall_entry(struct tracepoint__syscalls__sys_enter_* ctx) {
    u32 pid = bpf_get_current_pid_tgid();

    struct syscall_data_t data = {0};
    bpf_probe_read_kernel(&data.syscall_name, sizeof(data.syscall_name), (void *)ctx->name);
    data.timestamp = bpf_ktime_get_ns();

    syscall_order_map.update(&pid, &data);

    return 0;
}
"""

# Create an instance of the BPF class
b = bcc.BPF(text=bpf_text)

# Attach the BPF program to the syscalls tracepoint
b.attach_tracepoint("syscalls:sys_enter", "trace_syscall_entry")

# Get the BPF hash map
syscall_order_map = b.get_table("syscall_order_map")

# Iterate over the hash map and print the data
for k, v in syscall_order_map.items():
    print("pid: %d, syscall: %s, timestamp: %d" % (k.value, v.syscall_name.decode(), v.timestamp))

