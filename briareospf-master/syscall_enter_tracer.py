#!/usr/bin/env python
from time import sleep, strftime, time
import sys
from datetime import datetime
from bcc import BPF
from bcc.utils import printb
from bcc.syscall import syscall_name, syscalls
import struct
import time
from os.path import exists

text = """
#include <linux/sched.h>

struct data_t{
    u32 pid;
    char p_comm[TASK_COMM_LEN];
    char comm[TASK_COMM_LEN];
    u32 syscall_id;
};


BPF_PERF_OUTPUT(syscalls);

TRACEPOINT_PROBE(raw_syscalls,sys_enter){
    struct data_t event = {};
    struct task_struct *task;
    task = (struct task_struct *)bpf_get_current_task();
    u64 pid_tgid = bpf_get_current_pid_tgid();
    event.pid = pid_tgid >> 32;
    u32 tid = (u32)pid_tgid;  
    bpf_probe_read_kernel_str(&event.p_comm, TASK_COMM_LEN, task->real_parent->comm);
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.syscall_id = args->id;
    
    syscalls.perf_submit(args, &event, sizeof(event));
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
    if syscall_name(event.syscall_id).decode('utf-8') in filter_syscalls and event.comm.decode('utf-8') in filter_comms and event.pid in filter_pid:
        #print(event.pid)
        #print(filter_pid)
        ############# WRITE IN PLAINTEXT
        with open("data/sys_enter.txt", "a") as f:  
            print("%-10d %-10d %-10s %-10s" % (event.pid, time.time(), event.comm, syscall_name(event.syscall_id)), file=f)
        ############# WRITE IN BINARY
        #with open("data/sys_enter.bin", "ab") as f:
            #data = struct.pack("<di10s10s10s", event.ts, event.pid, event.p_comm, event.comm, syscall_name(event.syscall_id))
            #f.write(data)

filter_syscalls, filter_comms, filter_pid = get_filters()
bpf = BPF(text=text)

bpf["syscalls"].open_perf_buffer(callback)
if not exists("data/sys_enter.txt"):
    with open("data/sys_enter.txt", "w") as f:
        print("PID\tTS\tPROGRAM_COMM\tSYSCALL",file=f)

while True:
    try:
        bpf.perf_buffer_poll()
        #time.sleep(0.01)
    except KeyboardInterrupt:
        sys.exit()