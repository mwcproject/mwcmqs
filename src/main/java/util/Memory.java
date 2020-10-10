package util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Memory {
    private static Logger log = Logger.getLogger("Memory");
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
                            log.log(Level.INFO,"MEMPROFILE: Runtime  totalMemory(mb)=" +
                                    Runtime.getRuntime().totalMemory()/1024/1024 + " freeMemory(mb)=" +
                                    Runtime.getRuntime().freeMemory()/1024/1024);

                            // Additional mem usage info
                            MemoryMXBean memInfo = ManagementFactory.getMemoryMXBean();

                            log.log(Level.INFO,"MEMPROFILE: heapUsage(mb)=" +
                                    memInfo.getHeapMemoryUsage().getUsed()/1024/1024 + " heapMax(mb)=" + memInfo.getHeapMemoryUsage().getMax()/1024/1024 + " nonHeapUsage(mb)=" + memInfo.getNonHeapMemoryUsage().getUsed()/1024/1024 +
                                    " nonHeapMax = " + memInfo.getNonHeapMemoryUsage().getMax() );
                        }
                    }
                    catch (Exception ex) {
                        log.log(Level.SEVERE,"memSizeMonThread failed", ex);
                    }
                }
            }
        };
        memSizeMonThread.setDaemon(true);
        memSizeMonThread.start();
    }
}
