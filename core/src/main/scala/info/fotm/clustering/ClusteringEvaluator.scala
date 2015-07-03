package info.fotm.clustering

import info.fotm.clustering.RMClustering.EqClusterer
import info.fotm.clustering.enhancers.{ClonedClusterer, Verifier, Summator, Multiplexer}
import info.fotm.domain.Domain._
import info.fotm.domain._
import info.fotm.util.Statistics.Metrics
import info.fotm.util.{Statistics, MathVector}

import scala.util.Random

object ClusteringEvaluator extends App {
  def sqr(x: Double) = x * x

  def featurize(ci: CharFeatures): MathVector = MathVector(
    ci.nextInfo.rating - ci.prevInfo.rating,
    sqr(ci.nextInfo.rating - ci.prevInfo.rating) / ci.prevInfo.rating.toDouble,
    ci.nextInfo.rating,
    ci.nextInfo.seasonWins,
    ci.nextInfo.seasonLosses,
    ci.nextInfo.weeklyWins,
    ci.nextInfo.weeklyLosses
  )

  def findTeams(clusterer: RealClusterer, diffs: Seq[CharFeatures]): Set[Team] = {
    if (diffs.isEmpty)
      Set()
    else {
      val features: Seq[MathVector] = Statistics.normalize(diffs.map(featurize))
      val featureMap = diffs.map(_.prevInfo.id).zip(features).toMap
      val clusters = clusterer.clusterize(featureMap, ClusteringEvaluatorData.teamSize)
      clusters.map(ps => Team(ps.toSet))
    }
  }

  def evaluateStep(clusterer: RealClusterer,
                   ladder: LadderSnapshot,
                   nextLadder: LadderSnapshot,
                   games: Set[Game]): Statistics.Metrics = {
    print(".")
    val teamsPlayed: Set[Team] = games.map(g => Seq(g._1, g._2)).flatten

    val (wTeams, leTeams) = teamsPlayed.partition(t => t.rating(nextLadder) - t.rating(ladder) > 0)
    val (eTeams, lTeams) = leTeams.partition(t => t.rating(nextLadder) - t.rating(ladder) == 0)

    // algo input: ladder diffs for playersPlayed
    val wDiffs = wTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val lDiffs = lTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }
    val eDiffs = eTeams.flatMap(_.members).toList.map { p => CharFeatures(ladder(p), nextLadder(p)) }

    // algo evaluation: match output against teamsPlayed
    val teams = findTeams(clusterer, wDiffs) ++ findTeams(clusterer, lDiffs) ++ findTeams(clusterer, eDiffs)
    Statistics.calcMetrics(teams, teamsPlayed)
  }

  def evaluate(clusterer: RealClusterer, data: Seq[(LadderSnapshot, LadderSnapshot, Set[Game])]): Double = {
    val stats: Seq[Metrics] =
      for { (ladder, nextLadder, games) <- data }
      yield evaluateStep(clusterer, ladder, nextLadder, games)

    val combinedMetrics: Metrics = stats.reduce(_ + _)
    println(s"\n$combinedMetrics")

    Statistics.fScore(0.5)(combinedMetrics)
  }

  {
    // Runner
    import ClusteringEvaluatorData._

    val data = prepareData().drop(500).take(200).toList
    val (prevLadder, lastladder, _) = data.last
    lastladder.values.toList.sortBy(-_.rating).map(i => (i.rating, i.seasonWins, i.seasonLosses, i.weeklyWins, i.weeklyLosses)).foreach(println)

    val clusterers: Map[String, RealClusterer] = Map(
      "Random" -> RealClusterer.wrap(new RandomClusterer),
      "Closest + Multiplexer" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer,
      "Closest" -> RealClusterer.wrap(new ClosestClusterer),
      "Closest + Verifier" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Verifier,
      "Closest + Multiplexer + Verifier" -> new ClonedClusterer(RealClusterer.wrap(new ClosestClusterer)) with Multiplexer with Verifier,
      "HTClusterer" -> RealClusterer.wrap(new HTClusterer),
      "HTClusterer + Verifier" -> RealClusterer.wrap(new HTClusterer),
      "RMClusterer" -> RealClusterer.wrap(new EqClusterer),
      "RMClusterer + Verifier" -> new ClonedClusterer(RealClusterer.wrap(new EqClusterer)) with Verifier
//      "HT + RM + Verifier" -> new Summator(RealClusterer.wrap(new EqClusterer), RealClusterer.wrap(new HTClusterer)) with Verifier
//      "HT + RM + Closest" -> new Summator(new EqClusterer, new HTClusterer, new ClosestClusterer),
//      "HT + RM + (Closest with Multiplexer)" -> new Summator(new EqClusterer, new HTClusterer, new ClosestClusterer with Multiplexer)
    )

    for ((name, clusterer) <- clusterers) {
      val result = evaluate(clusterer, data)
      println(s"$name = $result")
    }
  }
}
