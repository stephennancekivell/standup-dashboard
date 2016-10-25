package com.stephenn.standup

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

object StandupApi {
  val service: StandUpService = StandupServiceESDynamo

  def toJson(members: Seq[String]) =
    members.asJson.noSpaces

  val route = path("members") {
    get {
      complete(toJson(service.listMembers()))
    } ~ post {
      entity(as[String]) { body =>
        service.addMember(body)
        complete(toJson(service.listMembers()))
      }
    }
  } ~ path("standup") {
    post {
      parameters("done-by".?, "send-to-back".?) { (doneBy,sendToBack) =>
        doneBy.foreach(service.standupPerformedBy)
        sendToBack.foreach(service.standupPerformedBy)
        complete(toJson(service.listMembers()))
      }
    }
  }
}