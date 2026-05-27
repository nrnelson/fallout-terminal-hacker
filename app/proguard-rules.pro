# ML Kit on-device text recognition
# ----------------------------------
# ML Kit looks up its model providers and native bridge classes by name via
# Service Locator / reflection. Without these keeps the recognizer fails to
# initialize at runtime on a minified release build (works fine in debug
# because debug isn't minified).
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.text.internal.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }

# ML Kit common — model manifest readers, language detectors, image converters
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }

# CameraX — preserves the public surface so reflection-based hooks
# (extensions, controllers) keep working
-keep class androidx.camera.** { *; }

# kotlinx.coroutines — service loader entry for ServiceLoader-based dispatcher
# discovery. The default consumer-rules cover most of this, but the
# DebugProbesKt entry is sometimes stripped.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose ships its own consumer-proguard inside the artifact; no extra rules
# needed here for runtime correctness.
