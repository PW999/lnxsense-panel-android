package com.pw999.lnxsense.android.data

import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass

/**
 * An ObjectInputStream that ignores serialVersionUID mismatches for classes in the
 * com.pw999.lnxsense.api package.
 *
 * The server is compiled with Java 25 and this app with Java 17. Even though the
 * record definitions (package, field names, field types, field order) are identical,
 * the auto-computed serialVersionUID differs because Java 25 bytecode includes
 * different synthetic methods for records than Java 17. Rather than hard-coding
 * matching UIDs on both sides, we override the stream descriptor to use the local
 * class's UID when we know the class is one of our own API types.
 */
class LenientObjectInputStream(input: InputStream) : ObjectInputStream(input) {

    companion object {
        private const val API_PACKAGE = "com.pw999.lnxsense.api."
    }

    override fun readClassDescriptor(): ObjectStreamClass {
        val descriptor = super.readClassDescriptor()
        val className = descriptor.name

        // Only patch serialVersionUID for our own API types
        if (className.startsWith(API_PACKAGE) || className.startsWith("[L$API_PACKAGE")) {
            try {
                val localClass = Class.forName(className)
                val localDescriptor = ObjectStreamClass.lookup(localClass)
                if (localDescriptor != null && localDescriptor.serialVersionUID != descriptor.serialVersionUID) {
                    return localDescriptor
                }
            } catch (_: ClassNotFoundException) {
                // Fall through to return the original descriptor
            }
        }

        return descriptor
    }
}
