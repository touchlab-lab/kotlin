class A {
    fun component1() : Int = 1
    fun component2() : Int = 2
}

fun a() {
    val (<!REDECLARATION, UNUSED_VARIABLE!>a<!>, <!REDECLARATION, UNUSED_VARIABLE!>a<!>) = A()
    val (<!UNUSED_VARIABLE!>x<!>, <!REDECLARATION, UNUSED_VARIABLE!>y<!>) = A();
    val <!REDECLARATION!>b<!> = 1
    use(b)
    val (<!REDECLARATION, UNUSED_VARIABLE!>b<!>, <!REDECLARATION, UNUSED_VARIABLE!>y<!>) = A();
}


fun use(a: Any): Any = a