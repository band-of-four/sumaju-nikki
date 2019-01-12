package services

import models.StudentDao
import play.api.libs.json.Json
import services.StageService.CurrentStage
import java.time.ZoneId

import scala.concurrent.ExecutionContext

object StageService {
  case class CurrentStage(name: String, durationMs: Long, elapsedMs: Long)

  implicit val stageWrites = Json.writes[CurrentStage]
}

class StageService(studentDao: StudentDao)(implicit ec: ExecutionContext) {
  def getCurrentStage(userId: Long): CurrentStage = {
    val student = studentDao.findForUser(userId).get

    val startTime = student.stageStartTime.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
    val endTime = student.nextStageTime.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
    val duration = endTime - startTime

    CurrentStage(
      student.stage,
      durationMs = endTime - startTime,
      elapsedMs = System.currentTimeMillis() - startTime
    )
  }
}