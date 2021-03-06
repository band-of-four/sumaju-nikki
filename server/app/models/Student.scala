package models

import java.time.{Duration, LocalDateTime}

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import db.{DbCtx, PgEnum}
import play.api.libs.json.Json

object Student {
  sealed trait Gender extends EnumEntry
  case object Gender extends Enum[Gender] with PgEnum[Gender] with PlayJsonEnum[Gender] {
    case object Female extends Gender
    case object Male extends Gender

    val values = findValues
  }

  sealed trait Stage extends EnumEntry
  case object Stage extends Enum[Stage] with PgEnum[Stage] with PlayJsonEnum[Stage] {
    case object Lesson extends Stage
    case object Club extends Stage
    case object Travel extends Stage
    case object Fight extends Stage
    case object FightWon extends Stage
    case object FightLost extends Stage
    case object Infirmary extends Stage
    case object Library extends Stage

    val values = findValues
  }

  implicit val studentWrites = Json.writes[Student]
}

case class Student(id: Long, name: String, gender: Student.Gender, level: Int, hp: Int, currentRoom: Long,
                   stageNoteId: Long, stageStartTime: LocalDateTime, nextStageTime: LocalDateTime) {
  // Convenience getter for constructing gender-specific messages
  def fem: Boolean = gender == Student.Gender.Female
}

case class StudentForUpdate(id: Long, gender: Student.Gender,
                            level: Int, hp: Int, currentRoom: Long, stage: Student.Stage)

class StudentDao(val db: DbCtx) {
  import db._

  def create(student: Student)(depsTransaction: Student => Unit): Student =
    transaction {
      run(query[Student].insert(lift(student)))
      depsTransaction(student)
      student
    }

  def doTransaction[T](f: => T): T = transaction(f)

  def findByName(name: String): Option[Student] =
    run(query[Student].filter(_.name == lift(name))).headOption

  def findLevelById(studentId: Long): Int =
    run(query[Student].filter(_.id == lift(studentId)).map(_.level)).head

  def findForUser(userId: Long): Option[Student] =
    run(query[Student].filter(_.id == lift(userId))).headOption

  def findStageForUser(userId: Long): Student.Stage =
    run(
      query[Student]
        .join(query[Note]).on((s, n) => s.id == lift(userId) && s.stageNoteId == n.id)
        .map(_._2.stage)
    ).head

  def findPendingStageUpdate(count: Int): Seq[StudentForUpdate] =
    run(
      query[Student]
        .filter(_.nextStageTime <= lift(LocalDateTime.now()))
        .sortBy(_.nextStageTime)(Ord.asc)
        .join(query[Note]).on {
          case (s, n) => s.stageNoteId == n.id
        }
        .map {
          case (s, n) => StudentForUpdate(s.id, s.gender, s.level, s.hp, s.currentRoom, n.stage)
        }
        .take(lift(count))
        .forUpdate
    )

  def updateStage(student: StudentForUpdate, stageNoteId: Long, stageDuration: Duration): Unit =
    run(
      query[Student]
        .filter(_.id == lift(student.id))
        .update(_.hp -> lift(student.hp),
          _.currentRoom -> lift(student.currentRoom),
          _.stageNoteId -> lift(stageNoteId),
          _.stageStartTime -> lift(LocalDateTime.now()),
          _.nextStageTime -> lift(LocalDateTime.now().plus(stageDuration)))
    )

  def updateHp(studentId: Long, power: Int): Unit =
    run(
      query[Student]
        .filter(_.id == lift(studentId))
        .update(s => s.hp -> (s.hp + lift(power)))
      )

  def levelUp(studentId: Long): Unit =
    run(
      query[Student]
        .filter(_.id == lift(studentId))
        .update(s => s.level -> (s.level + 1))
      )
}
