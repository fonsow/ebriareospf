Atualmente temos 3 módulos:
    - packet_tracker - como o nome indica dá track a um packet
    - syscall_enter_tracer e syscall_exit tracer
        - Dão trace às chamadas ao sistema, e recolhe dados como: o PID do processo que efetuou a chamada, timestamp, PID do processo pai, a syscall feita
        - A diferença dos programas é que o 1º recolhe o evento quando a chamada ao sistema é feita, e o 2º quando a chamada ao sistema é terminada

Com a adição do cli.py podemos usar este programa para correr os principais módulos (syscall_enter_tracer.py & syscall_exit_tracer.py) a partir do mesmo programa:
Para o fazer
```
$ sudo python3 cli.py exit --pid 5678,5682 --syscall 24,27
$ sudo python3 cli.py enter --bt 5000 --syscall 25
```
Importante notar que as flags --bt (bigger than) e --pid são mutualmente exclusivas

Caso queiramos também é possível correr os programas em standalone com:
```
$ sudo python3 [programa].py
```
