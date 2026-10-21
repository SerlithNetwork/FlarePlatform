package co.technove.flareplatform.fidorial.scheduler;

import co.technove.flareplatform.common.scheduler.IScheduler;

public final class NoOpSchedulerImpl implements IScheduler {

    @Override
    public int getPoolSize() {
        return 0;
    }
}
