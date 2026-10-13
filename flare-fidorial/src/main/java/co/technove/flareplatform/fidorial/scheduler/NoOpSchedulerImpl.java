package co.technove.flareplatform.fidorial.scheduler;

import co.technove.flareplatform.common.scheduler.IScheduler;

public class NoOpSchedulerImpl implements IScheduler {

    @Override
    public int getPoolSize() {
        return 0;
    }

}
