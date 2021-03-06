import java.io.Closeable

import actors.{GameProgressionActor, SocketMessengerActor}
import akka.actor.{ActorRef, Props}
import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import io.getquill._
import javax.sql.DataSource
import play.api.mvc.EssentialFilter
import utils.auth.SilhouetteLoader

class AppLoader extends ApplicationLoader {
  private var components: AppComponents = _

  override def load(ctx: Context): Application = {
    components = new AppComponents(ctx)

    if (components.configuration.get[Boolean]("game-progression.enabled"))
      components.gameProgressionActor ! GameProgressionActor.Poll

    components.application
  }
}

class AppComponents(ctx: Context) extends BuiltInComponentsFromContext(ctx)
                                  with play.filters.headers.SecurityHeadersComponents
                                  with play.api.db.DBComponents
                                  with play.api.db.evolutions.EvolutionsComponents
                                  with play.api.db.HikariCPComponents
                                  with play.api.libs.ws.ahc.AhcWSComponents
                                  with _root_.controllers.AssetsComponents {
  applicationEvolutions

  def httpFilters: Seq[EssentialFilter] = Nil

  lazy val db = new _root_.db.DbCtx(CompositeNamingStrategy2(SnakeCase, PluralizedTableNames),
    dbApi.database("default").dataSource.asInstanceOf[DataSource with Closeable])

  lazy val dbExecCtx = new _root_.db.DbExecutionContext(actorSystem, "db.default.executor")

  /* DAOs */
  lazy val userDao = new _root_.models.UserDao(db)
  lazy val userLoginInfoDao = new _root_.models.UserLoginInfoDao(db, dbExecCtx)
  lazy val studentDao = new _root_.models.StudentDao(db)
  lazy val roomDao = new _root_.models.RoomDao(db)
  lazy val lessonDao = new _root_.models.LessonDao(db)
  lazy val creatureDao = new _root_.models.CreatureDao(db)
  lazy val spellDao = new _root_.models.SpellDao(db)
  lazy val studentDiaryDao = new _root_.models.StudentDiaryDao(db)
  lazy val noteDao = new _root_.models.NoteDao(db)
  lazy val owlDao = new _root_.models.OwlDao(db)
  lazy val clubDao = new _root_.models.ClubDao(db)
  lazy val libraryDao = new _root_.models.StudentLibraryVisitDao(db)
  lazy val relationshipDao = new _root_.models.StudentRelationshipDao(db)
  /* Services */
  lazy val userService = new _root_.services.UserService(
    userDao, userLoginInfoDao, db, dbExecCtx, configuration)
  lazy val studentService = new _root_.services.StudentService(
    studentDao, spellDao, lessonDao, noteDao, creatureDao, relationshipDao)
  lazy val noteService = new _root_.services.NoteService(
    noteDao, studentDiaryDao, libraryDao)
  lazy val stageService = new _root_.services.StageService(
    studentDao, noteDao, studentDiaryDao, noteService)
  lazy val owlService = new _root_.services.OwlService(
    owlDao, studentDao, relationshipDao)
  lazy val libraryService = new _root_.services.LibraryService(
    spellDao, libraryDao)
  lazy val gameProgressionService = new _root_.services.GameProgressionService(
    stageService, owlService, libraryService, roomDao, lessonDao, creatureDao, spellDao, relationshipDao)
  lazy val suggestionService = new _root_.services.SuggestionService(
    noteDao, studentDao, creatureDao, clubDao, lessonDao)
  /* Actors */
  lazy val socketMessengerActor: ActorRef = actorSystem.actorOf(
    Props[SocketMessengerActor], "socket-messenger-actor")
  lazy val gameProgressionActor: ActorRef = actorSystem.actorOf(
    Props(new GameProgressionActor(gameProgressionService, socketMessengerActor)), "game-progression-actor")
  /* Auth */
  lazy val silhouette = new SilhouetteLoader(configuration, userService, wsClient)
  lazy val botSecuredAction = new _root_.utils.auth.BotSecuredAction(configuration, controllerComponents)
  /* Controllers */
  lazy val authController = new _root_.controllers.AuthController(
    controllerComponents, silhouette.env, silhouette.credentialsProvider, silhouette.socialProviderRegistry, userService)
  lazy val applicationController = new _root_.controllers.ApplicationController(
    controllerComponents, silhouette.env, socketMessengerActor, stageService)(materializer, executionContext, actorSystem)
  lazy val studentController = new _root_.controllers.StudentController(
    controllerComponents, silhouette.env, studentService)
  lazy val suggestionController = new _root_.controllers.SuggestionController(
    controllerComponents, silhouette.env, suggestionService)
  lazy val noteController = new _root_.controllers.NoteController(
    controllerComponents, silhouette.env, noteService)
  lazy val owlController = new _root_.controllers.OwlController(
    controllerComponents, silhouette.env, owlService)
  lazy val botController = new _root_.controllers.BotController(
    controllerComponents, botSecuredAction, suggestionService)
  /* Routes */
  lazy val router: Router = new _root_.router.Routes(
    httpErrorHandler, applicationController, assets,
    authController, studentController, owlController, noteController, suggestionController, botController)
}
