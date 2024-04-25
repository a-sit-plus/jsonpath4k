package at.asitplus.jsonpath

import at.asitplus.wallet.lib.data.jsonpath.JsonPathSelector


sealed interface JsonPathExpression {

    sealed class FilterExpression(
        val expressionType: JsonPathFilterExpressionType,
        open val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue,
    ) : JsonPathExpression {
        data class ValueExpression(
            override val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue.ValueTypeValue
        ) : FilterExpression(
            expressionType = JsonPathFilterExpressionType.ValueType,
            evaluate = evaluate
        )

        data class LogicalExpression(
            override val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue.LogicalTypeValue
        ) : FilterExpression(
            expressionType = JsonPathFilterExpressionType.LogicalType,
            evaluate = evaluate
        )

        sealed class NodesExpression(
            override val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue.NodesTypeValue
        ) : FilterExpression(
            expressionType = JsonPathFilterExpressionType.NodesType,
            evaluate = evaluate
        ) {
            sealed class FilterQueryExpression(
                open val jsonPathQuery: JsonPathQuery,
                override val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue.NodesTypeValue.FilterQueryResult
            ) : NodesExpression(evaluate) {
                data class SingularQueryExpression(
                    override val jsonPathQuery: JsonPathQuery,
                    override val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue.NodesTypeValue.FilterQueryResult.SingularQueryResult = {
                        val nodeList = jsonPathQuery.invoke(
                            currentNode = it.currentNode,
                            rootNode = it.rootNode,
                        ).map {
                            it.value
                        }
                        JsonPathFilterExpressionValue.NodesTypeValue.FilterQueryResult.SingularQueryResult(
                            nodeList
                        )
                    }
                ) : FilterQueryExpression(
                    jsonPathQuery = jsonPathQuery,
                    evaluate = evaluate,
                ) {
                    fun toValueTypeValue(): ValueExpression {
                        return ValueExpression { context ->
                            this.evaluate(context).nodeList.firstOrNull()?.let {
                                JsonPathFilterExpressionValue.ValueTypeValue.JsonValue(it)
                            } ?: JsonPathFilterExpressionValue.ValueTypeValue.Nothing
                        }
                    }
                }

                data class NonSingularQueryExpression(
                    override val jsonPathQuery: JsonPathQuery,
                    override val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue.NodesTypeValue.FilterQueryResult.NonSingularQueryResult = {
                        val nodeList = jsonPathQuery.invoke(
                            currentNode = it.currentNode,
                            rootNode = it.rootNode,
                        ).map {
                            it.value
                        }
                        JsonPathFilterExpressionValue.NodesTypeValue.FilterQueryResult.NonSingularQueryResult(
                            nodeList
                        )
                    }
                ) : FilterQueryExpression(
                    jsonPathQuery = jsonPathQuery,
                    evaluate = evaluate
                )
            }

            data class NodesFunctionExpression(
                override val evaluate: (JsonPathEvaluationContext) -> JsonPathFilterExpressionValue.NodesTypeValue.FunctionExtensionResult
            ) : NodesExpression(evaluate)
        }
    }

    data class SelectorExpression(val selector: JsonPathSelector) : JsonPathExpression

    data object NoType : JsonPathExpression
    data object ErrorType : JsonPathExpression
}