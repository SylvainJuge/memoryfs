package com.github.sylvainjuge.memoryfs;

public class ParamAssert {

    /**
     * Checks for parameter non-nullity
     * @param param parameter to check for non-nullity
     * @param name parameter name/description
     * @param <T> param type
     * @throws java.lang.IllegalArgumentException if {@code param} is null.
     * @return param
     */
    public static <T> T checkNotNull(T param, String name){
        if (null == param) {
            throw new IllegalArgumentException(name + " is required, got null");
        }
        return param;
    }
}
