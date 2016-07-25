package com.beepcast.model.reservedCode.internal;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.beepcast.model.reservedCode.ReservedCodeBean;
import com.beepcast.model.user.UserBean;
import com.beepcast.model.user.UserService;
import com.beepcast.util.NamingService;
import com.beepcast.util.Util;

public class InternalCodeProcessor {

  private ReservedCodeBean reservedCode;
  private boolean validUser;

  public InternalCodeProcessor( ReservedCodeBean reservedCode ) {

    this.reservedCode = reservedCode;

    UserService userService = new UserService();

    UserBean userBean = userService
        .selectBasedOnPhone( reservedCode.getPhone() );

    // validate role
    String[] roles = userBean.getRoles();
    if ( roles != null ) {
      for ( int i = 0 ; i < roles.length ; i++ ) {
        String role = roles[i];
        if ( role.equals( "BEEP_ADMIN" ) || role.equals( "SUPER" ) ) {
          validUser = true;
          break;
        }
      }
    }

  } // constructor()

  public String getIPAddress() throws IOException {

    // validate role
    if ( !validUser )
      return "";

    String ipAddress = Util.getIPAddress();
    if ( ipAddress == null )
      throw new IOException( "Unable to detect host IP address." );

    return ipAddress;

  } // getIPAddress()

  public String getMTDRowCount() throws IOException {

    // validate role
    if ( !validUser )
      return "";

    // TransactionLogSupport tls = new TransactionLogSupport();

    /*-------------------------
      set current month
    -------------------------*/
    java.util.Date now = new java.util.Date();
    Calendar c = new GregorianCalendar();
    c.setTime( now );
    c.set( Calendar.DAY_OF_MONTH , 1 );
    c.set( Calendar.HOUR_OF_DAY , 0 );
    c.set( Calendar.MINUTE , 0 );
    c.set( Calendar.SECOND , 0 );
    java.util.Date fromDate = c.getTime();
    c.add( Calendar.MONTH , 1 );
    c.add( Calendar.SECOND , -1 );
    java.util.Date toDate = c.getTime();

    /*--------------------------
      get total phone count
    --------------------------*/
    int totalCount = 0; // tls.getRowCount( 0 , 0 , fromDate , toDate , null ,
    // false );

    /*--------------------------
      get "BEEPCAST" phone count
    --------------------------*/
    int beepcastCount = 0;
    /*
     * ClientBean client = new ClientBean().selectBasedOnCompanyName( "BEEPCAST"
     * ); if ( client != null ) { beepcastCount = tls.getRowCount(
     * client.getClientID() , 0 , fromDate , toDate , null , true );
     * beepcastCount += tls.getRowCount( client.getClientID() , 0 , fromDate ,
     * toDate , null , false ); }
     */

    /*--------------------------
      get "Demo Client" phone count
    --------------------------*/
    int demoClientCount = 0;
    /*
     * client = new ClientBean().selectBasedOnCompanyName( "Demo Client" ); if (
     * client != null ) { demoClientCount = tls.getRowCount(
     * client.getClientID() , 0 , fromDate , toDate , null , true );
     * demoClientCount += tls.getRowCount( client.getClientID() , 0 , fromDate ,
     * toDate , null , false ); }
     */

    /*--------------------------
      return non-beepcast phone count
    --------------------------*/
    return "MTD hits = " + ( totalCount - ( beepcastCount + demoClientCount ) );

  } // getMTDRowCount()

  public String getMTDRevenue() throws IOException {

    // validate role
    if ( !validUser )
      return "";

    return "under construction";

  } // getMTDRevenue()

  public String getCommzgateBalance() throws IOException {

    String response = "";

    /*-----------------------------
      get application scope attributes
    -----------------------------*/
    NamingService nameSvc = NamingService.getInstance();
    String checkBalanceLink = (String) nameSvc
        .getAttribute( "commzgateCheckBalanceLink" );
    String checkBalanceUrl = (String) nameSvc
        .getAttribute( "commzgateCheckBalanceUrl" );
    String loginID = (String) nameSvc.getAttribute( "commzgateLoginID" );
    String password = (String) nameSvc.getAttribute( "commzgatePassword" );

    try {
      /*---------------------------
        connect to commzgate link
      ---------------------------*/
      URL url = new URL( checkBalanceLink );
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setRequestMethod( "POST" );
      httpConn.setDoInput( true );
      httpConn.setDoOutput( true );
      httpConn.setUseCaches( false );

      /*---------------------------
        encode the request parameters
      ---------------------------*/
      String parameters = "checkBalanceUrl="
          + URLEncoder.encode( checkBalanceUrl ) + "&" + "loginID="
          + URLEncoder.encode( loginID ) + "&" + "password="
          + URLEncoder.encode( password );

      /*---------------------------
        send the request
      ---------------------------*/
      DataOutputStream os = new DataOutputStream( httpConn.getOutputStream() );
      os.writeBytes( parameters );
      os.flush();
      os.close();

      /*---------------------------
        get response from web server
      ---------------------------*/
      BufferedReader in = new BufferedReader( new InputStreamReader(
          httpConn.getInputStream() ) );
      String line = "";
      while ( ( line = in.readLine() ) != null ) {
        response += line + "\n";
      }
      if ( response.length() > 0 )
        response = response.substring( 0 , response.length() - 1 );
      in.close();
      response = response.trim();

      /*---------------------------
        handle exceptions
      ---------------------------*/
    } catch ( Exception e ) {
    }

    /*---------------------------
      check for commzgate error
    ---------------------------*/
    if ( !response.startsWith( "05010," ) )
      return "Commzgate Error: " + response;

    /*---------------------------
      get commzgate balance
    ---------------------------*/
    return "commzgate balance = " + response.substring( 6 );

  } // getCommzgateBalance()

} // eof
