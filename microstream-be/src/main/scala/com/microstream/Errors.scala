package com.microstream

trait AppError extends Throwable
object AppError {
  case class ServiceError(msg: String) extends AppError
}
