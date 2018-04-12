// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.seqexec.web.server.http4s

import cats.effect.IO
import org.log4s._
import ch.qos.logback.core.Appender
import ch.qos.logback.classic.spi.ILoggingEvent
import edu.gemini.seqexec.server
import edu.gemini.seqexec.model.events.SeqexecEvent
import edu.gemini.seqexec.server.{SeqexecEngine, executeEngine}
import edu.gemini.seqexec.web.server.OcsBuildInfo
import edu.gemini.seqexec.web.server.security.{AuthenticationConfig, AuthenticationService, LDAPConfig}
import edu.gemini.seqexec.web.server.logging.AppenderForClients
import edu.gemini.web.server.common.{LogInitialization, RedirectToHttpsRoutes, StaticRoutes}
import fs2.StreamApp
import cats.implicits._
import knobs._
import org.http4s.server.SSLKeyStoreSupport.StoreInfo
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.Server
import squants.time.Hours

object WebServerLauncher extends StreamApp[IO] with LogInitialization {
  private val logger = getLogger

  final case class SSLConfig(keyStore: String, keyStorePwd: String, certPwd: String)

  /**
    * Configuration for the web server
    */
  final case class WebServerConfiguration(site: String, host: String, port: Int, insecurePort: Int, externalBaseUrl: String, devMode: Boolean, sslConfig: Option[SSLConfig])

  // Attempt to get the configuration file relative to the base dir
  val configurationFile: IO[java.nio.file.Path] = baseDir.map(_.resolve("conf").resolve("app.conf"))

  // Read the config, first attempt the file or default to the classpath file
  val defaultConfig: IO[Config] =
    knobs.loadImmutable(ClassPathResource("app.conf").required :: Nil)

  val fileConfig: IO[Config] = configurationFile >>= { f =>
    knobs.loadImmutable(FileResource(f.toFile).optional :: Nil)
  }

  val config: IO[Config] =
    for {
      dc <- defaultConfig
      fc <- fileConfig
    } yield dc ++ fc

