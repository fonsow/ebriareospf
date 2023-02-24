#!/usr/bin/env python
from time import sleep, strftime
import argparse
import errno
import itertools
import sys
import signal
import os
from bcc import BPF
from bcc.utils import printb
from bcc.syscall import syscall_name, syscalls

text = """
#include <linux/sched.h>

BPF_HASH(data, u32, u64);

struct data_t{
    u32 pid;
    char comm[TASK_COMM_LEN];
    u32 syscall_id;
};

BPF_RINGBUF_OUTPUT(syscalls, 8);

TRACEPOINT_PROBE(raw_syscalls,sys_exit){
    u64 pid_tgid = bpf_get_current_pid_tgid();
    u32 key = pid_tgid >> 32;
    u32 tid = (u32)pid_tgid;
    struct data_t event = {};
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.pid = key;
    event.syscall_id = args->id;
    syscalls.ringbuf_output(&event, sizeof(event),0);
    
    u64 *val, zero = 0;
    val = data.lookup_or_try_init(&key, &zero);
    if(val){
        lock_xadd(val,1);

    }
    return 0;
}
"""

agg_colname = "PID\t COMM"
time_colname = "TIME (us)"

def comm_for_pid(pid):
    try:
        return open("/proc/%d/comm" % pid, "rb").read().strip()
    except Exception:
        return b"[unknown]"

def agg_colval(key):
    return b"%-6d %-15s" % (key.value, comm_for_pid(key.value))

def print_stats():
    data = bpf["data"]
    print("[%s]" % strftime("%H:%M:%S"))
    print("%-22s %8s" % (agg_colname, "COUNT"))
    for k, v in sorted(data.items(), key=lambda kv: -kv[1].value)[:10]:
        if k.value == 0xFFFFFFFF:
            continue    # happens occasionally, we don't need it
        printb(b"%-22s %8d" % (agg_colval(k), v.value))
    print("")
    data.clear()

def callback(ctx, data, size):
    event = bpf["syscalls"].event(data)
    print("%-10d %-10s %-10s" % (event.pid,event.comm, syscall_name(event.syscall_id)))




bpf = BPF(text=text)

bpf["syscalls"].open_ring_buffer(callback)

while 1:
    try:
        bpf.ring_buffer_poll()
        sleep(0.5)
        # print_stats() isto n está a ser chamado idkw
    except KeyboardInterrupt:
        print("Bye")
        sys.exit(0)