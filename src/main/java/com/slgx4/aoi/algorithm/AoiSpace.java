package com.slgx4.aoi.algorithm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * cloudwu/aoi 的 Java 语义移植。
 *
 * <p>对应参考提交 54660b509d6f91bba24d0307c565a5f355508812。保留固定半径、
 * w/m/wm/d 协议、关键点移动判定、热点对与版本失效算法。原版的自定义分配器和
 * uint32 哈希表在 Java 中分别由 GC 与标准 Map 代替。</p>
 *
 * <p>与原 C 实现相同，本类不是线程安全的，回调中不应递归调用 message。</p>
 */
public final class AoiSpace implements AutoCloseable {
    public static final float AOI_RADIUS = 10.0f;
    public static final float SHIFT_RADIUS = AOI_RADIUS * 0.5f;
    public static final float HOT_PAIR_RADIUS = AOI_RADIUS * 2.0f;
    public static final long MAX_ID = 0xffff_ffffL;

    private static final float AOI_RADIUS_SQUARED = AOI_RADIUS * AOI_RADIUS;
    private static final int MODE_WATCHER = 1;
    private static final int MODE_MARKER = 2;
    private static final int MODE_MOVE = 4;
    private static final int MODE_DROP = 8;

    private final Map<Long, AoiObject> objects = new LinkedHashMap<>();
    private final List<AoiObject> watcherStatic = new ArrayList<>();
    private final List<AoiObject> markerStatic = new ArrayList<>();
    private final List<AoiObject> watcherMove = new ArrayList<>();
    private final List<AoiObject> markerMove = new ArrayList<>();
    private final LinkedList<HotPair> hotPairs = new LinkedList<>();
    private boolean closed;

    /** 对应 aoi_new() / 使用默认分配器的 aoi_create()。 */
    public static AoiSpace create() {
        return new AoiSpace();
    }

    /** 对应 aoi_update(space, id, mode, pos)。 */
    public void update(long id, String mode, AoiPosition position) {
        ensureOpen();
        validateId(id);
        Objects.requireNonNull(mode, "AOI mode 不能为空");
        Objects.requireNonNull(position, "AOI position 不能为空");

        AoiObject object = queryObject(id);
        boolean setWatcher = false;
        boolean setMarker = false;
        for (int index = 0; index < mode.length(); index++) {
            switch (mode.charAt(index)) {
                case 'w' -> setWatcher = true;
                case 'm' -> setMarker = true;
                case 'd' -> {
                    if ((object.mode & MODE_DROP) == 0) {
                        object.mode = MODE_DROP;
                        dropObject(object);
                    }
                    return;
                }
                default -> {
                    // 原实现忽略未知字符。
                }
            }
        }

        if ((object.mode & MODE_DROP) != 0) {
            object.mode &= ~MODE_DROP;
            grabObject(object);
        }

        boolean changed = changeMode(object, setWatcher, setMarker);
        object.position = position;
        if (changed || !isNear(position, object.lastKeyPosition)) {
            object.lastKeyPosition = position;
            object.mode |= MODE_MOVE;
            object.version++;
        }
    }

    /** 方便迁移原 C 调用点的 float[3] 重载。 */
    public void update(long id, String mode, float[] position) {
        Objects.requireNonNull(position, "AOI position 不能为空");
        if (position.length < 3) {
            throw new IllegalArgumentException("AOI position 至少需要 3 个分量");
        }
        update(id, mode, new AoiPosition(position[0], position[1], position[2]));
    }

    /** 对应 aoi_message(space, callback, ud)。 */
    public void message(AoiCallback callback) {
        ensureOpen();
        Objects.requireNonNull(callback, "AOI callback 不能为空");

        flushPairs(callback);
        watcherStatic.clear();
        watcherMove.clear();
        markerStatic.clear();
        markerMove.clear();

        // 快照避免用户回调更新对象时破坏标准 Map 的迭代器。
        for (AoiObject object : new ArrayList<>(objects.values())) {
            pushToSets(object);
        }
        generatePairList(watcherStatic, markerMove, callback);
        generatePairList(watcherMove, markerStatic, callback);
        generatePairList(watcherMove, markerMove, callback);
    }

    /** Java 便捷接口：执行一个 tick 并直接返回本 tick 的全部 AOI 消息。 */
    public List<AoiEvent> message() {
        List<AoiEvent> events = new ArrayList<>();
        message((watcherId, markerId) -> events.add(new AoiEvent(watcherId, markerId)));
        return List.copyOf(events);
    }

    public List<AoiObjectSnapshot> objects() {
        ensureOpen();
        return objects.values().stream()
                .map(AoiSpace::snapshot)
                .sorted((first, second) -> Long.compareUnsigned(first.id(), second.id()))
                .toList();
    }

    public Optional<AoiObjectSnapshot> object(long id) {
        ensureOpen();
        validateId(id);
        return Optional.ofNullable(objects.get(id)).map(AoiSpace::snapshot);
    }

    public List<AoiHotPairSnapshot> hotPairs() {
        ensureOpen();
        return hotPairs.stream()
                .map(pair -> new AoiHotPairSnapshot(pair.watcher.id, pair.marker.id))
                .toList();
    }

    public AoiStats stats() {
        ensureOpen();
        return new AoiStats(objects.size(), hotPairs.size(), watcherStatic.size(), watcherMove.size(),
                markerStatic.size(), markerMove.size());
    }

