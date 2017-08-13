package sharry.server

import java.nio.file.Path
import java.nio.channels.AsynchronousChannelGroup
import scala.collection.JavaConverters._

import sharry.common.version
import sharry.docs.route
import sharry.docs.md.ManualContext
import sharry.store.account._
import sharry.store.binary._
import sharry.store.upload._
import sharry.server.authc._
import sharry.webapp.route.webjar
import sharry.common.data._
import sharry.server.routes.{account, login, upload, download, alias, mail, settings}

/** Instantiate the app from a given configuration */
final class App(val cfg: config.Config)(implicit ACG: AsynchronousChannelGroup, S: fs2.Strategy, SCH: fs2.Scheduler) {
  if (cfg.logConfig.exists) {
    setupLogging(cfg.logConfig.config)
  }

  val jdbc = cfg.jdbc.transactor.unsafeRun

  val binaryStore: BinaryStore = new SqlBinaryStore(jdbc)
  val accountStore: AccountStore = new SqlAccountStore(jdbc)
  val uploadStore: UploadStore = new SqlUploadStore(jdbc, binaryStore)

  val auth = new Authenticate(accountStore, cfg.authConfig, ExternAuthc(cfg))

  val uploadConfig = cfg.uploadConfig

  val remoteConfig = RemoteConfig(
    paths.mounts.mapValues(_.path) + ("baseUrl" -> cfg.webConfig.baseurl)
      , cfg.webConfig.appName
      , cfg.authConfig.enable
      , cfg.authConfig.maxCookieLifetime.toMillis
      , uploadConfig.chunkSize.toBytes
      , uploadConfig.simultaneousUploads
      , uploadConfig.maxFiles
      , uploadConfig.maxFileSize.toBytes
      , uploadConfig.maxValidity.toString
      , version.projectString
      , routes.authz.aliasHeaderName
      , cfg.webmailConfig.enable
      , cfg.webConfig.highlightjsTheme
      , cfg.webConfig.welcomeMessage
      , version.shortVersion
  )

  val notifier: notification.Notifier = notification.scheduleNotify(
    cfg.smtpSetting, cfg.webConfig, cfg.webmailConfig, uploadStore, accountStore)_


  def endpoints = {
    routes.syntax.choice2(
      webjar.endpoint(remoteConfig)
        , route.manual(paths.manual.matcher, ManualContext(version.longVersion, version.shortVersion, defaultConfig, defaultCliConfig, cliHelp))
        , login.endpoint(auth, cfg.webConfig, cfg.authConfig)
        , account.endpoint(auth, cfg.authConfig, accountStore, cfg.webConfig)
        , upload.endpoint(cfg.authConfig, uploadConfig, uploadStore, notifier)
        , download.endpoint(cfg.authConfig, cfg.webConfig, uploadStore)
        , alias.endpoint(cfg.authConfig, uploadConfig, uploadStore)
        , mail.endpoint(cfg.authConfig, cfg.smtpSetting, cfg.webmailConfig)
        , settings.endpoint(remoteConfig)
    )
  }

  private lazy val defaultConfig = {
    getClass.getClassLoader.getResources("reference.conf").
      asScala.toList.
      filter(_.toString contains "sharry-server").
      map(scala.io.Source.fromURL(_).getLines.mkString("\n")).
      headOption.getOrElse("")
  }

  private lazy val defaultCliConfig = {
    Option(getClass.getResource("/reference-cli.conf")).
      map(scala.io.Source.fromURL(_).getLines.mkString("\n")).
      headOption.getOrElse("")
  }

  private lazy val cliHelp = {
    Option(getClass.getResource("/cli-help.txt")).
      map(scala.io.Source.fromURL(_).getLines.mkString("\n")).
      headOption.getOrElse("")
  }

  def setupLogging(logFile: Path): Unit = {
    import org.slf4j.LoggerFactory
    import ch.qos.logback.classic.LoggerContext
    import ch.qos.logback.classic.joran.JoranConfigurator
    import ch.qos.logback.core.util.StatusPrinter
    val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    scala.util.Try {
      val config = new JoranConfigurator()
      config.setContext(context)
      context.reset()
      config.doConfigure(logFile.toString)
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(context)
  }
}
