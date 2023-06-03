import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.ProtectionDomain;

public class ThreadStatusAgent {

    public static final int INTERVAL_MILLIS = 1000;


    public static void premain(String agentArgs, Instrumentation inst) {
        trackThreadClassMethods(inst);
//        trackThreadsStatusPeriodically();
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        trackThreadClassMethods(inst);
//        trackThreadsStatusPeriodically();
    }

    private static void trackThreadClassMethods(Instrumentation instrumentation) {

        String className = "java.lang.Thread";

        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        // see if we can get the class using forName
        try {
            targetCls = Class.forName(className);
            targetClassLoader = targetCls.getClassLoader();
            transform(targetCls, targetClassLoader, instrumentation);
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // otherwise iterate all loaded classes and find what we want
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation);
                return;
            }
        }
        throw new RuntimeException(
                "Failed to find class [" + className + "]");
    }

    private static void transform(
            Class<?> clazz,
            ClassLoader classLoader,
            Instrumentation instrumentation) {
        ThreadTransformer dt = new ThreadTransformer(
                clazz.getName(), classLoader);
        instrumentation.addTransformer(dt, true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class ThreadTransformer implements ClassFileTransformer {
        private final String targetClassName;
        private final ClassLoader targetClassLoader;

        public ThreadTransformer(String targetClassName, ClassLoader targetClassLoader) {
            this.targetClassName = targetClassName;

            this.targetClassLoader = targetClassLoader;
        }

        @Override
        public byte[] transform(
                ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {
            byte[] byteCode = classfileBuffer;
            String finalTargetClassName = this.targetClassName
                    .replaceAll("\\.", "/");
            if (!className.equals(finalTargetClassName)) {
                return byteCode;
            }


            try {
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.get(targetClassName);
                CtMethod m = cc.getDeclaredMethod("suspend");
                m.insertBefore("System.out.println(\"Thread state changed \" + this.getId() + \"/\" + this.getName() + \" state\" + this.getState());");
                byteCode = cc.toBytecode();
                cc.detach();
            } catch (NotFoundException | CannotCompileException | IOException e) {
                e.printStackTrace();
            }
            return byteCode;
        }
    }

    private static void trackThreadsStatusPeriodically() {
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


    public static void stateChanged(Thread thread) {
        System.out.println("Thread state changed " + thread.getId() + "/" + thread.getName() + " state" + thread.getState());
    }
}
