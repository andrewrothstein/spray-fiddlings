package com.drewfus.stocks

import akka.actor.ActorSystem
import akka.util.Timeout
import spray.client.pipelining._
import spray.http.Uri
import scala.concurrent.duration._

import scala.concurrent.Await
import scala.xml.XML

case class Mover(ticker :String, name :String, move :Double)

/**
 * Created by drew on 5/31/15.
 */
object YFQuery {

  def query(constituents: SNP500Constituents) = "select * from yahoo.finance.quote where symbol in " +
    "(" + constituents.tickers.map('"' + _ + '"').mkString(",") + ")"

  def movers(constituents: SNP500Constituents)(implicit system :ActorSystem) = {

    import system.dispatcher

    val pipeline = sendReceive ~> unmarshal[String]
    val uri = Uri("https://query.yahooapis.com/v1/public/yql")
      .withQuery(
        ("q", query(constituents)),
        ("env", "http://datatables.org/alltables.env"),
        ("format", "xml")
      )
    val response = pipeline(Get(uri))
    val results = response.map(x => {
      val xml = XML.loadString(x.toString)
      val quoteXMLs = xml \ "results" \\ "quote"
      quoteXMLs.map(n => Mover(
          ticker = (n \ "@symbol").text,
          name = (n \ "Name").text,
          move = (n \ "Change" text).toDouble
        )
      ).toList
    })
    implicit val timeout = Timeout(10 seconds)
    Await.result(results, timeout.duration)
  }

/*  def main(args :Array[String]) :Unit = {
    implicit val system = ActorSystem()
    movers(SNP500Constituents(List("MSFT", "BLK")))
      .foreach(println(_))
  }*/

}