    /** 对应 aoi_release。 */
    public void release() {
        close();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        hotPairs.clear();
        watcherStatic.clear();
        watcherMove.clear();
        markerStatic.clear();
        markerMove.clear();
        objects.clear();
        closed = true;
    }

    private AoiObject queryObject(long id) {
        return objects.computeIfAbsent(id, AoiObject::new);
    }

    private boolean changeMode(AoiObject object, boolean setWatcher, boolean setMarker) {
        boolean changed = false;
        if (object.mode == 0) {
            if (setWatcher) {
                object.mode = MODE_WATCHER;
            }
            if (setMarker) {
                object.mode |= MODE_MARKER;
            }
            return true;
        }

        if (setWatcher && (object.mode & MODE_WATCHER) == 0) {
            object.mode |= MODE_WATCHER;
            changed = true;
        } else if (!setWatcher && (object.mode & MODE_WATCHER) != 0) {
            object.mode &= ~MODE_WATCHER;
            changed = true;
        }
        if (setMarker && (object.mode & MODE_MARKER) == 0) {
            object.mode |= MODE_MARKER;
            changed = true;
        } else if (!setMarker && (object.mode & MODE_MARKER) != 0) {
            object.mode &= ~MODE_MARKER;
            changed = true;
        }
        return changed;
    }

    private void flushPairs(AoiCallback callback) {
        ListIterator<HotPair> iterator = hotPairs.listIterator();
        while (iterator.hasNext()) {
            HotPair pair = iterator.next();
            boolean stale = pair.watcher.version != pair.watcherVersion
                    || pair.marker.version != pair.markerVersion
                    || (pair.watcher.mode & MODE_DROP) != 0
                    || (pair.marker.mode & MODE_DROP) != 0;
            if (stale) {
                iterator.remove();
                dropPair(pair);
                continue;
            }

            float distanceSquared = distanceSquared(pair.watcher, pair.marker);
            if (distanceSquared > AOI_RADIUS_SQUARED * 4) {
                iterator.remove();
                dropPair(pair);
            } else if (distanceSquared < AOI_RADIUS_SQUARED) {
                callback.onMessage(pair.watcher.id, pair.marker.id);
                iterator.remove();
                dropPair(pair);
            }
        }
    }

    private void pushToSets(AoiObject object) {
        int mode = object.mode;
        if ((mode & MODE_WATCHER) != 0) {
            if ((mode & MODE_MOVE) != 0) {
                watcherMove.add(object);
                object.mode &= ~MODE_MOVE;
            } else {
                watcherStatic.add(object);
            }
        }
        if ((mode & MODE_MARKER) != 0) {
            if ((mode & MODE_MOVE) != 0) {
                markerMove.add(object);
                object.mode &= ~MODE_MOVE;
            } else {
                markerStatic.add(object);
            }
        }
    }

    private void generatePairList(List<AoiObject> watchers, List<AoiObject> markers,
                                  AoiCallback callback) {
        for (AoiObject watcher : watchers) {
            for (AoiObject marker : markers) {
                generatePair(watcher, marker, callback);
            }
        }
    }

    private void generatePair(AoiObject watcher, AoiObject marker, AoiCallback callback) {
        if (watcher == marker) {
            return;
        }
        float distanceSquared = distanceSquared(watcher, marker);
        if (distanceSquared < AOI_RADIUS_SQUARED) {
            callback.onMessage(watcher.id, marker.id);
            return;
        }
        if (distanceSquared > AOI_RADIUS_SQUARED * 4) {
            return;
        }

        HotPair pair = new HotPair(watcher, marker, watcher.version, marker.version);
        grabObject(watcher);
        grabObject(marker);
        hotPairs.addFirst(pair);
    }

    private void dropPair(HotPair pair) {
        dropObject(pair.watcher);
        dropObject(pair.marker);
    }

    private void grabObject(AoiObject object) {
        object.references++;
    }

    private void dropObject(AoiObject object) {
        object.references--;
        if (object.references <= 0) {
            objects.remove(object.id, object);
        }
    }

    private static boolean isNear(AoiPosition first, AoiPosition second) {
        return first.distanceSquared(second) < AOI_RADIUS_SQUARED * 0.25f;
    }

    private static float distanceSquared(AoiObject first, AoiObject second) {
        return first.position.distanceSquared(second.position);
    }

    private static AoiObjectSnapshot snapshot(AoiObject object) {
        return new AoiObjectSnapshot(object.id,
                (object.mode & MODE_WATCHER) != 0,
                (object.mode & MODE_MARKER) != 0,
                (object.mode & MODE_MOVE) != 0,
                (object.mode & MODE_DROP) != 0,
                object.version,
                object.lastKeyPosition,
                object.position);
    }

    private static void validateId(long id) {
        if (id < 0 || id > MAX_ID) {
            throw new IllegalArgumentException("AOI id 必须在 uint32 范围内: " + id);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("AOI space 已释放");
        }
    }

    private static final class AoiObject {
        private int references = 1;
        private final long id;
        private int version;
        private int mode;
        private AoiPosition lastKeyPosition = AoiPosition.of(0, 0);
        private AoiPosition position = AoiPosition.of(0, 0);

        private AoiObject(long id) {
            this.id = id;
        }
    }

    private record HotPair(AoiObject watcher, AoiObject marker,
                           int watcherVersion, int markerVersion) {
    }
}
