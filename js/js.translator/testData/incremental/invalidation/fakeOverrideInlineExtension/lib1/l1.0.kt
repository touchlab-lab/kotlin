abstract class AbstractClassA {
    inline fun String.fakeOverrideExtension() = "${this} fakeOverrideExtension 0"

    inline val String.fakeOverrideGetProperty
        get() = "${this} fakeOverrideGetProperty 0"

    inline var String.fakeOverrideSetProperty: String
        get() = "${savedString} fakeOverrideSetProperty getter 0"
        set(str) {
            savedString = "${str} fakeOverrideSetProperty setter 0"
        }

    var savedString = ""
}
