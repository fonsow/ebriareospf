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

examples = """
Examples:
  syscall_enter_tracer -p 1234 -s 42
  syscall_enter_tracer --pid 5678 --syscall 24
  syscall_enter_tracer --bt 5000 --syscall 25
"""

# Create the argument parser
parser = argparse.ArgumentParser(
    prog="syscall_enter_tracer",
    description="Trace System calls",
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog=examples)

# Add the mutually exclusive group for -p and --bt options
group = parser.add_mutually_exclusive_group()

# Add the -p option for filtering by PID
group.add_argument("-p", "--pid", action="store",
                   help="Filter by PID")

# Add the -s option for filtering by system call number
parser.add_argument("-s", "--syscall", action="store",
                    help="Filter by system call number")

# Add the --bt option for backtrace (only valid without -p)
group.add_argument("--bt", action="store",
                   help="Include backtrace")

# Parse the command line arguments
arguments = parser.parse_args()
print(arguments)


text = """
#include <linux/sched.h>

struct data_t{
    u32 pid;
    char p_comm[TASK_COMM_LEN];
    u32 ppid;
    char comm[TASK_COMM_LEN];
    u32 syscall_id;
};


BPF_PERF_OUTPUT(syscalls);

TRACEPOINT_PROBE(raw_syscalls,sys_enter){
    ##FILTER_SYSCALL##
    struct data_t event = {};
    struct task_struct *task;
    task = (struct task_struct *)bpf_get_current_task();
    u64 pid_tgid = bpf_get_current_pid_tgid();
    event.pid = pid_tgid >> 32; 
    event.ppid = task->real_parent->pid;
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
    with open("data/sys_enter.txt", "a") as f:  
        print("PID=%d;TS=%d;EXEC=%s;PPID=%d;SYSCALL=%d;" % (event.pid, time.time(), event.comm, event.ppid, event.syscall_id), file=f)
        ############# WRITE IN BINARY
        #with open("data/sys_enter.bin", "ab") as f:
            #data = struct.pack("<di10s10s10s", event.ts, event.pid, event.p_comm, event.comm, syscall_name(event.syscall_id))
            #f.write(data)
    #event.clear()

#######FILTERING######
if arguments.pid:
    pids = [int(pid) for pid in arguments.pid.split(',')]
    pids_if = ' && '.join(['event.pid != %d' % pid for pid in pids])
    text = text.replace('##FILTER_PID##',
                    'if (%s) {return 0;}' % pids_if)
elif arguments.bt:
    pid = int(arguments.bt)
    text = text.replace('##FILTER_PID##', 'if(event.pid < %d) {return 0;}' % pid)
else:
    text = text.replace('##FILTER_PID##', "")

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
try:
    while True:
        bpf.perf_buffer_poll()
        #sleep(0.01)
except KeyboardInterrupt:
    sys.enter()