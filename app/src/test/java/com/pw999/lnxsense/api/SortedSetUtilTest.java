package com.pw999.lnxsense.api;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SortedSetUtilTest {

    @Test
    public void ofSingleElement() {
        var set = SortedSetUtil.of(new Sensor("a", "A", 1));
        assertEquals(1, set.size());
        assertEquals("a", set.first().id());
    }

    @Test
    public void ofTwoElements() {
        var set = SortedSetUtil.of(
                new Sensor("b", "B", 2),
                new Sensor("a", "A", 1)
        );
        assertEquals(2, set.size());
        // Should be sorted by index (Comparable)
        assertEquals("a", set.first().id());
        assertEquals("b", set.last().id());
    }

    @Test
    public void ofThreeElements() {
        var set = SortedSetUtil.of(
                new Sensor("c", "C", 3),
                new Sensor("a", "A", 1),
                new Sensor("b", "B", 2)
        );
        assertEquals(3, set.size());
        assertEquals("a", set.first().id());
    }

    @Test
    public void ofFourElements() {
        var set = SortedSetUtil.of(
                new Sensor("d", "D", 4),
                new Sensor("b", "B", 2),
                new Sensor("c", "C", 3),
                new Sensor("a", "A", 1)
        );
        assertEquals(4, set.size());
        assertEquals("a", set.first().id());
        assertEquals("d", set.last().id());
    }

    @Test
    public void ofCollection() {
        var list = List.of(
                new Sensor("c", "C", 3),
                new Sensor("a", "A", 1)
        );
        var set = SortedSetUtil.of(list);
        assertEquals(2, set.size());
        assertEquals("a", set.first().id());
    }

    @Test
    public void ofVarargs() {
        var set = SortedSetUtil.of(
                new Sensor("e", "E", 5),
                new Sensor("c", "C", 3),
                new Sensor("a", "A", 1),
                new Sensor("d", "D", 4),
                new Sensor("b", "B", 2)
        );
        assertEquals(5, set.size());
        assertEquals("a", set.first().id());
        assertEquals("e", set.last().id());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void resultIsUnmodifiable() {
        var set = SortedSetUtil.of(new Sensor("a", "A", 1));
        set.add(new Sensor("b", "B", 2));
    }
}
