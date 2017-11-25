package io.github.michaelyaakoby.scalafun.games

import io.github.michaelyaakoby.scalafun.games.TowersOfHanoi._
import org.scalatest.{Matchers, WordSpec}

class TowersOfHanoiTest extends WordSpec with Matchers {

  def solveAndVerify(initialTower: Tower) = {
    def verify(initialTower: Tower, moves: Seq[DiskMove]) = {
      def verifyAndMove(towers: Towers, move: DiskMove): Towers = {
        val fromTower = towers(move.fromTower)
        fromTower should not be empty

        val diskToMove = fromTower.head

        val toTower = towers(move.toTower)
        toTower.foreach(topDisk => topDisk shouldBe >(diskToMove))

        towers
          .set(move.fromTower, fromTower.drop(1))
          .set(move.toTower, diskToMove :: toTower)
      }

      def verifyResults(targetTower: Int,
                        initialTower: Tower,
                        results: Towers) = {
        (1 to 3).foreach {
          case index if index != targetTower => results(index) should be(empty)
          case index if index == targetTower =>
            results(index) should be(initialTower)
        }
      }

      val results =
        moves.foldLeft(Towers(initialTower, List.empty, List.empty)) {
          (towers, move) =>
            val newTowers = verifyAndMove(towers, move)
            println(s"$towers -> $move -> $newTowers")
            newTowers
        }
      verifyResults(2, initialTower, results)
    }

    println(s"--- Solving $initialTower ---")
    verify(initialTower, move(initialTower))
  }

  "towers of hanoi" should {
    "move 2 disks" in {
      solveAndVerify(List(1, 2))
    }

    "move 3 disks" in {
      solveAndVerify(List(1, 2, 3))
    }

    "move 5 disks" in {
      solveAndVerify(List(1, 2, 3, 4, 5))
    }

    "move 10 disks" in {
      solveAndVerify((1 to 10).toList)
    }
  }
}
