fun box(stepId: Int): String {
    val d = Demo(15)
    when (stepId) {
        0 -> {
            if (d != Demo(15)) return "Fail equals"
            if (d.toString() != "Demo(x=15)") return "Fail toString"
            if (d.hashCode() != Demo(15).hashCode()) return "Fail hashCode"
        }
        1 -> {
            if (d == Demo(15)) return "Fail equals"
            if (d.toString() != "[object Object]") return "Fail toString"
            if (d.hashCode() == Demo(15).hashCode()) return "Fail hashCode"
        }
        else -> return "Unknown"
    }

    return "OK"
}
