Atualmente temos 3 módulos:
    - packet_tracker - como o nome indica dá track a um packet
    - syscall_enter_tracer e syscall_exit tracer
        - Dão trace às chamadas ao sistema, e recolhe dados como: o PID do processo que efetuou a chamada, timestamp, PID do processo pai, a syscall feita
        - A diferença dos programas é que o 1º recolhe o evento quando a chamada ao sistema é feita, e o 2º quando a chamada ao sistema é terminada

Para correr os programas basta corrê-los como programas de python normal, mas é recomendado correr como sudo:
sudo python3 [programa].py