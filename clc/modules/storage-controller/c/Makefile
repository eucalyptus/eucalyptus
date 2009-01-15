LIBDIR=../lib
LVM2_SO=liblvm2control.so
CFLAGS=-g -fpic -I. -I../h 
WARN=-Wall -Wno-unused-variable

all: $(LVM2_SO) 

.c.o:
	gcc -c $(WARN) $(CFLAGS) $<

$(LVM2_SO): 
	mkdir -p $(LIBDIR)
	gcc -shared $(CFLAGS) -o $(LIBDIR)/$(LVM2_SO) *.c

clean:
	rm -f $(LVM2_SO) *.o *~* *#*  

install:
	mkdir -p $(EUCALYPTUS)/usr/lib 
	cp $(LIBDIR)/$(LVM2_SO) $(EUCALYPTUS)/usr/lib
