package com.github.celadari.jsonlogicscala.core

import play.api.libs.json._


object ComposeLogic {

  object BINARY_OPERATORS {
    val LTEQ = "<="
    val LT = "<"
    val GTEQ = ">="
    val GT = ">"
    val EQ = "=="
    val DIFF = "!="
    val IN = "in"
    val NOT_IN = "not in"
    val ALL = Array(LTEQ, LT, GTEQ, GT, EQ, DIFF)
  }

  object MULTIPLE_OPERATORS {
    val OR = "or"
    val AND = "and"
    val XOR = "xor"
    val MAX = "max"
    val MIN = "min"
    val ALL = Array(OR, AND, XOR, MAX, MIN)
  }
  val OPERATORS: Array[String] = BINARY_OPERATORS.ALL ++ MULTIPLE_OPERATORS.ALL

  private[core] def decode(jsonLogic: JsObject, jsonLogicData: JsObject, isDataArray: Boolean = false)
                          (implicit decoder: Decoder): ComposeLogic = {
    // check for operator field
    val fields = jsonLogic.fields

    // check for compose logic operator field
    if (fields.length > 1) throw new Error("JSON object is supposed to have only one operator field.")
    val operator = fields.head._1

    // if operator is compose logic
    ComposeLogic(operator, decodeArrayOfConditions((jsonLogic \ operator).get, jsonLogicData, isDataArray)(decoder))
  }

  private[core] def encode(composeLogic: ComposeLogic)
                          (implicit encoder: Encoder): (JsValue, JsObject) = {
    // retrieve compose logic attributes
    val operator = composeLogic.operator
    val conditions = composeLogic.conditions

    // create js map operator -> conditions
    val (jsonLogic, jsonLogicData) = encodeArrayOfConditions(conditions)(encoder)
    (JsObject(Map(operator -> jsonLogic)), jsonLogicData)
  }

  private[core] def encodeArrayOfConditions(conditions: Array[JsonLogicCore])(implicit encoder: Encoder): (JsValue, JsObject) = {
    val (jsonLogics, jsonLogicData) = conditions.map(jsonLogic => JsonLogicCore.encode(jsonLogic)(encoder)).unzip
    (JsArray(jsonLogics), jsonLogicData.reduce(_ ++ _))
  }

  private[core] def decodeArrayOfConditions(json: JsValue, jsonLogicData: JsObject, isDataArray: Boolean = false)
                                           (implicit decoder: Decoder): Array[JsonLogicCore] = {
    val jsArray = json.asInstanceOf[JsArray]
    jsArray
      .value
      .map(jsValue => {
        JsonLogicCore.decode(jsValue.asInstanceOf[JsObject], jsonLogicData, isDataArray)(decoder)
      })
      .toArray
  }

}

case class ComposeLogic(override val operator: String, conditions: Array[JsonLogicCore])
  extends JsonLogicCore(operator) {

  def isEmpty: Boolean = conditions.forall(_.isEmpty)
}
