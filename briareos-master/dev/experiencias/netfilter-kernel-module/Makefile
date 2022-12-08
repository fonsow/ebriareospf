obj-m += briareos_interceptor.o
briareos_interceptor-objs := briareos_module.o bhc_communication.o

all:
	make -C /lib/modules/$(shell uname -r)/build M=$(shell pwd) modules
clean:
	make -C /lib/modules/$(shell uname -r)/build M=$(shell pwd) clean
