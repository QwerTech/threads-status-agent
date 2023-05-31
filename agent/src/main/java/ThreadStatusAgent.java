import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadStatusAgent {

    public static final int INTERVAL_MILLIS = 1000;

    public static void premain(String agentArgs, Instrumentation inst) {
        trackThreadsStatus();
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        trackThreadsStatus();
    }

    private static void trackThreadsStatus() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Runnable runnable = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                long[] threadIds = threadMXBean.getAllThreadIds();
                for (long threadId : threadIds) {
                    printThreadStatus(threadMXBean, threadId);
                }
                System.out.println();
                try {
                    //noinspection BusyWait
                    Thread.sleep(INTERVAL_MILLIS); // sleep for 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable, "ThreadStatusAgent").start();
    }

    private static void printThreadStatus(ThreadMXBean threadMXBean, long threadId) {
        //let's skip current thread because it's not valuable
        if (Thread.currentThread().getId() == threadId) {
            return;
        }
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId);
        System.out.println("Thread " + threadId + "/" + threadInfo.getThreadName() + ": " + threadInfo.getThreadState());
    }
}
