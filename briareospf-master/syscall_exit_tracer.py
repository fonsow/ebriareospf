#!/usr/bin/env python
from time import sleep, strftime, time
import sys
from datetime import datetime
from bcc import BPF
from bcc.utils import printb
from bcc.syscall import syscall_name, syscalls
import struct
import time

text = """
#include <linux/sched.h>

struct data_t{
    u32 pid;
    char p_comm[TASK_COMM_LEN];
    char comm[TASK_COMM_LEN];
    u32 syscall_id;
};


BPF_RINGBUF_OUTPUT(syscalls, 8);

TRACEPOINT_PROBE(raw_syscalls,sys_exit){
    struct data_t event = {};
    struct task_struct *task;
    task = (struct task_struct *)bpf_get_current_task();
    u64 pid_tgid = bpf_get_current_pid_tgid();
    event.pid = pid_tgid >> 32;
    u32 tid = (u32)pid_tgid;  
    bpf_probe_read_kernel_str(&event.p_comm, TASK_COMM_LEN, task->real_parent->comm);
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.syscall_id = args->id;
    
    syscalls.ringbuf_output(&event, sizeof(event),0);
    return 0;
}
"""
def get_filters():
    with open('data/filter_syscall.txt', 'r') as file:
        syscalls = file.readlines()
    # Remove the newline character at the end of each line
    syscalls = [line.strip() for line in syscalls]

    with open('data/filter_comm.txt', 'r') as file:
        comms = file.readlines()
    # Remove the newline character at the end of each line
    comms = [line.strip() for line in comms]

    with open('data/filter_pid.txt', 'r') as file:
        pids = [int(line) for line in file]

    return syscalls,comms, pids

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
    if syscall_name(event.syscall_id).decode('utf-8') not in filter_syscalls and event.comm.decode('utf-8') not in filter_comms and event.pid not in filter_pid:
        pass
        #print(event.pid)
        #print(filter_pid)
        ############# WRITE IN PLAINTEXT
        #with open("data/sys_exit.txt", "a") as f:  
        #    print("%-10d %-10s %-10s %-10s" % (event.pid, format_ts(event.ts), event.comm, syscall_name(event.syscall_id)), file=f)
        ############# WRITE IN BINARY
        #with open("data/sys_exit.bin", "ab") as f:
            #data = struct.pack("<di10s10s10s", event.ts, event.pid, event.p_comm, event.comm, syscall_name(event.syscall_id))
            #f.write(data)

filter_syscalls, filter_comms, filter_pid = get_filters()
bpf = BPF(text=text)

bpf["syscalls"].open_ring_buffer(callback)

exiting = 1
while exiting:
    try:
        bpf.ring_buffer_poll()
        time.sleep(0.5)
    except KeyboardInterrupt:
        sys.exit()