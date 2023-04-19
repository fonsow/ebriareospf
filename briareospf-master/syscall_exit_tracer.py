#!/usr/bin/env python
from time import sleep, strftime, time
import sys
from datetime import datetime
from bcc import BPF
from bcc.syscall import syscall_name, syscalls
import struct
import time
from os.path import exists
import argparse
from os import getpid

examples = """examples:
sudo python3 syscall_exit_tracer.py -p 1828,1837 # trace pids 1828 and 1837
sudo python3 syscall_exit_tracer.py -s 3,4 # trace syscalls number 3 and 4
"""
parser = argparse.ArgumentParser(
    prog="syscall_exit_tracer",
    description="Trace System calls",
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog=examples)
parser.add_argument("-p", "--pid",action="store",
                    help="Filter by PID")
parser.add_argument("-s", "--syscall", action="store",
                    help="Filter by system call number")
arguments= parser.parse_args()
print(arguments)

text = """
#include <linux/sched.h>

struct data_t{
    u32 pid;
    char p_comm[TASK_COMM_LEN];
    char comm[TASK_COMM_LEN];
    u32 syscall_id;
};


BPF_PERF_OUTPUT(syscalls);

TRACEPOINT_PROBE(raw_syscalls,sys_exit){
    ##FILTER_SYSCALL##
    struct data_t event = {};
    struct task_struct *task;
    task = (struct task_struct *)bpf_get_current_task();
    u64 pid_tgid = bpf_get_current_pid_tgid();
    event.pid = pid_tgid >> 32; 
    ##FILTER_SELF##
    ##FILTER_PID##
    bpf_probe_read_kernel_str(&event.p_comm, TASK_COMM_LEN, task->real_parent->comm);
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.syscall_id = args->id;
    
    syscalls.perf_submit(args, &event, sizeof(event));
    return 0;
}
"""

def comm_for_pid(pid):
    try:
        return open("/proc/%d/comm" % pid, "rb").read().strip()
    except Exception:
        return b"[unknown]"

def callback(ctx, data, size):
    event = bpf["syscalls"].event(data)
    #print(event.pid)
    #print(event.syscall_id)
    #print(filter_pid)
        ############# WRITE IN PLAINTEXT
    with open("data/sys_exit.txt", "a") as f:  
        print("%-10d %-10d %-10s %-10s %-10s" % (event.pid, time.time(), event.comm, event.p_comm, syscall_name(event.syscall_id)), file=f)
        ############# WRITE IN BINARY
        #with open("data/sys_exit.bin", "ab") as f:
            #data = struct.pack("<di10s10s10s", event.ts, event.pid, event.p_comm, event.comm, syscall_name(event.syscall_id))
            #f.write(data)

if not exists("data/sys_exit.txt"):
    with open("data/sys_exit.txt", "w") as f:
        print("PID\tTS\tPROGRAM_COMM\tPARENT_COMM\tSYSCALL",file=f)

#######FILTERING######
if arguments.pid:
    pids = [int(pid) for pid in arguments.pid.split(',')]
    pids_if = ' && '.join(['event.pid != %d' % pid for pid in pids])
    text = text.replace('##FILTER_PID##',
                    'if (%s) {return 0;}' % pids_if)
else:
    text = text.replace('##FILTER_PID##', '')

if arguments.syscall:
    
    syscalls = [int(syscall) for syscall in arguments.syscall.split(',')]
    syscalls_if = ' && '.join(['args->id != %d' % syscall for syscall in syscalls])
    text = text.replace('##FILTER_SYSCALL##',
                    'if (%s) {return 0;}' % syscalls_if)
else:
    text = text.replace('##FILTER_SYSCALL##', '')

text = text.replace('##FILTER_SELF##',
                    'if(event.pid == %d) {return 0;}' % getpid())

bpf = BPF(text=text)

bpf["syscalls"].open_perf_buffer(callback)
while True:
    bpf.perf_buffer_poll()