import time
import bcc

bpf_text = '''
#include <linux/sched.h>
#define MAX_SYSCALLS 1000000

struct sys_event_t {
    u32 id;
    u64 pid_tgid;
    u64 ns;
};

BPF_PERF_OUTPUT(events);
BPF_HASH(start, u64, u64);

TRACEPOINT_PROBE(raw_syscalls, sys_enter) {
    u64 pid_tgid = bpf_get_current_pid_tgid();
    u64 t = bpf_ktime_get_ns();
    start.update(&pid_tgid, &t);
    return 0;
}

TRACEPOINT_PROBE(raw_syscalls, sys_exit) {
    u64 pid_tgid = bpf_get_current_pid_tgid();
    u64 *start_ns = start.lookup(&pid_tgid);
    if (!start_ns)
        return 0;
    struct sys_event_t event = {};
    event.id = args->id;
    event.pid_tgid = pid_tgid;
    event.ns = bpf_ktime_get_ns() - *start_ns;
    events.perf_submit(args, &event, sizeof(event));
    return 0;
}'''

b = bcc.BPF(text=bpf_text)

b.attach_tracepoint("raw_syscalls:sys_exit", "sys_exit")

while True:
    try:
        time.sleep(1)
        print("Syscall ID \t Count")
        for k, v in b["start"].items():
            print("%d\t\t%d" % (k.value, v.value))
    except KeyboardInterrupt:
        exit()



