package info.fotm.clustering.RMClustering

import info.fotm.clustering.Clusterer
import info.fotm.clustering.Clusterer._
import ClusterRoutines._

class EqClusterer extends Clusterer
{
  //type MathCluster = Seq[MathVector]

  //def toMathCluster(cluster: Cluster): MathCluster = cluster.map(c => MathVector(c))

  //def toCluster(mathCluster: MathCluster): Cluster = mathCluster.map(c => c.coords)



  def clusterize(input: Cluster, groupSize: Int): Set[Cluster] =
  {
    //println(input.length, input)
    // moves one point from one cluster to another
    def movePoint(clusters: List[Cluster]): List[Cluster] =
    {
      val graph = makeGraphFromClusters(clusters, groupSize)
      val positiveVertices = getPositiveVertices(graph)
      if (positiveVertices.isEmpty)
        clusters
      else
      {
        val dijkstraAlg = new DijkstraAlg(graph)
        val paths = dijkstraAlg.findShortestPathsFrom(positiveVertices)
        val optPath = findOptimalPath(graph, paths)
        val newClusters = movePointAlongPath(clusters, optPath)
        movePoint(newClusters)
      }
    }

    val kMeansRunCount = 5

    val kmeansClusterer = new KMeansClusterer
    val approxCountOfGroups = input.length / groupSize
    val clusterizations = (1 to kMeansRunCount).map(i => kmeansClusterer.clusterize(input, approxCountOfGroups))
    //val optClusterization = clusterizations.minBy(estimateClusterization)
    val optClusterization = clusterizations.filter(c => c.size == approxCountOfGroups).minBy(estimateClusterization)
    movePoint(optClusterization.toList).toSet

    //val n = uneqClusters.map(x => difference(groupSize, x.length)).sum //negative clusters deviation sum
    //val p = uneqClusters.map(x => difference(x.length, groupSize)).sum //positive clusters deviation sum
    //val k = math.min(n, p) //they're equal for now
    //val sortedpaths = DijkstraAlg.findAllFromPositiveShortestPaths(uneqClusters).sortWith(pathesComparer)
    //val eqClusters = sortedpaths.scanLeft(uneqClusters)((acc, xpath) => passMaxByPath(acc, xpath, groupSize)).last
    //eqClusters.toSet
  }



}
