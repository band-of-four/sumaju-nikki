package utils

import java.util.concurrent.ThreadLocalRandom

object RandomEvent {
  def apply(odd: Double) = ThreadLocalRandom.current().nextDouble() < odd
}

object RandomDouble {
  def apply(bound: Double) = ThreadLocalRandom.current().nextDouble(bound)
}

object WeightedSample {
  def apply[A](elements: Seq[A])(weightMap: A => Double): Option[A] = {
    val weights = elements.map(weightMap)

    weights.sum match {
      case 0.0 =>
        None
      case weightSum =>
        val r = ThreadLocalRandom.current().nextDouble(0, weightSum)

        var currentWeight = 0.0
        for ((item, weight) <- elements.zip(weights)) {
          currentWeight += weight
          if (currentWeight >= r) return Some(item)
        }

        None
    }
  }
}
