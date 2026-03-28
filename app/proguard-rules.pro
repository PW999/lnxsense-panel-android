# Keep API records for Java serialization compatibility.
# The server sends Group[], Device, Sensor and Reading objects via ObjectOutputStream.
# R8 must not rename or remove fields, as the serialVersionUID is computed from the
# record component names and types — any change breaks deserialization.
-keep class com.pw999.lnxsense.api.** { *; }