  // configuration specific to the web server
//  val serverConf: IO[WebServerConfiguration] =
//    config.map { cfg =>
//      val site            = cfg.require[String]("seqexec-engine.site")
//      val host            = cfg.require[String]("web-server.host")
//      val port            = cfg.require[Int]("web-server.port")
//      val insecurePort    = cfg.require[Int]("web-server.insecurePort")
//      val externalBaseUrl = cfg.require[String]("web-server.externalBaseUrl")
//      val devMode         = cfg.require[String]("mode")
//      val keystore        = cfg.lookup[String]("web-server.tls.keyStore")
//      val keystorePwd     = cfg.lookup[String]("web-server.tls.keyStorePwd")
//      val certPwd         = cfg.lookup[String]("web-server.tls.certPwd")
//      val sslConfig       = (keystore |@| keystorePwd |@| certPwd)(SSLConfig.apply)
//      WebServerConfiguration(site, host, port, insecurePort, externalBaseUrl, devMode.equalsIgnoreCase("dev"), sslConfig)
//    }
//
//  // Configuration of the ldap clients
//  val ldapConf: IO[LDAPConfig] =
//    config.map { cfg =>
//      val urls = cfg.require[List[String]]("authentication.ldapURLs")
//      LDAPConfig(urls)
//    }
//
//  // Configuration of the authentication service
//  val authConf: Kleisli[IO, WebServerConfiguration, AuthenticationConfig] = Kleisli { conf =>
//    for {
//      ld <- ldapConf
//      cfg <- config
//    } yield {
//      val devMode = cfg.require[String]("mode")
//      val sessionTimeout = cfg.require[Int]("authentication.sessionLifeHrs")
//      val cookieName = cfg.require[String]("authentication.cookieName")
//      val secretKey = cfg.require[String]("authentication.secretKey")
//      val sslSettings = cfg.lookup[String]("web-server.tls.keyStore")
//      AuthenticationConfig(devMode.equalsIgnoreCase("dev"), Hours(sessionTimeout), cookieName, secretKey, sslSettings.isDefined, ld)
//    }
//  }
//
//  /**
//    * Configures the Authentication service
//    */
//  def authService: Kleisli[IO, AuthenticationConfig, AuthenticationService] = Kleisli { conf =>
//    IO.delay(AuthenticationService(conf))
//  }
//
//  /**
//    * Configures and builds the web server
//    */
//  def webServer(as: AuthenticationService, events: (server.EventQueue, Topic[SeqexecEvent]), se: SeqexecEngine): Kleisli[IO, WebServerConfiguration, Server] = Kleisli { conf =>
//    val builder = BlazeBuilder.bindHttp(conf.port, conf.host)
//      .withWebSockets(true)
//      .mountService(new StaticRoutes(index(conf.site, conf.devMode, OcsBuildInfo.builtAtMillis), conf.devMode, OcsBuildInfo.builtAtMillis).service, "/")
//      .mountService(new SeqexecCommandRoutes(as, events._1, se).service, "/api/seqexec/commands")
//      .mountService(new SeqexecUIApiRoutes(conf.devMode, as, events, se).service, "/api")
//    conf.sslConfig.fold(builder) { ssl =>
//      val storeInfo = StoreInfo(ssl.keyStore, ssl.keyStorePwd)
//      builder.withSSL(storeInfo, ssl.certPwd, "TLS")
//    }.start
//  }
//
//  def redirectWebServer: Kleisli[IO, WebServerConfiguration, Server] = Kleisli { conf =>
//    val builder = BlazeBuilder.bindHttp(conf.insecurePort, conf.host)
//      .mountService(new RedirectToHttpsRoutes(443, conf.externalBaseUrl).service, "/")
//    builder.start
//  }
//
//  def logStart: Kleisli[IO, WebServerConfiguration, Unit] = Kleisli { conf =>
//    val msg = s"Start web server for site ${conf.site} on ${conf.devMode ? "dev" | "production"} mode"
//    IO.delay { logger.info(msg) }
//  }
//
//  // We need to manually update the configuration of the logging subsystem
//  // to support capturing log messages and forward them to the clients
//  def logToClients(out: Topic[SeqexecEvent]): IO[Appender[ILoggingEvent]] = IO.delay {
//    import org.slf4j.LoggerFactory
//    import ch.qos.logback.classic.LoggerContext
//    import ch.qos.logback.classic.Logger
//    import ch.qos.logback.classic.AsyncAppender
//
//    val asyncAppender = new AsyncAppender
//    val appender = new AppenderForClients(out)
//    Option(LoggerFactory.getILoggerFactory()).collect {
//      case lc: LoggerContext => lc
//    }.foreach { ctx =>
//      asyncAppender.setContext(ctx)
//      appender.setContext(ctx)
//      asyncAppender.addAppender(appender)
//    }
//
//    Option(LoggerFactory.getLogger("edu.gemini.seqexec")).collect {
//      case l: Logger => l
//    }.foreach { l =>
//      l.addAppender(asyncAppender)
//      asyncAppender.start()
//      appender.start()
//    }
//    asyncAppender
//  }
//
//  /**
//    * Reads the configuration and launches the web server
//    */
//  override def process(args: List[String]): Process[IO, Nothing] = {
//    val engineIO = for {
//      _    <- configLog // Initialize log before the engine is setup
//      c    <- config
//      seqc <- SeqexecEngine.seqexecConfiguration.run(c)
//    } yield SeqexecEngine(seqc)
//
//    val inq  = async.boundedQueue[executeEngine.EventType](10)
//    val out  = async.topic[SeqexecEvent]()
//
//    // It should be possible to cleanup the engine at shutdown in this function
//    def cleanup = (s: SeqexecEngine) => Process.eval_(IO.now(()))
//    Process.bracket(engineIO)(cleanup) { case et =>
//        val pt = Nondeterminism[IO].both(
//          // Launch engine and broadcast channel
//          et.eventProcess(inq).to(out.publish).run,
//          // Launch web server
//          for {
//            wc <- serverConf
//            ac <- authConf.run(wc)
//            as <- authService.run(ac)
//            rd <- redirectWebServer.run(wc)
//            _  <- logStart.run(wc)
//            _  <- logToClients(out)
//            ws <- webServer(as, (inq, out), et).run(wc)
//          } yield (ws, rd)
//        )
//      Process.eval_(pt.map(_._2))
//    }
//  }

}
