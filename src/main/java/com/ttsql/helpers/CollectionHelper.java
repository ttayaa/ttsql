package com.ttsql.helpers;

import java.util.Collection;

public final class CollectionHelper {
    private CollectionHelper() {
    }

    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    public static boolean isEmpty(Collection<?> collections) {
        return collections == null || collections.isEmpty();
    }

    public static Object[] toArray(Object obj, int objType) {
        Object[] values;
        switch(objType) {
            case 1:
                values = (Object[])((Object[])obj);
                break;
            case 2:
                values = ((Collection)obj).toArray();
                break;
            default:
                values = new Object[]{obj};
        }

        return values;
    }
}
