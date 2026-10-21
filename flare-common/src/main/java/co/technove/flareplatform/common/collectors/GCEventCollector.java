package co.technove.flareplatform.common.collectors;

import co.technove.flare.Flare;
import co.technove.flare.live.CollectorData;
import co.technove.flare.live.EventCollector;
import co.technove.flare.live.LiveEvent;
import co.technove.flare.live.category.GraphCategory;
import co.technove.flare.live.formatter.DataFormatter;
import com.google.common.collect.ImmutableMap;
import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public final class GCEventCollector extends EventCollector implements NotificationListener {

    private static final CollectorData MINOR_GC = new CollectorData("builtin:gc:minor", "Minor GC", "A small pause in the program to allow Garbage Collection to run.", DataFormatter.MILLISECONDS, GraphCategory.SYSTEM);
    private static final CollectorData MAJOR_GC = new CollectorData("builtin:gc:major", "Major GC", "A large pause in the program to allow Garbage Collection to run.", DataFormatter.MILLISECONDS, GraphCategory.SYSTEM);
    private static final CollectorData UNKNOWN_GC = new CollectorData("builtin:gc:generic", "Major GC", "A run of the Garbage Collection.", DataFormatter.MILLISECONDS, GraphCategory.SYSTEM);

    public GCEventCollector() {
        super(MINOR_GC, MAJOR_GC, UNKNOWN_GC);
    }

    private static CollectorData fromString(final String string) {
        if (string.endsWith("minor GC")) {
            return MINOR_GC;
        } else if (string.endsWith("major GC")) {
            return MAJOR_GC;
        }
        return UNKNOWN_GC;
    }

    @Override
    public void start(final Flare flare) {
        for (final GarbageCollectorMXBean garbageCollectorBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            final NotificationEmitter notificationEmitter = (NotificationEmitter) garbageCollectorBean;
            notificationEmitter.addNotificationListener(this, null, null);
        }
    }

    @Override
    public void stop(final Flare flare) {
        for (final GarbageCollectorMXBean garbageCollectorBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            final NotificationEmitter notificationEmitter = (NotificationEmitter) garbageCollectorBean;
            try {
                notificationEmitter.removeNotificationListener(this);
            } catch (final ListenerNotFoundException ignored) {
            }
        }
    }

    @Override
    public void handleNotification(final Notification notification, final Object o) {
        if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            final GarbageCollectionNotificationInfo gcInfo = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
            reportEvent(new LiveEvent(fromString(gcInfo.getGcAction()), System.currentTimeMillis(), (int) gcInfo.getGcInfo().getDuration(), ImmutableMap.of()));
        }
    }
}
