package com.transatron.payroll;

import com.transatron.payroll.tt.TTServiceWrapperClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ThreadUtil {
    private static final Logger log = LogManager.getLogger(ThreadUtil.class);
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("Error while sleeping", e);
        }
    }

    public static void tronGridBouncePreventionSleep() {
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            log.error("Error while sleeping", e);
        }
    }
}
