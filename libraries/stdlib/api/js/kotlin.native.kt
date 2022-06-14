@kotlin.SinceKotlin(version = "1.7")
@kotlin.RequiresOptIn(level = Level.WARNING, message = "Freezing API is deprecated since 1.7.20. See https://github.com/JetBrains/kotlin/blob/master/kotlin-native/NEW_MM.md#freezing-deprecation for details")
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
public final annotation class FreezingIsDeprecated : kotlin.Annotation {
    public constructor FreezingIsDeprecated()
}
