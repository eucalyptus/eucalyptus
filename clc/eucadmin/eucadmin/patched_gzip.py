"""Functions that read and write gzipped files.

The user of the file doesn't have to worry about the compression,
but random access is not allowed."""

# This is revision 53772 plus the 'gzip.py.diff' patch from:
# http://bugs.python.org/issue1675951

# based on Andrew Kuchling's minigzip.py distributed with the zlib module

# Original copyright statement:
# PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2
# --------------------------------------------
#
# 1. This LICENSE AGREEMENT is between the Python Software Foundation
# ("PSF"), and the Individual or Organization ("Licensee") accessing
# and otherwise using this software ("Python") in source or binary
# form and its associated documentation.
#
# 2. Subject to the terms and conditions of this License Agreement,
# PSF hereby grants Licensee a nonexclusive, royalty-free, world-wide
# license to reproduce, analyze, test, perform and/or display
# publicly, prepare derivative works, distribute, and otherwise
# use Python alone or in any derivative version, provided, however,
# that PSF's License Agreement and PSF's notice of copyright, i.e.,
# "Copyright (c) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008,
# 2009 Python Software Foundation; All Rights Reserved" are retained
# in Python alone or in any derivative version prepared by Licensee.
#
# 3. In the event Licensee prepares a derivative work that is based
# on or incorporates Python or any part thereof, and wants to make
# the derivative work available to others as provided herein, then
# Licensee hereby agrees to include in any such work a brief summary
# of the changes made to Python.
#
# 4. PSF is making Python available to Licensee on an "AS IS" basis.
# PSF MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED.
# BY WAY OF EXAMPLE, BUT NOT LIMITATION, PSF MAKES NO AND DISCLAIMS
# ANY REPRESENTATION OR WARRANTY OF MERCHANTABILITY OR FITNESS FOR
# ANY PARTICULAR PURPOSE OR THAT THE USE OF PYTHON WILL NOT INFRINGE
# ANY THIRD PARTY RIGHTS.
#
# 5. PSF SHALL NOT BE LIABLE TO LICENSEE OR ANY OTHER USERS OF PYTHON
# FOR ANY INCIDENTAL, SPECIAL, OR CONSEQUENTIAL DAMAGES OR LOSS AS
# A RESULT OF MODIFYING, DISTRIBUTING, OR OTHERWISE USING PYTHON, OR
# ANY DERIVATIVE THEREOF, EVEN IF ADVISED OF THE POSSIBILITY THEREOF.
#
# 6. This License Agreement will automatically terminate upon a
# material breach of its terms and conditions.
#
# 7. Nothing in this License Agreement shall be deemed to create any
# relationship of agency, partnership, or joint venture between PSF
# and Licensee.  This License Agreement does not grant permission to
# use PSF trademarks or trade name in a trademark sense to endorse
# or promote products or services of Licensee, or any third party.
#
# 8. By copying, installing or otherwise using Python, Licensee agrees
# to be bound by the terms and conditions of this License Agreement.

import struct, sys, time
import zlib
import __builtin__

__all__ = ["GzipFile","open"]

FTEXT, FHCRC, FEXTRA, FNAME, FCOMMENT = 1, 2, 4, 8, 16

READ, WRITE = 1, 2

def LOWU32(i):
    """Return the low-order 32 bits of an int, as a non-negative int."""
    return i & 0xFFFFFFFFL

def write32(output, value):
    output.write(struct.pack("<l", value))

def write32u(output, value):
    # The L format writes the bit pattern correctly whether signed
    # or unsigned.
    output.write(struct.pack("<L", value))

def open(filename, mode="rb", compresslevel=9):
    """Shorthand for GzipFile(filename, mode, compresslevel).

    The filename argument is required; mode defaults to 'rb'
    and compresslevel defaults to 9.

    """
    return GzipFile(filename, mode, compresslevel)

class PaddedFile:
    """readonly file object that prepends string the content of f"""
    def __init__(self, string, f):
        self._string = string
        self._length = len(string)
        self._file = f
        self._read = 0

    def read(self, size):
        if self._read is None:
            return self._file.read(size)
        if self._read + size <= self._length:
            read = self._read
            self._read += size
            return self._string[read:self._read]
        else:
            read = self._read
            self._read = None
            return self._string[read:] + \
                   self._file.read(size-self._length+read)

    def unused(self):
        if self._read is None:
            return ''
        return self._string[self._read:]
        
