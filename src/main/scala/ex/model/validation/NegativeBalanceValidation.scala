package ex.model.validation

import cats.data.{NonEmptyList, Validated, ValidatedFunctions}
import ex.model._
import ex.model.state.Storage
import cats.implicits._
import cats._
import cats.free.Free
object NegativeBalanceValidation extends ValidatedFunctions with Storage {

  def apply(startTime: Timestamp)(t: FromToTransaction):
  Free[DSL, Validated[NonEmptyList[BlockId], FromToTransaction]] =
    for {
      time <- lastConfirmedBlockTimestamp()
      r <- if (time < startTime) {
        pure(valid(t))
      } else {
        for {
          senderBalance <- accBalance(t.sender)
        } yield {
          if (stateStaysPositive(senderBalance, t.quantity, t.fee))
            valid(t)
          else invalidNel("Insufficient Sender Funds")
        }
      }
    } yield r

  private def stateStaysPositive(senderBalance: Portfolio, quantity: Volume, fee: Volume): Boolean = {
    val totalTransfer = liftVolume(quantity) combine liftVolume(fee)
    totalTransfer.forall {
      case (m: Money, amt: Long) =>
        senderBalance.get(m) match {
          case Some(v) if v >= amt => true
          case _                   => false
        }
    }
  }
}