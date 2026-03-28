package com.pw999.lnxsense.api;

import java.util.*;
//TODO: this should not be in the API module

// Utility class to quickly make a SortedSet. A bit like Set.of().
public final class SortedSetUtil {
    private SortedSetUtil() {}

    public static <A extends Comparable<A>> SortedSet<A> of(A a) {
        TreeSet<A> ts = new TreeSet<>();
        ts.add(a);
        return Collections.unmodifiableSortedSet(ts);
    }

    public static <A extends Comparable<A>> SortedSet<A> of(A a, A b) {
        TreeSet<A> ts = new TreeSet<>(List.of(a, b));
        return Collections.unmodifiableSortedSet(ts);
    }

    public static <A extends Comparable<A>> SortedSet<A> of(A a, A b, A c) {
        TreeSet<A> ts = new TreeSet<>(List.of(a, b, c));
        return Collections.unmodifiableSortedSet(ts);
    }

    public static <A extends Comparable<A>> SortedSet<A> of(A a, A b, A c, A d) {
        TreeSet<A> ts = new TreeSet<>(List.of(a, b, c, d));
        return Collections.unmodifiableSortedSet(ts);
    }

    public static <A extends Comparable<A>> SortedSet<A> of(Collection<A> a) {
        TreeSet<A> ts = new TreeSet<>(a);
        return Collections.unmodifiableSortedSet(ts);
    }

    public static <A extends Comparable<A>> SortedSet<A> of(A ... a) {
        TreeSet<A> ts = new TreeSet<>(Arrays.asList(a));
        return Collections.unmodifiableSortedSet(ts);
    }

}
