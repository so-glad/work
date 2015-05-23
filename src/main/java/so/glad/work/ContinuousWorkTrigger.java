package so.glad.work;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Cartoon
 *         on 2015/5/5.
 */
public class ContinuousWorkTrigger implements WorkTrigger{

    private int threadNumber;
    private String workName;
    private Runnable runnable;

    private ThreadPoolExecutor executors;
    private boolean enable = false;

    public ContinuousWorkTrigger(Integer workerNumber, String workName, Runnable work){
        this.threadNumber = workerNumber == null ? 10 : workerNumber;
        this.runnable = work;
        this.workName = workName;
    }

    private void initExecutor(){
        executors = new ThreadPoolExecutor(threadNumber, threadNumber,
                0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new CustomizableThreadFactory(workName));
    }

    private void startup(){
        initExecutor();
        for(int i = 0; i < threadNumber; i++){
            runOne();
        }
    }

    private void shutdown(){
        if(executors != null){
            try {
                executors.shutdown();
            } catch (Exception ignored) {
            }
        }
    }

    private void runOne(){
        executors.submit(new Runnable() {
            @Override
            public void run() {
                while(enable) {
                    runnable.run();
                }
            }
        });
    }

    private void setEnable(boolean enable) {
        if(this.enable == enable) {
            return;
        }
        this.enable = enable;
        if(enable) {
            startup();
        } else {
            shutdown();
        }
    }

    public void setThreadNumber(int threadNumber) {
        int moreThreadNumber = threadNumber - this.threadNumber;
        this.threadNumber = threadNumber;
        if(!enable) {
            return;
        }
        if(moreThreadNumber > 0){
            executors.setCorePoolSize(threadNumber);
            executors.setMaximumPoolSize(threadNumber);
            for(int i = 0; i < moreThreadNumber; i++){
                runOne();
            }
        } else {
            setEnable(false);//Shutdown
            setEnable(true); //Startup
        }
    }

    @Override
    public void switchOn() {
        this.setEnable(true);
    }

    @Override
    public void switchOff() {
        this.setEnable(false);
    }
}
