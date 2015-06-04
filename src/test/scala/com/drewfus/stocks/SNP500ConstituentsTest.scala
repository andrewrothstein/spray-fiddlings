package com.drewfus.stocks

import java.io.{BufferedReader, InputStreamReader}

import org.apache.commons.io.IOUtils
import org.scalatest.{Matchers, FlatSpec}


/**
 * Created by drew on 5/31/15.
 */
class SNP500ConstituentsTest extends FlatSpec with Matchers {

  "the SNP500" should "have 502 constituents" in {
    val constituents = SNP500Constituents.parseTickers(
      IOUtils.toString(getClass.getResourceAsStream("snp500.html"))
    )
    constituents.tickers.size should be (502)
  }

}
