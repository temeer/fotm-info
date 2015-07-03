import info.fotm.clustering._
import ClusteringEvaluatorData._
import ClusteringEvaluator._
import info.fotm.domain.Domain.LadderSnapshot
import info.fotm.domain.Team
import info.fotm.util.MathVector
import org.scalatest._

import scala.collection.immutable.{TreeMap, IndexedSeq}

class ClusteringEvaluatorSpec extends FlatSpec with Matchers with ClusteringEvaluatorSpecBase {
  "evaluateStep" should "return correct metrics when everything is guessed right" in {
    val games = Set((team1580, team1500))

    val nextLadder = play(ladder, team1580, team1500)

    val clusterer = new RealClusterer {
      override def clusterize[T](input: Map[T, MathVector], groupSize: Int): Set[Seq[T]] =
        Set(team1580.members.map(m => m.asInstanceOf[T]).toSeq, team1500.members.map(m => m.asInstanceOf[T]).toSeq)
    }

    val metrics = evaluateStep(clusterer, ladder, nextLadder, games)

    metrics.truePositive should be(2)
    metrics.falsePositive should be(0)
    metrics.falseNegative should be(0)
  }
}
