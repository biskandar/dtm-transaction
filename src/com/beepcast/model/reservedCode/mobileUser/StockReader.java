package com.beepcast.model.reservedCode.mobileUser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import com.beepcast.model.reservedCode.ReservedCodeBean;
import com.beepcast.util.StrTok;

/*******************************************************************************
 * Stock Reader.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class StockReader {

  private ReservedCodeBean reservedCode;
  private String params[];

  /*****************************************************************************
   * Constructor.
   * <p>
   ****************************************************************************/
  public StockReader( ReservedCodeBean reservedCode ) {
    this.reservedCode = reservedCode;
    this.params = reservedCode.getParams();
  }

  /*****************************************************************************
   * Gets stock quote from yahoo.
   * <p>
   * 
   * @return Stock name, price and date.
   ****************************************************************************/
  public String getStockQuote() {

    String quote = "";
    String symbol = params[0];
    String quoteFormat = "&f=slc1wop";

    try {
      /*-------------------------
        connect to yahoo server
      -------------------------*/
      URL url = new URL( "http://quote.yahoo.com/d/quotes.csv?" );
      URLConnection httpConn = url.openConnection();
      httpConn.setDoOutput( true );

      /*-------------------------
        send request
      -------------------------*/
      PrintWriter out = new PrintWriter( httpConn.getOutputStream() );
      out.println( "s=" + symbol + quoteFormat );
      out.close();

      /*-------------------------
        get response
      -------------------------*/
      BufferedReader in = new BufferedReader( new InputStreamReader(
          httpConn.getInputStream() ) );
      String line = in.readLine();
      in.close();

      /*-------------------------
        format the response
      -------------------------*/
      StrTok st = new StrTok( line , "\"" );
      st.nextTok();
      String name = st.nextTok();
      st.nextTok();
      String dateTm = st.nextTok( "-" );
      st.nextTok( ">" );
      String price = st.nextTok( "<" );

      quote = "Name: " + name + "\n" + "Price: " + price + "\n" + "Date: "
          + dateTm;

    } catch ( Exception e ) {
    }

    // success
    return quote;
  }

} // eof

