#!/usr/bin/env python
from time import sleep, strftime
import argparse
import errno
import itertools
import sys
import signal
import os
from datetime import datetime
from bcc import BPF
from bcc.utils import printb
from bcc.syscall import syscall_name, syscalls

text = """
#include <linux/sched.h>

struct data_t{
    u32 pid;
    char comm[TASK_COMM_LEN];
    u32 syscall_id;
    u64 ts;
};

BPF_RINGBUF_OUTPUT(syscalls, 8);

TRACEPOINT_PROBE(raw_syscalls,sys_enter){
    u64 pid_tgid = bpf_get_current_pid_tgid();
    u32 key = pid_tgid >> 32;
    u32 tid = (u32)pid_tgid;
    
    #ifdef FILTER_PID
        if (key == FILTER_PID)
            return 0;
    #endif
    
    u64 ts = bpf_ktime_get_ns();
    struct data_t event = {};
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.pid = key;
    event.syscall_id = args->id;
    event.ts = ts;
    syscalls.ringbuf_output(&event, sizeof(event),0);
    return 0;
}
"""

def comm_for_pid(pid):
    try:
        return open("/proc/%d/comm" % pid, "rb").read().strip()
    except Exception:
        return b"[unknown]"

def format_ts(nanos):
    dt = datetime.fromtimestamp(nanos / 1000000000)
    return dt.strftime('%Y-%m-%d %H:%M:%S')

def callback(ctx, data, size):
    event = bpf["syscalls"].event(data)
    with open("sys_enter.txt", "a") as f:  
        print("%-10d %-10s %-10s %-10s" % (event.pid, format_ts(event.ts), event.comm, syscall_name(event.syscall_id)), file=f)



text = ("#define FILTER_PID %d\n" % os.getpid()) + text
bpf = BPF(text=text)

bpf["syscalls"].open_ring_buffer(callback)

while 1:
    try:
        bpf.ring_buffer_poll()
        
    except KeyboardInterrupt:
        os.exit()