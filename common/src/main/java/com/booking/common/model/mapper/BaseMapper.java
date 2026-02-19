package com.booking.common.model.mapper;

import java.util.Collection;
import java.util.List;

/**
 * Base mapper contract for MapStruct mappers.
 *
 * @param <S> Source type
 * @param <T> Target type
 */
public interface BaseMapper<S, T> {

    /**
     * Maps a single source object to a target object.
     */
    T map(S source);

    /**
     * Maps a collection of source objects to a list of target objects.
     */
    List<T> map(Collection<S> sources);
}

