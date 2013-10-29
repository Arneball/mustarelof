package controllers
sealed trait ExceptionCause

case object NoUser extends ExceptionCause
case object NoJson  extends ExceptionCause
case object Other extends ExceptionCause
case class RestException(message: String, cause: ExceptionCause) extends Exception
