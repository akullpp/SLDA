from marshal import dump, load
from os import path, makedirs
from time import time

# Marshall
def dumpStruct(filename, data):
    print "Dumping structure %s" % filename

    createPath(filename)

    with open(filename, "wb") as fout:
        dump(data, fout)


# Unmarshall
def loadStruct(filename):
    print "Loading structure %s" % filename

    with open(filename, "rb") as fin:
        return load(fin)


# Create path
def createPath(filename):
    fpath = filename.rsplit("/", 1)[0]

    if not path.exists(fpath):
        makedirs(fpath)


# Timer decorator
def timer(func):
    def wrapper(*arg):
        t = time()
        r = func(*arg)
        print func.func_name, time() - t
        return r
    return wrapper