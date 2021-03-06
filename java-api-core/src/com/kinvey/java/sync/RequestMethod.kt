package com.kinvey.java.sync

/**
 * Created by yuliya on 09/28/17.
 */

enum class RequestMethod (private val query: String) {
    SAVE("SAVE"),
    DELETE("DELETE");
    companion object {
        @JvmStatic
        fun fromString(verb: String): RequestMethod? {
            for (v in RequestMethod.values()) {
                if (v.query.equals(verb, ignoreCase = true)) {
                    return v
                }
            }
            return null
        }
    }
}
