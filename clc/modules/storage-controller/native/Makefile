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
	rm -rf $(LIBDIR)
	rm -f $(LVM2_SO) *.o *~* *#*  

distclean: clean

install:
	mkdir -p $(DESTDIR)$(EUCALYPTUS)/usr/lib/eucalyptus/
	cp $(LIBDIR)/$(LVM2_SO) $(DESTDIR)$(EUCALYPTUS)/usr/lib/eucalyptus/

uninstall:
	rm -r $(DESTDIR)$(EUCALYPTUS)/usr/lib/eucalyptus/$(LVM2_SO)
