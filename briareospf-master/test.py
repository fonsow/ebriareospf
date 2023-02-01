import ctypes as ct
import sys

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
}

BPF_ARRAY(syscalls, u64, 100000);

TRACEPOINT_PROBE(raw_syscalls, sys_exit) {
    u64 pid_tgid = bpf_get_current_pid_tgid();
    u32 pid = pid_tgid >> 32;
    u32 tid = (u32)pid_tgid;

    u32 key = args->id;
    u64 val = pid_tgid;

    bpf_map_update_elem(&syscalls, &key, &val, BPF_NOEXIST);

    return 0;
}'''

def print_syscalls(syscalls):
    for i in range(100000):
        key = ct.c_int(i)
        val = ct.c_ulonglong(0)
        try:
            ret = b.syscalls.map.lookup(key, ct.byref(val))
        except Exception:
            return
        if ret == -1:
            continue
        print("System call ID: {}, PID-TID: {}".format(i, val.value))

b = BPF(text=bpf_text)
print_syscalls(b.syscalls)



