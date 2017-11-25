package io.github.michaelyaakoby.scalafun.games

object TowersOfHanoi {

  type Tower = List[Int]
  case class Towers(t1: Tower, t2: Tower, t3: Tower) {
    def apply(i: Int): Tower = i match {
      case 1 => t1
      case 2 => t2
      case 3 => t3
    }

    def set(i: Int, tower: Tower) = i match {
      case 1 => copy(t1 = tower)
      case 2 => copy(t2 = tower)
      case 3 => copy(t3 = tower)
    }

    override def toString: String = s"1(${t1.mkString(",")}), 2(${t2.mkString(",")}), 3(${t3.mkString(",")})"
  }

  case class DiskMove(fromTower: Int, toTower: Int)

  def move(initialTower: Tower): List[DiskMove] = {
    def move(towers: Towers, from: Int, target: Int): List[DiskMove] = {
      val other = 6 - from - target
      val fromTower = towers(from)
      if (fromTower.isEmpty) List.empty[DiskMove]
      else {
        // move all but last to other
        val allButLastToOther = move(towers.set(from, fromTower.dropRight(1)), from, other)

        // move last to target
        val lastToTargetTower = DiskMove(from, target)

        // move all from other to target
        val otherToToTower = move(towers.set(from, List.empty).set(target, List(fromTower.last)).set(other, fromTower.dropRight(1)), other, target)

        (allButLastToOther :+ lastToTargetTower) ++ otherToToTower
      }
    }

    move(Towers(initialTower, List.empty, List.empty), 1, 2)
  }

}
