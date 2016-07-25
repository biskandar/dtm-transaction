package com.beepcast.model.transaction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.beepcast.loadmng.LoadManagement;
import com.beepcast.model.gateway.GatewayLogBean;
import com.beepcast.model.gateway.GatewayLogDAO;
import com.beepcast.model.gateway.GatewayLogService;
import com.beepcast.router.RouterApp;
import com.beepcast.router.mt.SendBufferBean;
import com.beepcast.router.mt.SendBufferService;
import com.beepcast.util.NamingService;
import com.beepcast.util.StrTok;
import com.beepcast.util.Util;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class HttpSupport {

  static final DLogContext lctx = new SimpleContext( "HttpSupport" );

  private LoadManagement loadMng = LoadManagement.getInstance();
  private RouterApp routerApp = RouterApp.getInstance();

  public String httpRequest( HttpRequestBean request ) throws IOException {

    String response = "";

    try {

      // connect to web server
      URL url = new URL( "http://" + request.getHost()
          + ":8080/beepadmin/trans_control" );
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setRequestMethod( "POST" );
      httpConn.setDoInput( true );
      httpConn.setDoOutput( true );
      httpConn.setUseCaches( false );

      // encode the request parameters
      String parameters = "phone="
          + URLEncoder.encode( request.getPhone() , "UTF-8" ) + "&"
          + "message=" + URLEncoder.encode( request.getMessage() , "UTF-8" )
          + "&" + "simulation="
          + URLEncoder.encode( request.getSimulation() , "UTF-8" ) + "&"
          + "messageType="
          + URLEncoder.encode( request.getMessageType() , "UTF-8" ) + "&"
          + "provider=" + URLEncoder.encode( request.getProvider() , "UTF-8" )
          + "&" + "command="
          + URLEncoder.encode( request.getCommand() , "UTF-8" ) + "&"
          + "senderID=" + URLEncoder.encode( request.getSenderID() , "UTF-8" ); // Dev0

      System.out.println( "HttpSupport.class-Parameters string: " + parameters ); // Dev0-DEBUG

      // send the request
      DataOutputStream os = new DataOutputStream( httpConn.getOutputStream() );
      os.writeBytes( parameters );
      os.flush();
      os.close();

      // get response from web server
      BufferedReader in = new BufferedReader( new InputStreamReader(
          httpConn.getInputStream() ) );
      String line = "";
      while ( ( line = in.readLine() ) != null ) {
        response += line + "\n";
      }
      if ( response.length() > 0 )
        response = response.substring( 0 , response.length() - 1 );
      in.close();

    } catch ( Exception e ) {
      throw new IOException( "HttpSupport.httpRequest(): " + e.getMessage() );
    }

    // success
    return response;

  } // httpRequest()

  public String getSendQueue( String phone , boolean simulation ,
      String strMessageType , String provider ) throws IOException {

    String response = "";

    // build query criteria

    String criteria = " ( simulation = " + ( ( simulation ) ? 1 : 0 ) + " ) ";
    provider = StringUtils.trimToEmpty( provider );
    if ( !provider.equals( "" ) ) {
      if ( provider.equalsIgnoreCase( "MODEM" ) ) {
        criteria += " AND ( provider like '%MDM%'  ) ";
      } else {
        criteria += " AND ( provider like '" + provider + "%' ) ";
      }
    }
    if ( !strMessageType.equals( "" ) ) {
      criteria += " AND ( message_type = " + strMessageType + " ) ";
    }
    if ( !phone.equals( "" ) ) {
      criteria += " AND ( phone = '" + phone + "' ) ";
    }

    SendBufferService sendBufferService = null;
    try {
      sendBufferService = routerApp.getRouterMTWorker().getSendBufferService();
    } catch ( Exception e ) {
      DLog.warning( lctx , "The router app's send buffer service "
          + "is not ready , " + e );
      return response;
    }

    if ( sendBufferService == null ) {
      DLog.warning( lctx , "Failed to get send queue "
          + ", found null send buffer service" );
      return response;
    }

    List listSendBufferBeans = sendBufferService.selectBeans( criteria , 1 );
    if ( listSendBufferBeans == null ) {
      return response;
    }

    SendBufferBean sendBufferBean = null;

    Iterator iterSendBufferBeans = listSendBufferBeans.iterator();
    if ( iterSendBufferBeans.hasNext() ) {
      sendBufferBean = (SendBufferBean) iterSendBufferBeans.next();
    }

    if ( sendBufferBean == null ) {
      return response;
    }

    // header log

    String headerLog = "[" + sendBufferBean.getMessageId() + "-"
        + sendBufferBean.getProvider() + "-" + sendBufferBean.getPhoneNumber()
        + "] ";

    // delete first

    if ( !sendBufferService.delete( sendBufferBean ) ) {
      DLog.warning( lctx , headerLog + "Failed to create response "
          + ", found failed to delete send record" );
      return response;
    }
    DLog.debug( lctx , headerLog + "Successfully deleted "
        + "a send buffer record " );

    // prepare and verify params

    String messageId = sendBufferBean.getMessageId();
    int intMessageType = sendBufferBean.getMessageType();
    strMessageType = MessageType.messageTypeToString( intMessageType );
    int messageCount = sendBufferBean.getMessageCount();
    int eventId = sendBufferBean.getEventId();
    int channelSessionId = sendBufferBean.getChannelSessionId();
    provider = sendBufferBean.getProvider();
    double debitAmount = sendBufferBean.getDebitAmount();
    Date dateSend = sendBufferBean.getDateSend();
    phone = sendBufferBean.getPhoneNumber();
    String originalNumber = "";
    String messageContent = sendBufferBean.getMessageContent();
    String messageStatus = "SUBMITTED";

    // get countryId
    int countryId = TransactionCountryUtil.getCountryId( phone );

    // log it
    DLog.debug( lctx , headerLog
        + "Insert outgoing message into gateway log table : messageType = "
        + strMessageType + " , messageCount = " + messageCount
        + " , eventId = " + eventId + " , channelSessionId = "
        + channelSessionId + " , provider = " + provider + " , debitAmount = "
        + debitAmount + " , dateSend = " + dateSend + " , phone = " + phone
        + " , countryId = " + countryId + " , messageStatus = " + messageStatus );

    // insert into gateway log table

    GatewayLogService gatewayLogService = new GatewayLogService();

    boolean inserted = gatewayLogService.insertOutgoingMessage( messageId ,
        strMessageType , messageCount , eventId , channelSessionId , provider ,
        debitAmount , dateSend , phone , countryId , originalNumber ,
        messageContent , messageStatus );

    if ( inserted ) {
      DLog.debug( lctx , headerLog + "Successfully insert send record "
          + "into gateway log table" );

      // trap mt traffic
      loadMng.hitMt( provider , 1 );

      // compose response string body
      response += "1:";
      response += phone + "^";
      response += messageContent + "^";
      response += intMessageType + "^";
      response += provider;

      DLog.debug( lctx , headerLog + "Created a body response "
          + StringEscapeUtils.escapeJava( response ) );

    } else {
      DLog.warning( lctx , headerLog + "Failed to insert send record "
          + "into gateway log table" );
    }

    return response;
  }

  public SendBufferBean[] extractSendQueue( String sendQueue ) {

    StrTok st1 = new StrTok( sendQueue );
    int numRecords = Integer.parseInt( st1.nextTok( ":" ).trim() );
    SendBufferBean sendRecords[] = new SendBufferBean[numRecords];
    for ( int i = 0 ; i < numRecords ; i++ ) {
      try {

        String record = st1.nextTok( "~" ).trim();

        StrTok st2 = new StrTok( record , "^" );

        String phoneNumber = st2.nextTok().trim();
        String messageContent = st2.nextTok().trim();
        String senderId = st2.nextTok().trim();
        int messageType = Integer.parseInt( st2.nextTok().trim() );
        int eventId = Integer.parseInt( st2.nextTok().trim() );

        routerApp.sendMtMessage(
            TransactionMessageFactory.generateMessageId( "INT" ) , messageType ,
            1 , messageContent , 1.0 , phoneNumber , null , eventId , 0 ,
            senderId , 0 , 0 , new Date() , null );

      } catch ( Exception e ) {
        DLog.warning( lctx , "Failed to extract send queue , " + e );
      }
    }

    return sendRecords;

  }

  public String getGatewayLogRecords( long lastLogID ) throws IOException {
    StringBuffer response = new StringBuffer();

    try {

      /*--------------------------
        kick start the log viewer
      --------------------------*/
      if ( lastLogID < 1 ) {
        lastLogID = new GatewayLogDAO().getMaxLogID();
        if ( lastLogID >= 2 ) {
          lastLogID -= 2;
        }
      }

      /*--------------------------
        get unread log entries
      --------------------------*/

      String criteria = "log_id > " + lastLogID;
      GatewayLogBean logRecords[] = new GatewayLogService().select( criteria );
      if ( ( logRecords == null ) || ( logRecords.length < 1 ) ) {
        return response.toString();
      }

      int numRecords = 0;
      StringBuffer records = new StringBuffer();
      for ( int i = 0 ; i < logRecords.length ; i++ ) {
        GatewayLogBean logRecord = logRecords[i];
        records.append( logRecord.getLogID() );
        records.append( "^" );
        records.append( logRecord.getProvider() );
        records.append( "^" );
        records.append( logRecord.getShortCode() );
        records.append( "^" );
        records.append( Util.strFormat( logRecord.getDateTm() ,
            "yyyy-mm-dd hh:nn:ss" ) );
        records.append( "^" );
        records.append( logRecord.getMode() );
        records.append( "^" );
        records.append( logRecord.getPhone() );
        records.append( "^" );
        records.append( "(" + logRecord.getEventID() + ")" );
        records.append( "(" + logRecord.getChannelSessionID() + ")" );
        records.append( "(" + logRecord.getStatus() + ")" );
        records.append( "(" + logRecord.getMessageID() + ")" );
        records.append( "(" + logRecord.getExternalStatus() + ")" );
        records.append( "(" + logRecord.getExternalMessageID() + ")" );
        records.append( " " );
        records.append( logRecord.getMessage() );
        records.append( "~" );
        numRecords = numRecords + 1;
      } // end for
      if ( numRecords > 0 ) {
        response.append( numRecords );
        response.append( ":" );
        response.append( records.toString() );
      }

      /*--------------------------
        handle exceptions
      --------------------------*/

    } catch ( Exception e ) {

      throw new IOException( "HttpSupport.getGatewayLogRecords(): "
          + e.getMessage() );

    }

    return response.toString();
  } // getGatewayLogRecords()

  public GatewayLogBean[] extractGatewayLogRecords( String records ) {

    StrTok st1 = new StrTok( records );
    int numRecords = Integer.parseInt( st1.nextTok( ":" ).trim() );
    GatewayLogBean logRecords[] = new GatewayLogBean[numRecords];
    for ( int i = 0 ; i < numRecords ; i++ ) {
      String record = st1.nextTok( "~" ).trim();
      StrTok st2 = new StrTok( record , "^" );
      GatewayLogBean logRecord = new GatewayLogBean();
      logRecord.setLogID( Long.parseLong( st2.nextTok().trim() ) );
      logRecord.setProvider( st2.nextTok().trim() );
      logRecord.setShortCode( st2.nextTok().trim() );
      logRecord.setDateTm( Util.stringToDate( st2.nextTok().trim() ) );
      logRecord.setMode( st2.nextTok().trim() );
      logRecord.setPhone( st2.nextTok().trim() );
      logRecord.setMessage( st2.nextTok().trim() );
      logRecords[i] = logRecord;
    }

    return logRecords;

  } // extractGatewayLogRecords()

  public String setBeepadminIP() throws IOException {

    NamingService nameSvc = NamingService.getInstance();
    String systemConfigPath = (String) nameSvc
        .getAttribute( "systemConfigPath" );
    String response = "";

    try {
      /*---------------------------
        get system mode
      ---------------------------*/
      String mode = Util.readINI( systemConfigPath + "system.ini" , "mode" ,
          "mode" );
      if ( mode.equalsIgnoreCase( "standby" ) )
        return "OK (standby)";

      /*---------------------------
        get system ip address
      ---------------------------*/
      String ipAddress = Util.getIPAddress();
      // String ipconfigUrl =
      // Util.readINI(systemConfigPath+"system.ini","beepadminLink","ipconfigUrl");
      String webvisionsBaseUrl = Util.readINI( systemConfigPath
          + "provider.ini" , "beepadminLink" , "webvisionsBaseUrl" );
      String setipconfigUrl = webvisionsBaseUrl + "setipconfig.jsp";

      /*---------------------------
        connect to hosting server
      ---------------------------*/
      URL url = new URL( setipconfigUrl );
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setRequestMethod( "POST" );
      httpConn.setDoInput( true );
      httpConn.setDoOutput( true );
      httpConn.setUseCaches( false );

      /*---------------------------
        encode the request parameters
      ---------------------------*/
      String parameters = "ipAddress="
          + URLEncoder.encode( ipAddress , "UTF-8" )
          + "&"
          + "pageToModify="
          + URLEncoder.encode( Util.readINI( systemConfigPath + "system.ini" ,
              "beepadminLink" , "pageToModify" ) , "UTF-8" )
          + "&"
          + "ipAddressFile="
          + URLEncoder.encode( Util.readINI( systemConfigPath + "system.ini" ,
              "beepadminLink" , "ipAddressFile" ) , "UTF-8" );

      /*---------------------------
        send the request
      ---------------------------*/
      DataOutputStream os = new DataOutputStream( httpConn.getOutputStream() );
      os.writeBytes( parameters );
      os.flush();
      os.close();

      /*---------------------------
        get response from hosting server
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

      /*---------------------------
        handle exceptions
      ---------------------------*/
    } catch ( Exception e ) {
      throw new IOException( "HttpSupport.setBeepadminIP(): " + e.getMessage() );
    }

    // success
    return response;

  } // setBeepadminIP()

  public String getBeepadminIP() throws IOException {

    String response = "";

    try {

      // connect to webvisions server
      URL url = new URL( "http://wwwtest.beepcast.com/getip/getipconfig.jsp" );
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setRequestMethod( "POST" );
      httpConn.setDoInput( true );
      httpConn.setUseCaches( false );

      // get response
      BufferedReader in = new BufferedReader( new InputStreamReader(
          httpConn.getInputStream() ) );
      String line = "";
      while ( ( line = in.readLine() ) != null ) {
        response += line + "\n";
      }
      if ( response.length() > 0 )
        response = response.substring( 0 , response.length() - 1 );
      in.close();

    } catch ( Exception e ) {
      throw new IOException( "HttpSupport.getBeepadminIP(): " + e.getMessage() );
    }

    // success
    return response;

  } // getBeepadminIP()

} // eof
