LIBDIR=../lib
FS_STORE_SO=libfsstorage.so
PLATFORM=linux
CFLAGS=-g -fpic -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(PLATFORM) -I. -I../include
WARN=-Wall -Wno-unused-variable

all: $(FS_STORE_SO) 

.c.o:
	gcc -c $(WARN) $(CFLAGS) $<

$(FS_STORE_SO): 
	mkdir -p $(LIBDIR)
	gcc -shared $(CFLAGS) -o $(LIBDIR)/$(FS_STORE_SO) *.c

clean:
	rm -f $(FS_STORE_SO) *.o *~* *#*  

install:
	mkdir -p $(EUCALYPTUS)/usr/lib/eucalyptus/
	cp $(LIBDIR)/$(FS_STORE_SO) $(EUCALYPTUS)/usr/lib/eucalyptus/
