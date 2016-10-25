package com.stephenn.standup

import java.util.UUID

trait StandUpService {
  def listMembers(): Seq[String]
  def addMember(member: String): Unit
  def standupPerformedBy(member: String): Unit
}

object StandUpServiceInMemory extends StandUpService {
  var members: Seq[String] =
    Seq("Bill", "Bob", "Beth")

  def listMembers(): Seq[String] = members
  def addMember(member: String): Unit =
    members = members :+ member

  def standupPerformedBy(member: String): Unit =
    members = members.filterNot(_ == member) :+ member
}

object StandUpServiceEventSourceInMemory extends StandUpService {
  var membersList: Seq[String] = Nil
  sealed trait Event
  case class AddMember(member: String) extends Event
  case class StandupPerformed(by: String) extends Event

  var events: Seq[Event] = Nil

  def listMembers(): Seq[String] = {
    println(events)
    membersList
  }

  def addMember(member: String): Unit =
    addEvent(AddMember(member))

  def processAddMember(event:AddMember): Unit =
    membersList = membersList :+ event.member

  def standupPerformedBy(member: String): Unit =
    addEvent(StandupPerformed(member))

  def processPerformStandup(event: StandupPerformed): Unit =
    membersList = membersList.filterNot(_ == event.by) :+ event.by

  def addEvent(e: Event): Unit = {
    events = events :+ e
    process(e)
  }

  def process(e: Event): Unit = e match {
    case e: AddMember => processAddMember(e)
    case e: StandupPerformed => processPerformStandup(e)
  }

//  type EventProcess = Event
//
//  case class Processor[T](fn: Event[T] => ())
//
//  Processor[AddMember]({ e =>
//    e.member
//
//  })
//
//  val eventProcessors: Seq[Event => ()]

}

object StandupServiceESDynamo extends StandUpService {
  import com.amazonaws.regions.{Region, Regions}
  import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
  import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
  import scala.collection.convert.decorateAsScala._

  val client = new AmazonDynamoDBClient()
  client.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_2))

  val db = new DynamoDB(client)
  val eventsTable = db.getTable("standup-events")
  val membersTable = db.getTable("standup-members")

  var membersList: Seq[String] = Nil
  sealed trait Event
  case class AddMember(member: String) extends Event
  case class StandupPerformed(by: String) extends Event


  def listMembers(): Seq[String] = {
    val maybeData = membersTable.query("key", "members")
      .pages()
      .asScala
      .headOption
      .flatMap(
        _.asScala
        .headOption
        .map(membersFromDynamo))

    maybeData.getOrElse {
      println("couldnt find members")
      Nil
    }
  }

  def addMember(member: String): Unit =
    addEvent(AddMember(member))

  def setMembersList(members: Seq[String]): Unit = {
    membersTable.putItem(membersToDynamo(members))
  }

  def processAddMember(event:AddMember): Unit = {
    setMembersList(listMembers() :+ event.member)
  }

  def membersFromDynamo(it: Item): Seq[String] = {
    decode[Seq[String]](it.getString("data")).toOption.get
  }

  def membersToDynamo(members: Seq[String]): Item =
    new Item()
      .withPrimaryKey("key", "members")
      .withString("data", members.asJson.noSpaces)

  def standupPerformedBy(member: String): Unit =
    addEvent(StandupPerformed(member))

  def processPerformStandup(event: StandupPerformed): Unit =
    setMembersList(listMembers().filterNot(_ == event.by) :+ event.by)

  def addEvent(e: Event): Unit = {
    eventsTable.putItem(eventToDynamo(e))
    process(e)
  }

  def eventToDynamo(e: Event): Item = {
    new Item()
      .withPrimaryKey("id", UUID.randomUUID().toString, "when", System.currentTimeMillis())
      .withString("type", e.getClass.getName)
      .withString("data", e.asJson.noSpaces)
  }

  def process(e: Event): Unit = e match {
    case e: AddMember => processAddMember(e)
    case e: StandupPerformed => processPerformStandup(e)
  }
}
