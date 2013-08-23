#
# Makefile to build the maniac-sniffer
#

maniac-sniffer-linux : maniac-sniffer-linux.o
	gcc -g -o maniac-sniffer-linux maniac-sniffer-linux.o -lpcap

maniac-sniffer-linux.o : maniac-sniffer-linux.c
	gcc -g -c -o maniac-sniffer-linux.o maniac-sniffer-linux.c

maniac-sniffer-osx : maniac-sniffer-osx.o
	gcc -g -o maniac-sniffer-osx maniac-sniffer-osx.o -lpcap

maniac-sniffer-osx.o : maniac-sniffer-osx.c
	gcc -g -c -o maniac-sniffer-osx.o maniac-sniffer-osx.c

linux : maniac-sniffer-linux

osx : maniac-sniffer-osx

clean:
	rm -rf maniac-sniffer-linux maniac-sniffer-osx *.o

