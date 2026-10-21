package co.technove.flareplatform.paper.scheduler;

import co.technove.flareplatform.common.scheduler.IScheduler;

public final class NoOpSchedulerImpl implements IScheduler {

    @Override
    public int getPoolSize() {
        return 0;
    }
}
