import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SchedulerTest {
    private static Logger LOG = LoggerFactory.getLogger(SchedulerTest.class);

    @Test
    public void schedulerExecutionException() throws Exception {
        System.out.println("Test: schedulerExecutionException");

        ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(2);
        sched.setRemoveOnCancelPolicy(true);

        ScheduledFuture future1 = sched.scheduleAtFixedRate(new Runnable() {
            int counter = 0;
            @Override
            public void run() {
                System.out.println("Runnable 1: "+ ++counter);

                if (counter >= 2) {
                    System.out.println("Runnable 1: BOOOM");
                    throw new RuntimeException("boom");
                }

            }
        }, 1, 1, TimeUnit.SECONDS);

        ScheduledFuture future2 = sched.scheduleAtFixedRate(new Runnable() {
            int counter = 0;
            @Override
            public void run() {
                System.out.println("Runnable 2: "+ ++counter);
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        long cutoff = new Date().getTime() + 6000;

        while (new Date().getTime() < cutoff) {
            System.out.println("Scheduler Queue size: "+ sched.getQueue().size());
            System.out.println("Future 1: is "+ (future1.isCancelled() ? "" : "not ") +"cancelled, is "+ (future1.isDone()? "" : "not ") +"done");
            System.out.println("Future 2: is "+ (future2.isCancelled() ? "" : "not ") +"cancelled, is "+ (future2.isDone()? "" : "not ") +"done");
            Thread.sleep(1000);
        }
        assertEquals(sched.getQueue().size(), 1);

        future2.cancel(true);
        System.out.println("Scheduler Queue size: "+ sched.getQueue().size());
        System.out.println("Future 1: is "+ (future1.isCancelled() ? "" : "not ") +"cancelled, is "+ (future1.isDone()? "" : "not ") +"done");
        System.out.println("Future 2: is "+ (future2.isCancelled() ? "" : "not ") +"cancelled, is "+ (future2.isDone()? "" : "not ") +"done");

        assertEquals(sched.getQueue().size(), 0);

        sched.shutdownNow();
    }
}