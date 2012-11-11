
import functools
import threading
import eucaconsole
from tornado.ioloop import IOLoop

class Threads(object):
    max_threads = 1

    def __init__(self):
        self.max_threads = eucaconsole.config.getint("server", "threads.max")
        self.counter = threading.Semaphore(self.max_threads)

    @staticmethod
    def instance():
        if not hasattr(Threads, "_instance"):
            Threads._instance = Threads()
        return Threads._instance
        
    # This method kicks off a background thread calling the given target
    # and passing it the provided arguments, a tuple that should contain
    # kwargs dict and a callback function
    def runThread(self, target, args=(None, None,)):
        self.counter.acquire(True)
        t = threading.Thread(target=self.__thread_done_callback__, \
                             args=(target, args))
        #t = Thread(target=target, args=args)
        t.start()

    def __thread_done_callback__(self, target, args):
        target(args[0], args[1])
        self.counter.release()

    # This method is used to pass results back to the main thread for
    # response to the client
    def invokeCallback(self, callback, response):
        IOLoop.instance().add_callback(functools.partial(callback, response))
    
