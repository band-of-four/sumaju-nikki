package services

import db.Pagination
import game.Fight
import models._
import play.api.libs.json.Json
import services.SuggestionService._

import scala.concurrent.{ExecutionContext, Future}

object SuggestionService {
  case class NoteApproved(id: Long, text: String, isApproved: Boolean)
  case class CreatureApproved(id: Long, name: String, isApproved: Boolean, notes: Seq[NoteApproved])
  case class TextSuggestion(text: String, gender: Student.Gender, lessonName: Option[String])
  case class CreatureSuggestion(name: String, notes: Seq[CreatureTextSuggestion])
  case class CreatureTextSuggestion(text: String, gender: Student.Gender, stage: Student.Stage)

  implicit val textSuggestionReads = Json.reads[TextSuggestion]
  implicit val creatureTextSuggestionReads = Json.reads[CreatureTextSuggestion]
  implicit val creatureSuggestionReads = Json.reads[CreatureSuggestion]
  implicit val creatorNoteWrites = Json.writes[NoteForCreator]
}

class SuggestionService(noteDao: NoteDao,
                        studentDao: StudentDao,
                        creatureDao: CreatureDao,
                        lessonDao: LessonDao)
                       (implicit ec: ExecutionContext) {
  def create(creatorId: Long, suggestion: TextSuggestion): Future[Unit] = Future {
    suggestion match {
      case TextSuggestion(text, gender, Some(lessonName)) =>
        noteDao.createForLesson(creatorId, text, gender, lessonId = lessonDao.findIdByName(lessonName))
    }
  }

  def create(creatorId: Long, suggestion: CreatureSuggestion): Future[Unit] = Future {
    val studentLevel = studentDao.findLevelById(creatorId)
    val creatureStats = Fight.BaseCreatureStats(studentLevel)
    val creature = Creature(
      suggestion.name,
      totalHp = creatureStats.totalHp,
      power = creatureStats.power,
      level = studentLevel
    )
    creatureDao.create(creature) { creature =>
      suggestion.notes.foreach { note =>
        noteDao.createForCreature(creatorId, note.text, note.gender, note.stage, creature.id)
      }
    }
  }

  def getLessonNamesForStudent(studentId: Long): Future[Seq[String]] = Future {
    lessonDao.findNamesForStudentId(studentId)
  }

  def getFirstUnapprovedCreature(): Future[Option[CreatureForApproval]] = Future {
    creatureDao.loadFirstUnapproved()
  }

  def getFirstUnapprovedNote(): Future[Option[NoteForApproval]] = Future {
    noteDao.loadFirstUnapproved()
  }

  def getCreatedByUser(userId: Long, pagination: Pagination): Future[Seq[NoteForCreator]] = Future {
    noteDao.loadForCreator(userId, pagination)
  }
  
  def applyApprovedCreature(ac: CreatureApproved): Future[Unit] = Future {
    creatureDao.applyApproved(ac)
  }
}
