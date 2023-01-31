from bcc import BPF

bpf_text = """
#include <uapi/linux/ptrace.h>
#include <linux/sched.h>

BPF_HASH(syscalls, u32, u64);

int trace_sys_enter(struct pt_regs *ctx) {
    u32 pid = bpf_get_current_pid_tgid();
    u64 val = bpf_ktime_get_ns();

    syscalls.update(&pid, &val);
    return 0;
}

int trace_sys_exit(struct pt_regs *ctx) {
    u32 pid = bpf_get_current_pid_tgid();
    u64 *val;

    val = syscalls.lookup(&pid);
    if (val) {
        bpf_trace_printk("%d %llu\n", pid, bpf_ktime_get_ns() - *val);
        syscalls.delete(&pid);
    }

    return 0;
}
"""
syscalls = bpf["syscalls"]

for k, v in syscalls.items():
    pid, start_time = k.value, v.value
    print("PID: {} Start Time: {}".format(pid, start_time))
