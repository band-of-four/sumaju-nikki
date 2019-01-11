package models

import db.DbCtx

case class Spell(name: String, kind: String, power: Int, level: Int, id: Long = -1)

case class SpellsStudent(spellId: Long, studentId: Long)

object Spell {
  val AttackSpell = "attack"
  val LuckSpell = "luck"
  val DefenceSpell = "defence"
}

class SpellDao(val db: DbCtx) {
  import db._

  def findLearned(studentId: Long): Seq[Spell] =
    run(
      query[Spell]
        .join(query[SpellsStudent])
        .on((sp, spst) => sp.id == spst.spellId && spst.studentId == lift(studentId))
        .map(_._1)
    )

  def findRandom(minLevel: Int, maxLevel: Int): Spell =
    run(
      query[Spell]
        .filter(s => s.level <= lift(maxLevel) && s.level >= lift(minLevel))
        .randomSort
        .take(1)
    ).head
}