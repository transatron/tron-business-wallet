package com.transatron.payroll;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

public class Model {
    private static final Logger log = LogManager.getLogger(Model.class);

    public static final String salt = "268f94e1ab1573d65f0c6f68d83a2818";
    public enum Action{
        GET_RESOURCES,
        RUN_DISTRIBUTION,
        EXPORT_RESULTS,
        RE_CHECK_AVAILABLE_RESOURCES,
        AUTO_EXPORT_RESULTS
    }

    private Set<IMPropertyListener> listeners;

    private ExecutorService executorService;

    private ScheduledExecutorService scheduler ;

    private Map<String,Object> properties ;

    public Model(){
        this(new Properties());
    }

    private static class ModelThreadFactory implements java.util.concurrent.ThreadFactory {
        private int counter = 0;
        private String name;

        public ModelThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Runnable r1 = () -> {
                try {
                    r.run();
                } catch (Throwable e) {
                    log.error("Error in model executor thread", e);
                }
            };
            return new Thread(r1,  name+"-" + counter++);
        }
    }

    public Model(Properties applicationSettings) {
        listeners = Collections.synchronizedSet(new HashSet<>());
        properties = Collections.synchronizedMap(new HashMap<>());
        executorService = Executors.newCachedThreadPool(new ModelThreadFactory("ModelThread"));
        scheduler = Executors.newScheduledThreadPool(2, new ModelThreadFactory("ModelSchedulerThread"));
        initFromSettings(applicationSettings);
    }

    private void initFromSettings(Properties applicationSettings){
        for(String key : applicationSettings.stringPropertyNames())
            properties.put(key,applicationSettings.getProperty(key));
    }

    public void destroy() {
        listeners.clear();
        executorService.shutdown();
        scheduler.shutdown();
    }

    public void addListener(IMPropertyListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IMPropertyListener listener) {
        listeners.remove(listener);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object newValue) {
        Object oldValue = properties.get(key);
        properties.put(key, newValue);
        Set<IMPropertyListener> copySet;
        synchronized (listeners){
            copySet = new HashSet<>(listeners);
        }
        for(IMPropertyListener listener : copySet)
            executorService.submit(() -> listener.onPropertyChanged(key, oldValue, newValue));
    }

    public void setAppStatus(PayrollFrame.Status appStatus, String statusMessage, int progress){
        properties.put(IMProperty.APP_STATUS_MESSAGE, statusMessage);
        properties.put(IMProperty.APP_STATUS_PROGRESS, progress);
        setProperty(IMProperty.APP_STATUS, appStatus);
    }


    public void scheduleRecurrent(Runnable task, long period){
         scheduler.scheduleWithFixedDelay(task, 0, period, TimeUnit.MILLISECONDS);
    }

}