class GzipFile:
    """The GzipFile class simulates most of the methods of a file object with
    the exception of the readinto() and truncate() methods.

    """

    myfileobj = None

    def __init__(self, filename=None, mode=None,
                 compresslevel=9, fileobj=None):
        """Constructor for the GzipFile class.

        At least one of fileobj and filename must be given a
        non-trivial value.

        The new class instance is based on fileobj, which can be a regular
        file, a StringIO object, or any other object which simulates a file.
        It defaults to None, in which case filename is opened to provide
        a file object.

        When fileobj is not None, the filename argument is only used to be
        included in the gzip file header, which may includes the original
        filename of the uncompressed file.  It defaults to the filename of
        fileobj, if discernible; otherwise, it defaults to the empty string,
        and in this case the original filename is not included in the header.

        The mode argument can be any of 'r', 'rb', 'a', 'ab', 'w', or 'wb',
        depending on whether the file will be read or written.  The default
        is the mode of fileobj if discernible; otherwise, the default is 'rb'.
        Be aware that only the 'rb', 'ab', and 'wb' values should be used
        for cross-platform portability.

        The compresslevel argument is an integer from 1 to 9 controlling the
        level of compression; 1 is fastest and produces the least compression,
        and 9 is slowest and produces the most compression.  The default is 9.

        """

        # guarantee the file is opened in binary mode on platforms
        # that care about that sort of thing
        if mode and 'b' not in mode:
            mode += 'b'
        if fileobj is None:
            fileobj = self.myfileobj = __builtin__.open(filename, mode or 'rb')
        if filename is None:
            if hasattr(fileobj, 'name'): filename = fileobj.name
            else: filename = ''
        if mode is None:
            if hasattr(fileobj, 'mode'): mode = fileobj.mode
            else: mode = 'rb'

        if mode[0:1] == 'r':
            self.mode = READ
            # Set flag indicating start of a new member
            self._new_member = True

            self.decompobj = zlib.decompressobj(-zlib.MAX_WBITS)
            self.crcval = zlib.crc32("")
            self.buffer = []   # List of data blocks
            self.bufferlen = 0
            self.pos = 0       # Offset of next data to read from buffer[0]
            
            self.name = filename

        elif mode[0:1] == 'w' or mode[0:1] == 'a':
            self.mode = WRITE
            self._init_write(filename)
            self.compress = zlib.compressobj(compresslevel,
                                             zlib.DEFLATED,
                                             -zlib.MAX_WBITS,
                                             zlib.DEF_MEM_LEVEL,
                                             0)
        else:
            raise IOError, "Mode " + mode + " not supported"

        self.size = 0
        self.fileobj = fileobj
        self.offset = 0

        if self.mode == WRITE:
            self._write_gzip_header()

    @property
    def filename(self):
        import warnings
        warnings.warn("use the name attribute", DeprecationWarning)
        if self.mode == WRITE and self.name[-3:] != ".gz":
            return self.name + ".gz"
        return self.name

    def __repr__(self):
        s = repr(self.fileobj)
        return '<gzip ' + s[1:-1] + ' ' + hex(id(self)) + '>'

    def _init_write(self, filename):
        self.name = filename
        self.crc = zlib.crc32("")
        self.writebuf = []
        self.bufsize = 0

    def _write_gzip_header(self):
        self.fileobj.write('\037\213')             # magic header
        self.fileobj.write('\010')                 # compression method
        fname = self.name
        if fname.endswith(".gz"):
            fname = fname[:-3]
        flags = 0
        if fname:
            flags = FNAME
        self.fileobj.write(chr(flags))
        write32u(self.fileobj, long(time.time()))
        self.fileobj.write('\002')
        self.fileobj.write('\377')
        if fname:
            self.fileobj.write(fname + '\000')

    def _read_gzip_header(self, f):
        magic = f.read(2)
        if magic != '\037\213':
            raise IOError, 'Not a gzipped file'
        method = ord( f.read(1) )
        if method != 8:
            raise IOError, 'Unknown compression method'
        flag = ord( f.read(1) )
        # modtime = f.read(4)
        # extraflag = f.read(1)
        # os = f.read(1)
        f.read(6)

        if flag & FEXTRA:
            # Read & discard the extra field, if present
            xlen = ord(f.read(1))
            xlen = xlen + 256*ord(f.read(1))
            f.read(xlen)
        if flag & FNAME:
            # Read and discard a null-terminated string containing the filename
            while True:
                s = f.read(1)
                if not s or s=='\000':
                    break
        if flag & FCOMMENT:
            # Read and discard a null-terminated string containing a comment
            while True:
                s = f.read(1)
                if not s or s=='\000':
                    break
        if flag & FHCRC:
            f.read(2)     # Read & discard the 16-bit header CRC


    def write(self,data):
        if self.mode != WRITE:
            import errno
            raise IOError(errno.EBADF, "write() on read-only GzipFile object")

        if self.fileobj is None:
            raise ValueError, "write() on closed GzipFile object"
        if len(data) > 0:
            self.size = self.size + len(data)
            self.crc = zlib.crc32(data, self.crc)
            self.fileobj.write( self.compress.compress(data) )
            self.offset += len(data)

    def _read_eof(self):
        if len(self.decompobj.unused_data) < 8:
            unused = self.decompobj.unused_data  + \
                     self.fileobj.read(8-len(self.decompobj.unused_data))
            if len(unused)<8:
                raise IOError, "Unexpected EOF"
        else:
            unused = self.decompobj.unused_data
        crc32 = struct.unpack(
            "<L", unused[0:4])[0] #le signed int
        isize = struct.unpack(
            "<L", unused[4:8])[0] #le unsigned int

        # Do the CRC check with a LOWU32 on the read self.crcval due to the
        # signed/unsigned problem of the zlib crc code.
        if crc32 != LOWU32(self.crcval):
            raise IOError, "CRC check failed"
        if isize != LOWU32(self.size):
            raise IOError, "Incorrect length of data produced"
        if len(unused) > 8:
            f = PaddedFile(unused[8:], self.fileobj)
        else:
            f = self.fileobj
        try:
            self._read_gzip_header(f)
        except IOError:
            return ''
        if len(unused) > 8:
            return f.unused()
        else:
            return ''
        

    def _read(self, readsize):
        data = self.fileobj.read(readsize)

        while True:
            if data == "":
                decompdata = self.decompobj.flush()
            else:
                decompdata = self.decompobj.decompress(data)
            decomplen = len(decompdata)
            self.buffer.append(decompdata)
            self.bufferlen += decomplen
            self.size += decomplen
            self.crcval = zlib.crc32(decompdata, self.crcval)
            if self.decompobj.unused_data:
                data = self._read_eof()
                self.decompobj = zlib.decompressobj(-zlib.MAX_WBITS)
                self.crcval = zlib.crc32("")
                self.size = 0
                if data:
                    continue
            break
        return data==''

    def read(self, size=-1):
        """Decompress up to bytes bytes from input.

        Raise IOError."""

        if self.mode != READ:
            import errno
            raise IOError(errno.EBADF, "read() on write-only GzipFile object")

        if self._new_member:
            self._read_gzip_header(self.fileobj)
            self._new_member = False

        while size < 0 or self.bufferlen <  size:
            if size < 0:
                readsize = 65536 - self.bufferlen
            else:
                readsize = size - self.bufferlen
                
            if readsize > 65536:
                readsize = 32768
            elif readsize > 32768:
                readsize = 16384
            elif readsize > 16384:
                readsize = 8192
            elif readsize > 8192:
                readsize = 4096
            elif readsize > 4096:
                readsize = 2048
            else:
                readsize = 1024

            eof = self._read(readsize)
            if eof:
                break
        if size < 0:
            size = self.bufferlen
        retdata = ""
        while size > 0 and self.buffer:
            decompdata = self.buffer[0]
            decomplen = len(decompdata)
            if size+self.pos <= decomplen:
                tmpdata = decompdata[self.pos:size+self.pos]
                retdata += tmpdata
                self.bufferlen -= size
                self.pos += size
                break
            decomplen -= self.pos
            size -= decomplen
            self.bufferlen -= decomplen
            if self.pos != 0:
                retdata += decompdata[self.pos:]
            else:
                retdata += decompdata
            self.pos = 0
            self.buffer.pop(0)
        self.offset += len(retdata)
        return retdata

    def close(self):
        if self.mode == WRITE:
            self.fileobj.write(self.compress.flush())
            # The native zlib crc is an unsigned 32-bit integer, but
            # the Python wrapper implicitly casts that to a signed C
            # long.  So, on a 32-bit box self.crc may "look negative",
            # while the same crc on a 64-bit box may "look positive".
            # To avoid irksome warnings from the `struct` module, force
            # it to look positive on all boxes.
            write32u(self.fileobj, LOWU32(self.crc))
            # self.size may exceed 2GB, or even 4GB
            write32u(self.fileobj, LOWU32(self.size))
            self.fileobj = None
        elif self.mode == READ:
            self.fileobj = None
        if self.myfileobj:
            self.myfileobj.close()
            self.myfileobj = None

    def __del__(self):
        try:
            if (self.myfileobj is None and
                self.fileobj is None):
                return
        except AttributeError:
            return
        self.close()

    def flush(self,zlib_mode=zlib.Z_SYNC_FLUSH):
        if self.mode == WRITE:
            # Ensure the compressor's buffer is flushed
            self.fileobj.write(self.compress.flush(zlib_mode))
        self.fileobj.flush()

    def fileno(self):
        """Invoke the underlying file object's fileno() method.

        This will raise AttributeError if the underlying file object
        doesn't support fileno().
        """
        return self.fileobj.fileno()

    def isatty(self):
        return False

    def tell(self):
        return self.offset

    def rewind(self):
        '''Return the uncompressed stream file position indicator to the
        beginning of the file'''
        if self.mode != READ:
            raise IOError("Can't rewind in write mode")
        self.fileobj.seek(0)
        self._new_member = True
        self.decompobj = zlib.decompressobj(-zlib.MAX_WBITS)
        self.crcval = zlib.crc32("")
        self.buffer = []   # List of data blocks
        self.bufferlen = 0
        self.pos = 0       # Offset of next data to read from buffer[0]
        self.offset = 0
        self.size = 0

    def seek(self, offset, whence=0):
        if whence:
            if whence == 1:
                offset = self.offset + offset
            else:
                raise ValueError('Seek from end not supported')
        if self.mode == WRITE:
            if offset < self.offset:
                raise IOError('Negative seek in write mode')
            count = offset - self.offset
            for i in range(count // 1024):
                self.write(1024 * '\0')
            self.write((count % 1024) * '\0')
        elif self.mode == READ:
            if offset < self.offset:
                # for negative seek, rewind and do positive seek
                self.rewind()
            count = offset - self.offset
            for i in range(count // 1024):
                self.read(1024)
            self.read(count % 1024)

    def readline(self, size=-1):
        if self._new_member:
            self._read_gzip_header(self.fileobj)
            self._new_member = False

        scansize = 0
        buffpos = 0
        while True:
            for idx in range(buffpos, len(self.buffer)):
                if idx == 0:
                    scansize -= self.pos
                    pos = self.buffer[idx].find('\n', self.pos)
                else:
                    pos = self.buffer[idx].find('\n')                    
                if pos != -1:
                    if size>=0 and scansize+pos+1>size:
                        return self.read(size)
                    return self.read(scansize+pos+1)
                scansize += len(self.buffer[idx])
                if size>=0 and scansize>size:
                    return self.read(size)
            buffpos = len(self.buffer)
            eof = self._read(1024)
            if eof:
                return self.read(scansize)

    def readlines(self, sizehint=0):
        # Negative numbers result in reading all the lines
        if sizehint <= 0:
            sizehint = sys.maxint
        L = []
        while sizehint > 0:
            line = self.readline()
            if line == "":
                break
            L.append(line)
            sizehint = sizehint - len(line)

        return L

    def writelines(self, L):
        for line in L:
            self.write(line)

    def __iter__(self):
        return self

    def next(self):
        line = self.readline()
        if line:
            return line
        else:
            raise StopIteration


def _test():
    # Act like gzip; with -d, act like gunzip.
    # The input file is not deleted, however, nor are any other gzip
    # options or features supported.
    args = sys.argv[1:]
    decompress = args and args[0] == "-d"
    if decompress:
        args = args[1:]
    if not args:
        args = ["-"]
    for arg in args:
        if decompress:
            if arg == "-":
                f = GzipFile(filename="", mode="rb", fileobj=sys.stdin)
                g = sys.stdout
            else:
                if arg[-3:] != ".gz":
                    print "filename doesn't end in .gz:", repr(arg)
                    continue
                f = open(arg, "rb")
                g = __builtin__.open(arg[:-3], "wb")
        else:
            if arg == "-":
                f = sys.stdin
                g = GzipFile(filename="", mode="wb", fileobj=sys.stdout)
            else:
                f = __builtin__.open(arg, "rb")
                g = open(arg + ".gz", "wb")
        while True:
            chunk = f.read(1024)
            if not chunk:
                break
            g.write(chunk)
        if g is not sys.stdout:
            g.close()
        if f is not sys.stdin:
            f.close()

if __name__ == '__main__':
    _test()

