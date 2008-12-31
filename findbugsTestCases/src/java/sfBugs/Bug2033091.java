package sfBugs;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bug2033091 {

static ReentrantReadWriteLock lockArray[];
static {
lockArray =
new ReentrantReadWriteLock[5];
for (int i=0; i<lockArray.length; i++)
lockArray[i] = new ReentrantReadWriteLock();
}

static void method(int n) throws IOException {

// WriteLock lock = lockArray[n].writeLock();
// lock.lock();
lockArray[n].writeLock().lock();
try {
// do some disk I/O
} finally {
// lock.unlock();
lockArray[n].writeLock().unlock();
}
}

public static void main(String[] args) throws IOException { }
}