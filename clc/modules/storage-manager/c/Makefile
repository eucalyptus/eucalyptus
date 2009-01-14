LIBDIR=../lib
FS_STORE_SO=libfsstorage.so
CFLAGS=-g -fpic -I. -I../h 
WARN=-Wall -Wno-unused-variable

all: $(FS_STORE_SO) 

.c.o:
	gcc -c $(WARN) $(CFLAGS) $<

$(FS_STORE_SO): 
	mkdir -p $(LIBDIR)
	gcc -shared $(CFLAGS) -o $(LIBDIR)/$(FS_STORE_SO) *.c

clean:
	rm -f $(FS_STORE_SO) *.o *~* *#*  

deploy:
	mkdir -p $(EUCALYPTUS)/usr/share/eucalyptus/lib 
	cp $(LIBDIR)/$(FS_STORE_SO) $(EUCALYPTUS)/usr/share/eucalyptus/lib
