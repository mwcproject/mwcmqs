package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class Memory {
    private static Logger log = LoggerFactory.getLogger(Memory.class);
    private static Thread memSizeMonThread = null;

    public static void startMonitoringThread()
    {
        if (memSizeMonThread!=null)
            return;

        memSizeMonThread = new Thread() {
            @Override
            public void run()
            {
                setName("memSizeMonThread");
                while(true)
                {
                    try
                    {
                        Thread.sleep(60000); // Every minute seconds to check

                        {
                            log.info("MEMPROFILE: Runtime  totalMemory(mb)=" +
                                    Runtime.getRuntime().totalMemory()/1024/1024 + " freeMemory(mb)=" +
                                    Runtime.getRuntime().freeMemory()/1024/1024);

                            // Additional mem usage info
                            MemoryMXBean memInfo = ManagementFactory.getMemoryMXBean();

                            log.info("MEMPROFILE: heapUsage(mb)=" +
                                    memInfo.getHeapMemoryUsage().getUsed()/1024/1024 + " heapMax(mb)=" + memInfo.getHeapMemoryUsage().getMax()/1024/1024 + " nonHeapUsage(mb)=" + memInfo.getNonHeapMemoryUsage().getUsed()/1024/1024 +
                                    " nonHeapMax = " + memInfo.getNonHeapMemoryUsage().getMax() );
                        }
                    }
                    catch (Exception ex) {
                        log.info("memSizeMonThread failed", ex);
                    }
                }
            }
        };
        memSizeMonThread.setDaemon(true);
        memSizeMonThread.start();
    }
}
