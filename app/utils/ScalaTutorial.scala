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
}
