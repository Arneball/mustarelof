package utils

/** Interface for transforming T => String */
trait ToJson[T]{
  def toJson(t: T): String
}

/** Som javas interface, men man kan ha implementation i dem också */
trait Plant {
  /** Length above ground */
  def length: Double // abstract
  /** Plant name */
  def plantName: String // abstract
  
  /** Concrete method */
  def rootDepth = length / 2  // rot längden är hälften av längden
}

/** A class' or Traits companion object contains singleton stuff related to the class */
object Plant {
  implicit object toJson extends ToJson[Plant]{
    override def toJson(p: Plant) = s"""{"length": ${p.length}, "plantName": "${p.plantName}"}""" 
  } 
}

/** Implements Plant */
case class Tree(plantName: String, length: Double) extends Plant // ClassName(Constructorparam1 ... ConstructorParamN) extends Trait with Trait2
case class Orchid(rgbColor: Int, plantName: String, length: Double) extends Plant


object ScalaTutorial {
  def main(args: Array[String]): Unit = {
    printPlant(new Orchid(13, "Shit", 0xFF00FF))
  }
  
  def printPlant(plant: Plant) = plant match { // Pattern matching
    case Tree("gran", _) => println("We got a gran") // pattern match på delar av granen
    case Tree(_, length) => println(s"We got a tall tree! Length is $length")
    case Orchid(color, name, length) => println(s"We got an ochid with color $color") // 
  }
  
  /** List är en klass som ärver av Seq (Sequence) */
  def getLengthSum(plants: List[Plant]): Double = plants.foldLeft(0.0){case (accumulator: Double, plant: Plant) => plant.length + accumulator }
  /** Same shit */
  def getLengthSum2(plants: List[Plant]): Double = plants.foldLeft(0.0){ _ + _.length }
  def longestPlant(plants: List[Plant]) = plants.maxBy{ plant => plant.length }
  def longestTree(plants: List[Plant]) = plants.maxBy{ _.length }
  
  def partition(plants: List[Plant]) = {
    val (trees, notTrees) = plants.partition{ _.isInstanceOf[Tree] }
    (trees, notTrees)  
  }
  /** Returns a sorted List, sorted by plant.length */
  def sort(plants: List[Plant]) = plants.sortBy{ _.length }

  /** Gets a list of plants, pattern matches and extracts the lengths */
  def tree_lengths(plants: List[Plant]): Seq[Double] = for {
    Tree(_, length) <- plants
  } yield length
  
  def parse_string(str: String): Option[Plant] = str match {
    case "TREE" => Some(Tree("generic tree", 0))
    case _ => None 
  }
  
  def try_parse = parse_string("jadad") match {
    case Some(tree) => println(s"Successfully parsed $tree")
    case None => println("No tree :( ")
  }
  
  /** Option[Plant].filter{ Plant => Booleaon} returnerar Option[Plant] 
   *  Option[Plant].map[U]{ Plant => U} returnerar Option[U]
   */
  def advanced_tree_parsing: String = {
                  // Option[Plant]     ->   Option[Plant]  ->        Option[String] -> String
    parse_string("blabla").filter{ _.length > 0 }.map{ plant => plant.plantName }.getOrElse("No tree bigger than 0 found")
  }
  
  def toJson[T](theValueToBecomeJson: T)(implicit converter: ToJson[T]) = converter.toJson(theValueToBecomeJson)
  // Same with other syntax
  def toJson2[T : ToJson](valueToJson: T) = implicitly[ToJson[T]].toJson(valueToJson)
  
  def plantToJson(plant: Plant) = toJson(plant) // it will look for an implicit ToJson[Plant], and it will find it in object Plant
  
  def filterTest(list: Seq[String]) = {
    def predicate(str: String): Boolean = str.isEmpty
    def predicate0(str: String) = str.isEmpty
    type Pred = String => Boolean
    def predicate2: Pred = (str: String) => str.isEmpty
    def predicate3: Pred = _.isEmpty
    
    def getMegaString: String = io.Source.fromFile("/tmp/megastring.txt").getLines.mkString 
    def isGreaterThanMega(str: String): Boolean = str.length > getMegaString.length // superslow
    val isGreater2: Pred = {
      val megalength = getMegaString.length
      str => str.length > megalength                    // creates an anomyous object with megalength cached
    }
    
    list.filter(isGreaterThanMega) // fail
    list.filter{ str => str.length > getMegaString.length } // same as above
    list.filter{ _.length > getMegaString.length }          // same as above
    list.filter(isGreater2) // faster since isGreater2 is a local variable and has megalength cached
    
    
  }
}


// MOBILER-labben från chalmers
import Node._

trait Mobile {
  def weight: Double
  def height: Int
  def mirror: Mobile
}

case class Node(left: Mobile, right: Mobile, leftLen: Double, rightLen: Double) extends Mobile {
  def height = 1 + Math.max(left.height, right.height)
  def mirror = copy(left = right.mirror, right = left.mirror, leftLen = rightLen, rightLen = leftLen)
  def weight = left.weight + right.weight
  def isBalanced = left.weight * leftLen ~= right.weight * rightLen
  
  override def equals(that: Any) = that match {
    case Node(thatLeft, thatRight, le_leftLen, le_rightLen) =>
      thatLeft == left && thatRight == right && (le_leftLen ~= le_rightLen)
    case _ => false
  }
  override def toString = s"[($left), $leftLen, ($right), $rightLen]"
}

case class Leaf(weight: Double) extends Mobile{
  def height = 1
  def mirror = this
  def isBalanced = true

  override def equals(that: Any) = that match{
    case Leaf(le_w) => le_w ~= weight
    case _ => false
  }
  override def toString = s"$weight"
}

/** Alla dessa komparatorobject är ekvivalenta */
object Mobile {
  implicit val ord = new Ordering[Mobile] {
    def compare(e1: Mobile, e2: Mobile) = (e1.weight - e2.weight).toInt
  }
  
  private val ord2: Ordering[Mobile] = Ordering.by{ _.weight } // Mest läsbar?
  private val ord3: Ordering[Mobile] = Ordering.fromLessThan{ _.weight < _.weight } // klovad? 
                          //Samma som  Ordering.fromLessThan{ (e1, e2) => e1.weight < e2.weight }
  private object ord4 extends Ordering[Mobile] {
    def compare(m1: Mobile, m2: Mobile) = Ordering.Double.compare(m1.weight, m2.weight)  // verbos
  }
}

object Node {
  
  implicit class DoubleW(val f: Double) extends AnyVal {
    /** pimpa Double med metoden ~= som kollar om differensen är inom ett intervall */
    def ~=(that: Double) = Math.abs(that - f) < 0.01 
  }
}