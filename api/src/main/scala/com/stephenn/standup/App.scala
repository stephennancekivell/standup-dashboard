package com.stephenn.standup

import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}

object WebApp extends App {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  def getPath(p: String) = {
    println("get path "+p)
    Option(ClassLoader.getSystemResource("static/" + p)).map(p => Paths.get(p.toURI))
  }

  private def getExtensions(fileName: String) : String = {
    val index = fileName.lastIndexOf('.')
    if(index != 0) {
      fileName.drop(index+1)
    } else
      ""
  }

  val route =
    path("api") {
      logRequestResult("foo") {
        get {
          complete("hello world!")
        }
      }
    } ~ get {
      entity(as[HttpRequest]) { requestData =>
        complete {
          val fullPath = requestData.uri.path.toString match {
            case "/" | "" => getPath("index.html")
            case _ => getPath(requestData.uri.path.toString)
          }

          def onlyFixed(m: MediaType): Option[MediaType.WithFixedCharset] = m match {
            case t: MediaType.WithFixedCharset => Some(t)
            case _ => None
          }

          fullPath match {
            case None =>
              HttpResponse(NotFound)
            case Some(foundPath) =>
              val ext = getExtensions(foundPath.getFileName.toString)
              val maybeContentType = MediaTypes.forExtensionOption(ext)
                .flatMap {
                  case t: MediaType.WithFixedCharset => Some(ContentType(t))
                  case o: MediaType.WithOpenCharset => Some(ContentType(o, HttpCharsets.`UTF-8`))
                  case other =>
                    println("unknown content type for "+other.getClass.getName)
                    None
                }

              val byteArray = Files.readAllBytes(foundPath)
              val entity = maybeContentType.map(HttpEntity(_, byteArray)).getOrElse(HttpEntity(byteArray))
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

object TestWriteToDB {//sextends scala.App {

  val client = new AmazonDynamoDBClient()
  client.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_2))

  val db = new DynamoDB(client)
  val table = db.getTable("standup")

  val result = table.putItem(new Item()
    .withPrimaryKey("id", "id1")
    .withString("value", "value1")
  )
}
