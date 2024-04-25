package at.asitplus.jsonpath

/*
specification: https://datatracker.ietf.org/doc/rfc9535/
section: 2.4.1.  Type System for Function Expressions
 */
enum class JsonPathFilterExpressionType {
    ValueType,
    LogicalType,
    NodesType;
}