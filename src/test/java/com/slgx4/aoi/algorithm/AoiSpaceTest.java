package com.slgx4.aoi.algorithm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AoiSpaceTest {
    @Test
    void movingMarkerEnteringWatcherRadiusEmitsOneDirectionalMessage() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            space.update(2, "m", AoiPosition.of(9, 0));

            assertEquals(List.of(new AoiEvent(1, 2)), space.message());
            assertEquals(List.of(), space.message());
        }
    }

    @Test
    void bothModeProducesBothWatcherMarkerDirections() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "wm", AoiPosition.of(0, 0));
            space.update(2, "wm", AoiPosition.of(1, 0));

            List<AoiEvent> events = space.message();

            assertEquals(2, events.size());
            assertTrue(events.contains(new AoiEvent(1, 2)));
            assertTrue(events.contains(new AoiEvent(2, 1)));
        }
    }

    @Test
    void hotPairDetectsSmallShiftIntoRadiusWithoutNewKeyPosition() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            space.update(2, "m", AoiPosition.of(12, 0));
            assertEquals(List.of(), space.message());
            assertEquals(1, space.stats().hotPairCount());

            space.update(2, "m", AoiPosition.of(9, 0));

            assertEquals(List.of(new AoiEvent(1, 2)), space.message());
            assertEquals(0, space.stats().hotPairCount());
        }
    }

    @Test
    void hotPairIsDiscardedPastTwiceTheAoiRadius() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            space.update(2, "m", AoiPosition.of(15, 0));
            space.message();
            assertEquals(1, space.stats().hotPairCount());

            space.update(2, "m", AoiPosition.of(21, 0));
            space.message();

            assertEquals(0, space.stats().hotPairCount());
        }
    }

    @Test
    void leavingProducesNoMessageAndReenteringProducesAnotherMessage() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            space.update(2, "m", AoiPosition.of(5, 0));
            assertEquals(1, space.message().size());

            space.update(2, "m", AoiPosition.of(30, 0));
            assertEquals(List.of(), space.message());

            space.update(2, "m", AoiPosition.of(5, 0));
            assertEquals(List.of(new AoiEvent(1, 2)), space.message());
        }
    }

    @Test
    void exactlyHalfRadiusCreatesNewMoveVersion() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            int initialVersion = space.object(1).orElseThrow().version();

            space.update(1, "w", AoiPosition.of(4.99f, 0));
            assertEquals(initialVersion, space.object(1).orElseThrow().version());

            space.update(1, "w", AoiPosition.of(5, 0));
            assertEquals(initialVersion + 1, space.object(1).orElseThrow().version());
        }
    }

    @Test
    void distanceThresholdsRemainStrictLikeTheCImplementation() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            space.update(2, "m", AoiPosition.of(10, 0));
            assertEquals(List.of(), space.message());
            assertEquals(1, space.stats().hotPairCount());

            space.update(2, "m", AoiPosition.of(9.99f, 0));
            assertEquals(List.of(new AoiEvent(1, 2)), space.message());

            space.update(3, "m", AoiPosition.of(20.01f, 0));
            assertEquals(List.of(), space.message());
            assertEquals(0, space.hotPairs().stream()
                    .filter(pair -> pair.markerId() == 3)
                    .count());
        }
    }

    @Test
    void dropInvalidatesHotPairsAndRemovesObject() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            space.update(2, "m", AoiPosition.of(15, 0));
            space.message();
            assertEquals(1, space.stats().hotPairCount());

            space.update(2, "d", AoiPosition.of(15, 0));
            assertTrue(space.object(2).isPresent());
            assertEquals(List.of(), space.message());

            assertTrue(space.object(2).isEmpty());
            assertEquals(0, space.stats().hotPairCount());
        }
    }

    @Test
    void droppedObjectCanBeResurrectedBeforeHotPairFlush() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", AoiPosition.of(0, 0));
            space.update(2, "m", AoiPosition.of(15, 0));
            space.message();

            space.update(2, "d", AoiPosition.of(15, 0));
            space.update(2, "m", AoiPosition.of(5, 0));

            assertEquals(List.of(new AoiEvent(1, 2)), space.message());
            assertTrue(space.object(2).isPresent());
        }
    }

    @Test
    void callbackApiAndFloatArrayOverloadMatchTheCShape() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(1, "w", new float[]{0, 0, 0});
            space.update(2, "m", new float[]{0, 0, 5});
            List<AoiEvent> events = new ArrayList<>();

            space.message((watcher, marker) -> events.add(new AoiEvent(watcher, marker)));

            assertEquals(List.of(new AoiEvent(1, 2)), events);
        }
    }

    @Test
    void validatesUnsignedIdAndReleaseLifecycle() {
        AoiSpace space = AoiSpace.create();
        space.update(AoiSpace.MAX_ID, "w", AoiPosition.of(0, 0));
        assertTrue(space.object(AoiSpace.MAX_ID).isPresent());
        assertThrows(IllegalArgumentException.class,
                () -> space.update(-1, "w", AoiPosition.of(0, 0)));

        space.release();

        assertThrows(IllegalStateException.class, space::message);
        space.close();
    }

    @Test
    void modeChangesAreAppliedExactlyByEachUpdateString() {
        try (AoiSpace space = AoiSpace.create()) {
            space.update(7, "wm", AoiPosition.of(1, 2));
            AoiObjectSnapshot both = space.object(7).orElseThrow();
            assertTrue(both.watcher());
            assertTrue(both.marker());

            space.update(7, "w", AoiPosition.of(1, 2));
            AoiObjectSnapshot watcherOnly = space.object(7).orElseThrow();
            assertTrue(watcherOnly.watcher());
            assertFalse(watcherOnly.marker());
        }
    }

    @Test
    void referenceTestCScenarioProducesTheSameFiveMessagesInOrder() {
        try (AoiSpace space = AoiSpace.create()) {
            MovingObject[] objects = {
                    new MovingObject(0, "w", 40, 0, 0, 1),
                    new MovingObject(1, "wm", 42, 100, 0, -1),
                    new MovingObject(2, "w", 0, 40, 1, 0),
                    new MovingObject(3, "wm", 100, 45, -1, 0)
            };
            List<AoiEvent> actual = new ArrayList<>();

            for (int tick = 0; tick < 100; tick++) {
                if (tick < 50) {
                    for (MovingObject object : objects) {
                        object.advanceAndUpdate(space);
                    }
                } else if (tick == 50) {
                    space.update(3, "d", objects[3].position());
                } else {
                    for (int index = 0; index < 3; index++) {
                        objects[index].advanceAndUpdate(space);
                    }
                }
                actual.addAll(space.message());
            }

            assertEquals(List.of(
                    new AoiEvent(0, 1),
                    new AoiEvent(2, 3),
                    new AoiEvent(3, 1),
                    new AoiEvent(1, 3),
                    new AoiEvent(0, 1)
            ), actual);
        }
    }

    private static final class MovingObject {
        private final long id;
        private final String mode;
        private final float velocityX;
        private final float velocityY;
        private AoiPosition position;

        private MovingObject(long id, String mode, float x, float y,
                             float velocityX, float velocityY) {
            this.id = id;
            this.mode = mode;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.position = AoiPosition.of(x, y);
        }

        private void advanceAndUpdate(AoiSpace space) {
            position = AoiPosition.of(wrap(position.x() + velocityX),
                    wrap(position.y() + velocityY));
            space.update(id, mode, position);
        }

        private AoiPosition position() {
            return position;
        }

        private static float wrap(float value) {
            if (value < 0) {
                return value + 100.0f;
            }
            if (value > 100.0f) {
                return value - 100.0f;
            }
            return value;
        }
    }
}
