package info.fotm.clustering

import info.fotm.util.MathVector

trait Clusterer {
  type Cluster = Seq[MathVector]
  def clusterize(input: Cluster, groupSize: Int): Set[Cluster]
}
