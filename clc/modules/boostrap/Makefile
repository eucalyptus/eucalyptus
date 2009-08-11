CC = gcc
JAVAC = /usr/lib/jvm/java-6-openjdk/bin/javac
JAR = /usr/lib/jvm/java-6-openjdk/bin/jar
CFLAGS = -g -O2 -DOS_LINUX -DDSO_DLFCN -DCPU=\"amd64\" \
		 -I/usr/lib/jvm/java-6-openjdk/include \
		 -I/usr/lib/jvm/java-6-openjdk/include/linux \
		 -Wall -Wstrict-prototypes
LDFLAGS =  -ldl -lpthread
JAVACFLAGS = 
RANLIB = ranlib
LDCMD = gcc

OBJS =  eucalyptus-bootstrap.o eucalyptus-opts.o
all: eucalyptus-cloud

.c.o:
	$(CC) $(CFLAGS) -c $< -o $@

eucalyptus-cloud: $(OBJS)
	$(LDCMD) $(LDFLAGS) $(OBJS) -o eucalyptus-cloud

ggo:
	gengetopt --input=arguments.ggo --file-name=eucalyptus-opts --func-name=arguments --arg-struct-name=eucalyptus_opts
	
clean:
	rm -f $(OBJS) eucalyptus-cloud

