package com.drewfus.stocks

import akka.actor.{Props, ActorRef, Actor, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import org.htmlcleaner.{TagNode, HtmlCleaner}
import spray.client.pipelining._

import scala.concurrent.{Await, Future}

case class SNP500Constituents(tickers :List[String])

object SNP500Constituents {

  private[this] lazy val cleaner = new HtmlCleaner()

  def parseTickers(html: String) = {
    SNP500Constituents(
      cleaner
      .clean(html)
      .evaluateXPath("//*[@id=\"mw-content-text\"]/table[1]/tbody/tr/*[1]")
      .map(x => x.asInstanceOf[TagNode].getText().toString)
      .filter(!_.startsWith("Ticker"))
      .toList
    )
  }
}

object SNP500ConstituentsActor {
  def props(toUpdate :ActorRef) = Props(new SNP500ConstituentsActor(toUpdate))
}

class SNP500ConstituentsActor(toUpdate :ActorRef) extends Actor {

  case object Tick

  import context.dispatcher

  override def preStart() =
    context.system.scheduler.scheduleOnce(500 millis, self, Tick)

  def receive = {
    case Tick => {
      // execution context for futures
      val pipeline = sendReceive ~> unmarshal[String]
      val response = pipeline(Get("http://en.wikipedia.org/wiki/List_of_S%26P_500_companies"))
      val result = response.map(SNP500Constituents.parseTickers(_))
      implicit val timeout = Timeout(10 seconds)
      val tickers = Await.result(result, timeout.duration)
      toUpdate ! tickers
      context.system.scheduler.scheduleOnce(1 minute, self, Tick)
    }
  }

}