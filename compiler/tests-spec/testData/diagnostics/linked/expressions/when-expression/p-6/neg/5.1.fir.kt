// SKIP_TXT


// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean, value_2: Boolean, value_3: Long) {
    <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
        value_2 -> {}
        !value_2 -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>getBoolean() && value_2<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>getChar() != 'a'<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>getList() === getAny()<!> -> {}
        <!CONFUSING_BRANCH_CONDITION_WARNING!>value_3 <= 11<!> -> {}
    }
}
