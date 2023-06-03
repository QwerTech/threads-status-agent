import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class ThreadStatusAgent {

    public static final long PRINT_DELAY_MS = 3000L;
    private final static Map<Long, ThreadStateStatistics> THREAD_STATE_STATISTICS_MAP = new LinkedHashMap<>();


    public static final int INTERVAL_MILLIS = 100;

    public static void premain(String agentArgs, Instrumentation inst) {
        trackThreadsStatus();
        printThreadStatistics();
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        trackThreadsStatus();
        printThreadStatistics();
    }

    private static void trackThreadsStatus() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Runnable runnable = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                long[] threadIds = threadMXBean.getAllThreadIds();
                for (long threadId : threadIds) {
                    trackThreadStateTimeTaken(threadMXBean, threadId);
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(INTERVAL_MILLIS); // sleep for 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable, "ThreadStatusAgent-Tracker").start();
    }

    private static void printThreadStatistics() {
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                System.out.println(THREAD_STATE_STATISTICS_MAP);

            }
        };
        Timer timer = new Timer("ThreadStatusAgent-Timer", true);

        timer.scheduleAtFixedRate(task, PRINT_DELAY_MS, PRINT_DELAY_MS);
    }

    private static void trackThreadStateTimeTaken(ThreadMXBean threadMXBean, long threadId) {
        //let's skip current thread because it's not valuable
        if (Thread.currentThread().getId() == threadId) {
            return;
        }
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId);
        Thread.State threadState = threadInfo.getThreadState();
        String name = threadInfo.getThreadName();

        THREAD_STATE_STATISTICS_MAP.compute(threadId, (aLong, threadStateStatistics) -> {

            threadStateStatistics = threadStateStatistics != null ? threadStateStatistics :
                    new ThreadStateStatistics(name, threadState, System.nanoTime());

            if (threadStateStatistics.lastState != threadState) {
                threadStateStatistics.changeState(threadState, System.nanoTime());
            }
            return threadStateStatistics;
        });
    }

    private static class ThreadStateStatistics {
        public ThreadStateStatistics(String name, Thread.State lastState, long lastStatusChangedAt) {
            this.name = name;
            this.lastState = lastState;
            this.lastStatusChangedAt = lastStatusChangedAt;
            this.timeInStateMap.put(lastState, 0L);
        }

        private final String name;
        private Thread.State lastState;
        private long lastStatusChangedAt;

        private final Map<Thread.State, Long> timeInStateMap = new EnumMap<>(Thread.State.class);

        public void changeState(Thread.State threadState, long nanoTime) {
            lastState = threadState;
            timeInStateMap.compute(lastState, (state, timeInState) -> {
                long lastTimeInState = nanoTime - lastStatusChangedAt;
                if (timeInState == null) {
                    timeInState = lastTimeInState;
                } else {
                    timeInState += lastTimeInState;
                }
                return timeInState;
            });
            lastStatusChangedAt = nanoTime;
        }

        @Override
        public String toString() {
            return name + ":{" +
                    timeInStateMap.entrySet().stream().map(t -> {
                        Thread.State state = t.getKey();
                        Long timeInState = t.getValue();
                        if (state.equals(lastState)) {
                            timeInState += System.nanoTime() - lastStatusChangedAt;
                        }
                        Duration duration = Duration.ofNanos(timeInState);
                        return Arrays.toString(new Object[]{state, humanReadableFormat(duration)});
                    }).collect(Collectors.joining(",")) + "}";
        }

        public static String humanReadableFormat(Duration duration) {
            return duration.toString()
                    .substring(2)
                    .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                    .replaceAll("\\.\\d+", "")
                    .toLowerCase();
        }
    }
}
