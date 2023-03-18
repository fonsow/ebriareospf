#!/usr/bin/env python
from time import sleep, strftime, time
import os
from datetime import datetime
from bcc import BPF
from bcc.utils import printb
from bcc.syscall import syscall_name, syscalls
import struct

text = """
#include <linux/sched.h>

struct data_t{
    u32 pid;
    char p_comm[TASK_COMM_LEN];
    char comm[TASK_COMM_LEN];
    u32 syscall_id;
    u64 ts;
};


BPF_RINGBUF_OUTPUT(syscalls, 8);

TRACEPOINT_PROBE(raw_syscalls,sys_exit){
    struct task_struct *task;
    u64 pid_tgid = bpf_get_current_pid_tgid();
    u32 key = pid_tgid >> 32;
    u32 tid = (u32)pid_tgid;
    task = (struct task_struct *)bpf_get_current_task();
    
    u64 ts = bpf_ktime_get_ns();
    struct data_t event = {};
    bpf_probe_read_kernel_str(&event.p_comm, TASK_COMM_LEN, task->real_parent->comm);
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
    if event.pid != os.getpid():
        with open("sys_exit.bin", "ab") as f:
            data = struct.pack("<di10s10s10s", event.ts, event.pid, event.p_comm, event.comm, syscall_name(event.syscall_id))
            f.write(data)

bpf = BPF(text=text)

bpf["syscalls"].open_ring_buffer(callback)

while 1:
    try:
        bpf.ring_buffer_poll()
    except KeyboardInterrupt:
        os.exit()