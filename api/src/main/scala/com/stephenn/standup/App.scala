package com.stephenn.standup=

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._

import scala.io.Source

object WebApp extends App {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  def readFile(p: String): Option[String] = {
    val path = p.startsWith("/") match {
      case true => "/static" + p
      case false => "/static/" + p
    }
    println("reading "+path)
    val stream = Option(getClass.getResourceAsStream(path))
    stream.map(Source.fromInputStream(_).getLines().mkString)
  }

  private def getExtensions(fileName: String) : String = {
    val index = fileName.lastIndexOf('.')
    if(index != 0) {
      fileName.drop(index+1)
    } else
      ""
  }

  val route =
    StandupApi.route ~
    path("api") {
      StandupApi.route ~
      logRequestResult("foo") {
        get {
          complete("hello world!")
        }
      }
    } ~ get {
      entity(as[HttpRequest]) { requestData =>
        complete {
          val fullPath = requestData.uri.path.toString match {
            case "/" | "" => "index.html"
            case _ => requestData.uri.path.toString
          }

          val maybeContent = readFile(fullPath)

          maybeContent match {
            case None =>
              HttpResponse(NotFound)
            case Some(content) =>
              val ext = getExtensions(requestData.uri.path.toString)
              val maybeContentType = MediaTypes.forExtensionOption(ext)
                .flatMap {
                  case t: MediaType.WithFixedCharset => Some(ContentType(t))
                  case o: MediaType.WithOpenCharset => Some(ContentType(o, HttpCharsets.`UTF-8`))
                  case other =>
                    println("unknown content type for "+other.getClass.getName)
                    None
                }

              val entity = maybeContentType.map(HttpEntity(_, content)).getOrElse(HttpEntity(content))
              HttpResponse(OK, entity = entity)
          }
        }
      }
    }

  val logger = Logging(system, getClass)

  val bindingFuture = Http().bindAndHandle(route, AppConfig.http.interface, AppConfig.http.port)

  bindingFuture.onComplete { _ =>
    println("listening on "+AppConfig.http.port)
  }

  sys.addShutdownHook(system.terminate())
}

object AppConfig {
  object http {
    val interface = "0.0.0.0"
    val port = 8080
  }
}