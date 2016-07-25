package com.beepcast.model.exchange;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/*******************************************************************************
 * Exchange dispatcher.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class ExchangeDispatcher {

  private int clientId;
  private ExchangeBean exchange;
  private String params[];
  private String provider;

  /*****************************************************************************
   * Constructor.
   * <p>
   ****************************************************************************/
  public ExchangeDispatcher( int clientId , ExchangeBean exchange ,
      String provider ) {
    this.clientId = clientId;
    this.exchange = exchange;
    this.params = exchange.getParams();
    this.provider = provider;
  }

  /*****************************************************************************
   * Process exchange and return the response string.
   * <p>
   * 
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String getResponse() throws IOException {

    /*--------------------------
      X NEW <nickname>
    --------------------------*/
    if ( params.length == 2 && params[0].equalsIgnoreCase( "NEW" ) )
      return new ExchangeSupport().newNickname( exchange );

    /*--------------------------
      X NEW <nickname> <password>
    --------------------------*/
    else if ( params.length == 3 && params[0].equalsIgnoreCase( "NEW" ) )
      return new ExchangeSupport().newPassword( exchange );

    /*--------------------------
      X E <nickname> <password>
    --------------------------*/
    else if ( params.length == 3 && params[0].equalsIgnoreCase( "E" ) )
      return new ExchangeSupport().editMessage( exchange );

    /*--------------------------
      X D <nickname> <password>
    --------------------------*/
    else if ( params.length == 3 && params[0].equalsIgnoreCase( "D" ) )
      return new ExchangeSupport().deleteExchange( exchange );

    /*--------------------------
      X DELETE <nickname>
    --------------------------*/
    else if ( params.length == 2 && params[0].equalsIgnoreCase( "DELETE" ) )
      return new ExchangeSupport().deleteNickname( exchange );

    /*--------------------------
      X S <nickname> <password>
    --------------------------*/
    else if ( params.length == 3 && params[0].equalsIgnoreCase( "S" ) )
      return new ExchangeSupport().subscribe( exchange );

    /*--------------------------
      X U <nickname> <password>
    --------------------------*/
    else if ( params.length == 3 && params[0].equalsIgnoreCase( "U" ) )
      return new ExchangeSupport().unsubscribe( exchange );

    /*--------------------------
      X B <nickname> <password>
    --------------------------*/
    else if ( params.length == 3 && params[0].equalsIgnoreCase( "B" ) )
      return new ExchangeSupport().broadcast( exchange , provider );

    /*--------------------------
      X L
    --------------------------*/
    else if ( params.length == 1 && params[0].equalsIgnoreCase( "L" ) )
      return new ExchangeSupport().listNicknames( exchange );

    /*--------------------------
      X L <nickname>
    --------------------------*/
    else if ( params.length == 2 && params[0].equalsIgnoreCase( "L" ) )
      return new ExchangeSupport().listPasswords( exchange );

    /*--------------------------
      X N <old nickname> <new nickname>
    --------------------------*/
    else if ( params.length == 3 && params[0].equalsIgnoreCase( "N" ) )
      return new ExchangeSupport().renameNickname( exchange );

    /*--------------------------
      X P <nickname> <old password> <new password>
    --------------------------*/
    else if ( params.length == 4 && params[0].equalsIgnoreCase( "P" ) )
      return new ExchangeSupport().renamePassword( exchange );

    /*--------------------------
      X <nickname> <password> <phone> <phone>
    --------------------------*/
    else if ( params.length >= 3 && StringUtils.isNumeric( params[2] ) )
      return new ExchangeSupport().tellAFriend( clientId , exchange , provider );

    /*--------------------------
      X <nickname> <password>
    --------------------------*/
    else if ( params.length == 2 )
      return new ExchangeSupport().getMessage( exchange );

    /*--------------------------
      unrecognized exchange
    --------------------------*/
    return null;

  } // getResponse()

} // eof
