package com.stephenn.standup

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.CirceSupport

import io.circe.generic.auto._
import CirceSupport.circeToEntityMarshaller

object StandupApi {
  val service: StandUpService = StandUpServiceInMemory

  val route = path("members") {
    get {
      complete(service.listMembers())
    } ~ post {
      entity(as[String]) { body =>
        service.addMember(body)
        complete(service.listMembers())
      }
    }
  } ~ path("standup") {
    post {
      parameters("done-by".?, "send-to-back".?) { (doneBy,sendToBack) =>
        doneBy.foreach(service.standupPerformedBy)
        sendToBack.foreach(service.standupPerformedBy)
        complete(service.listMembers())
      }
    }
  }
}