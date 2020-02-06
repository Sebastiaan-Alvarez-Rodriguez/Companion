package com.python.companion.util;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListUtil {
    public static <T> List<T> getRemoved(@Nullable Collection<T> old, @Nullable Collection<T> cur) {
        if (old == null)
            return new ArrayList<>();
        if (cur == null)
            return new ArrayList<>(old);
        List<T> retList = new ArrayList<>();
        for (T t : old)
            if (!cur.contains(t))
                retList.add(t);
        return retList;
    }

    public static <T> List<T> getAdded(@Nullable Collection<T> old, @Nullable Collection<T> cur) {
        if (cur == null)
            return new ArrayList<>();
        if (old == null) {
            return new ArrayList<>(cur);
        }
        List<T> retList = new ArrayList<>();
        for (T t : cur)
            if (!old.contains(t))
                retList.add(t);
        return retList;
    }
}
