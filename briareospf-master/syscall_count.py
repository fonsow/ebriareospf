#!/usr/bin/env python
from time import sleep, strftime
import argparse
import errno
import itertools
import sys
import signal
from bcc import BPF
from bcc.utils import printb
from bcc.syscall import syscall_name, syscalls

text = """
#include <linux/sched.h>

#define MAX_SYSCALL_NAME_LEN 64

BPF_HASH(data, u32, u64);

TRACEPOINT_PROBE(raw_syscalls,sys_exit){
    u64 pid_tgid = bpf_get_current_pid_tgid();
    u32 key = pid_tgid >> 32;
    u32 tid = (u32)pid_tgid;

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


bpf = BPF(text=text)
exiting = 0
while True:
    try:
        sleep(5)
    except KeyboardInterrupt:
        exiting = 1
    
    print_stats()

    if exiting:
        print("Bye")
        exit()
