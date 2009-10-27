LIBDIR=../lib
LVM2_SO=liblvm2control.so
PLATFORM=linux
CFLAGS=-g -fpic -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(PLATFORM) -I. -I../include
WARN=-Wall -Wno-unused-variable

.PHONY: all clean install

all: $(LIBDIR)/$(LVM2_SO) 

.c.o:
	gcc -c $(WARN) $(CFLAGS) $<

$(LIBDIR)/$(LVM2_SO): 
	mkdir -p $(LIBDIR)
	gcc -shared $(CFLAGS) -o $(LIBDIR)/$(LVM2_SO) *.c

clean:
	rm -f $(LVM2_SO) *.o *~* *#*  

install:
	mkdir -p $(EUCALYPTUS)/usr/lib/eucalyptus/
	cp $(LIBDIR)/$(LVM2_SO) $(EUCALYPTUS)/usr/lib/eucalyptus/

uninstall:
	rm -r $(EUCALYPTUS)/usr/lib/eucalyptus/$(LVM2_SO)
