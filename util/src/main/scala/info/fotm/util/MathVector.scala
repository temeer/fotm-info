package info.fotm.util

case class MathVector(coords: Double*) {
  private def operation(that: MathVector, coordsOp: (Double, Double) => Double) = {
    require(this.dimension == that.dimension)
    MathVector(coords.zip(that.coords).map(c => coordsOp(c._1, c._2)): _*)
  }

  lazy val dimension = coords.length

  lazy val length: Double = Math.sqrt(coords.map(Math.pow(_, 2)).sum)

  lazy val unary_- = MathVector(coords.map(-1 * _): _*)

  override lazy val toString = coords.mkString("(", ",", ")")

  def unary_+ = this

  def +(that: MathVector) = operation(that, _ + _)

  def -(that: MathVector) = operation(that, _ - _)

  def scalar_*(that: MathVector) = operation(that, _ * _).coords.sum

  def *(x: Double) = MathVector(coords.map(_ * x): _*)

  def /(x: Double) = this * (1.0 / x)

  def distTo(that: MathVector) = (this - that).length

  def normalize: MathVector = MathVector(coords.map(x => x / length): _*)
}

object MathVector
{
  implicit def doubleToMathVectorAsScalar(i: Double): MathVectorAsScalar = new MathVectorAsScalar(i)
}

class MathVectorAsScalar(i: Double)
{
  def *(v: MathVector) = v * i
}