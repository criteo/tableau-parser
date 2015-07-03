package com.criteo.tableau.validation

/**
 * The return value of applying a validation rule.
 */
abstract sealed class ValidationStatus(val isSuccess: Boolean)

case object Success extends ValidationStatus(true)

case class Failure(msg: String) extends ValidationStatus(false)
