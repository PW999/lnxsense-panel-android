/**
 * Verbatim copies of the lnxsense API records from {@code api/src/main/java/com/pw999/lnxsense/api/}.
 *
 * <p>These files are duplicated rather than shared because:
 * <ul>
 *   <li>The original API module is compiled with Java 25, uses JPMS ({@code module-info.java}),
 *       and has a compile-time dependency on Spring Modulith — none of which are compatible
 *       with the Android build toolchain.</li>
 *   <li>Java 23+ markdown doc comments ({@code ///}) are not valid at Java 17, so they have
 *       been converted to regular {@code //} comments.</li>
 * </ul>
 *
 * <p><b>Important:</b> The package name, record component names, types, and field order must
 * remain identical to the server's version. Java records have a {@code serialVersionUID} of
 * {@code 0L} computed from the record descriptor — any mismatch will cause
 * {@link java.io.ObjectInputStream} deserialization to fail when receiving data from the server.
 *
 * <p>When updating the server's API records, these copies must be updated in sync.
 */
package com.pw999.lnxsense.api;
