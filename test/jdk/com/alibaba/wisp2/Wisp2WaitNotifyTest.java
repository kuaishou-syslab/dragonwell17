/*
 * @test
 * @library /test/lib
 * @summary Test Object.wait/notify with coroutine in wisp2
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 Wisp2WaitNotifyTest
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static jdk.test.lib.Asserts.assertEQ;

public class Wisp2WaitNotifyTest {

    private AtomicInteger seq = new AtomicInteger();
    private int finishCnt = 0;
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean fooCond = false;

    public static void main(String[] args) throws Exception {
        Wisp2WaitNotifyTest s = new Wisp2WaitNotifyTest();
        synchronized (s) {
            WispEngine.dispatch(s::foo);
            assertEQ(s.seq.getAndIncrement(), 0);
        }

        s.latch.await();

        assertEQ(s.seq.getAndIncrement(), 5);
        synchronized (s) {
            while (s.finishCnt < 2) {
                s.wait();
            }
        }
        assertEQ(s.seq.getAndIncrement(), 6);
    }

    private synchronized void foo() {
        assertEQ(seq.getAndIncrement(), 1);

        WispEngine.dispatch(this::bar);
        assertEQ(seq.getAndIncrement(), 2);
        try {
            while (!fooCond) {
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEQ(seq.getAndIncrement(), 4);

        latch.countDown();
        finishCnt++;
        notifyAll();
    }

    private void bar() {
        synchronized (this) {
            assertEQ(seq.getAndIncrement(), 3);
            fooCond = true;
            notifyAll();
        }
        synchronized (this) {
            finishCnt++;
            notifyAll();
        }
    }
}
