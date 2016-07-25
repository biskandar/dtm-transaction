package com.beepcast.model.exchange;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.beepcast.model.beepcode.BeepcodeBean;
import com.beepcast.model.beepcode.BeepcodeService;
import com.beepcast.model.event.CatagoryBean;
import com.beepcast.model.event.EventBean;
import com.beepcast.model.event.EventService;
import com.beepcast.model.friend.FriendBean;
import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.mobileUser.MobileUserService;
import com.beepcast.model.mobileUser.MobileUserSupport;
import com.beepcast.model.mobileUser.NicknameBean;
import com.beepcast.model.transaction.BogusRequestService;
import com.beepcast.model.transaction.MessageType;
import com.beepcast.model.transaction.TransactionLogConstanta;
import com.beepcast.model.transaction.TransactionLogService;
import com.beepcast.model.transaction.TransactionMessageFactory;
import com.beepcast.model.transaction.TransactionQueueBean;
import com.beepcast.model.transaction.TransactionQueueService;
import com.beepcast.router.RouterApp;

/*******************************************************************************
 * Exchange support class.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class ExchangeSupport {

  private RouterApp routerApp = null;

  private EventService eventService = null;
  private TransactionQueueService transQueueService = null;
  private TransactionLogService transLogService = null;
  private BogusRequestService bogusReqService = null;

  public ExchangeSupport() {

    routerApp = RouterApp.getInstance();

    eventService = new EventService();
    transQueueService = new TransactionQueueService();
    transLogService = new TransactionLogService();
    bogusReqService = new BogusRequestService();

  }

  /*****************************************************************************
   * Create new nickname.
   * <p>
   * 
   * @param ExchangeBean
   * @return Validation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String newNickname( ExchangeBean exchange ) throws IOException {

    // X NEW <nickname>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();

    /*--------------------------
      validate nickname
    --------------------------*/
    String errMsg = validateNickname( nickname , phone );
    if ( errMsg != null ) {
      closeTransaction( phone );
      return errMsg;
    }

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "Hi " + nickname
        + ". To create a new passcode, reply back with X NEW " + nickname
        + " <passcode>";

  } // newNickname()

  /*****************************************************************************
   * Create new password.
   * <p>
   * 
   * @param ExchangeBean
   * @return Validation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String newPassword( ExchangeBean exchange ) throws IOException {

    // X NEW <nickname> <password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();
    String password = Xparams[2].toUpperCase();

    /*--------------------------
      validate nickname
    --------------------------*/
    String errMsg = validateNickname( nickname , phone );
    if ( errMsg != null ) {
      closeTransaction( phone );
      return errMsg;
    }

    /*--------------------------
      create exchange
    --------------------------*/
    createExchange( nickname , password );

    /*--------------------------
      success
    --------------------------*/
    // return editMessage(exchange);
    closeTransaction( phone );
    return "Hi " + nickname + ". To create content for passcode [" + password
        + "], reply back with X E " + nickname + " " + password;

  } // newPassword()

  /*****************************************************************************
   * Edit message.
   * <p>
   * 
   * @param ExchangeBean
   * @return Instructions.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String editMessage( ExchangeBean exchange ) throws IOException {

    // X E <nickname> <password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();
    String password = Xparams[2].toUpperCase();

    /*--------------------------
      validate exchange
    --------------------------*/
    String errMsg = validateExchange( nickname , password , phone );
    if ( errMsg != null ) {
      closeTransaction( phone );
      return errMsg;
    }

    /*--------------------------
      setup transObj for E mode
    --------------------------*/
    TransactionQueueBean transObj = transQueueService.select( phone );
    if ( transObj == null )
      return null;
    else {
      String params = transObj.getParams();
      if ( params != null && params.length() > 0 )
        params += ","; // setup for append
      params += "NICKNAME=" + nickname + ",";
      params += "PASSWORD=" + password + ",";
      params += "MODE=E";
      transObj.setParams( params );
      transQueueService.update( transObj );
    }

    /*--------------------------
      success
    --------------------------*/
    // return "Hi "+nickname+". Reply back with your new content for passcode
    // ["+password+"]. (Maximum 140 characters.)";
    exchange = new ExchangeBean().select( nickname , password );
    return exchange.getMessage();

  } // editMessage()

  /*****************************************************************************
   * Update message.
   * <p>
   * 
   * @param ExchangeBean
   * @return Validation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String updateMessage( ExchangeBean exchange ) throws IOException {

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String nickname = exchange.getNickname();
    String password = exchange.getPassword();
    String message = exchange.getMessage();

    /*--------------------------
      validate exchange
    --------------------------*/
    String errMsg = validateExchange( nickname , password , phone );
    if ( errMsg != null ) {
      closeTransaction( phone );
      return errMsg;
    }

    /*--------------------------
      validate message
    --------------------------*/
    if ( message.length() > 140 ) {
      message = message.substring( 0 , 140 );
      return "Hi "
          + nickname
          + ". Your content for passcode ["
          + password
          + "] is too long. To try again, reply back with your new content. (Maximum 140 characters.)";
    }

    /*--------------------------
      save content
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , password );
    exchange.setMessage( message );
    exchange.update();

    /*--------------------------
      success
    --------------------------*/
    TransactionQueueBean transObj = transQueueService.select( phone );
    transObj.setParams( "NICKNAME=" + nickname + ",PASSWORD=" + password
        + ",MODE=X" ); // disable "MODE=E"
    transQueueService.update( transObj );
    closeTransaction( phone );
    return "Hi " + nickname + ". Your content for passcode [" + password
        + "] is saved. To view your content, reply back with X " + nickname
        + " " + password;

  } // updateMessage()

  /*****************************************************************************
   * Delete exchange.
   * <p>
   * 
   * @param ExchangeBean
   * @return Instructions.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String deleteExchange( ExchangeBean exchange ) throws IOException {

    // X D <nickname> <password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();
    String password = Xparams[2].toUpperCase();

    /*--------------------------
      validate exchange
    --------------------------*/
    String errMsg = validateExchange( nickname , password , phone );
    if ( errMsg != null ) {
      closeTransaction( phone );
      return errMsg;
    }

    /*--------------------------
      delete exchange
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , password );
    if ( exchange != null )
      exchange.delete();

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "Hi "
        + nickname
        + ". Your passcode ["
        + password
        + "] and content have been deleted. To create a new passcode and content, reply back with X NEW "
        + nickname + " <passcode>";

  } // deleteExchange()

  /*****************************************************************************
   * Delete nickname.
   * <p>
   * 
   * @param ExchangeBean
   * @return Instructions.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String deleteNickname( ExchangeBean exchange ) throws IOException {

    // X DELETE <nickname>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();

    /*--------------------------
      delete nickname
    --------------------------*/
    NicknameBean nicknameBean = new NicknameBean().select( nickname );
    if ( nicknameBean == null || !nicknameBean.getPhone().equals( phone ) ) {
      closeTransaction( phone );
      return null;
    }
    nicknameBean.delete();

    /*--------------------------
      delete exchanges
    --------------------------*/
    Vector v = new ExchangeBean().selectPasswords( nickname );
    for ( int i = 0 ; i < v.size() ; i++ ) {
      String password = (String) v.elementAt( i );
      exchange = new ExchangeBean().select( nickname , password );
      if ( exchange != null )
        exchange.delete();
    }

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "Hi "
        + nickname
        + ". This nickname and all of its Xchange passcodes and content have been deleted. To create a new nickname, reply back with X NEW <nickname>";

  } // deleteNickname()

  /*****************************************************************************
   * Subscribe.
   * <p>
   * 
   * @param ExchangeBean
   * @return Confirmation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String subscribe( ExchangeBean exchange ) throws IOException {

    // X S <nickname> <password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();
    String password = Xparams[2].toUpperCase();

    /*--------------------------
      subscribe
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , password );
    SubscriberBean subscriber = new SubscriberBean();
    subscriber.setPhone( phone );
    subscriber.setExchangeID( exchange.getExchangeID() );
    subscriber.insert();

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "Thank you for subscribing.";

  } // subscribe()

  /*****************************************************************************
   * Unsubscribe.
   * <p>
   * 
   * @param ExchangeBean
   * @return Confirmation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String unsubscribe( ExchangeBean exchange ) throws IOException {

    // X U <nickname> <password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();
    String password = Xparams[2].toUpperCase();

    /*--------------------------
      subscribe
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , password );
    if ( exchange != null ) {
      SubscriberBean subscriber = new SubscriberBean().select( phone ,
          exchange.getExchangeID() );
      if ( subscriber != null )
        subscriber.delete();
    }

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "You have unsubscribed.";

  } // unsubscribe()

  /*****************************************************************************
   * Broadcast.
   * <p>
   * 
   * @param ExchangeBean
   * @param ReceiveBufferBean
   * @return Confirmation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String broadcast( ExchangeBean exchange , String provider )
      throws IOException {

    // X B <nickname> <password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();
    String password = Xparams[2].toUpperCase();

    /*--------------------------
      validate exchange
    --------------------------*/
    String errMsg = validateExchange( nickname , password , phone );
    if ( errMsg != null ) {
      closeTransaction( phone );
      return errMsg;
    }

    /*--------------------------
      get message content
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , password );
    if ( exchange == null ) {
      closeTransaction( phone );
      return null;
    }
    String message = exchange.getMessage();
    String sponsorCode = getSponsorCode( phone );
    message += "\n<Reply " + sponsorCode + ">";

    /*--------------------------
      broadcast to subscribers
    --------------------------*/
    Vector vPhones = new SubscriberBean().select( exchange.getExchangeID() );
    for ( int i = 0 ; i < vPhones.size() ; i++ ) {
      String subscriberPhone = (String) vPhones.elementAt( i );
      routerApp.sendMtMessage(
          TransactionMessageFactory.generateMessageId( "INT" ) ,
          MessageType.TEXT_TYPE , 1 , message , 1.0 , subscriberPhone ,
          provider , 0 , 0 , null , 0 , 0 , new Date() , null );
    }

    /*--------------------------
      increment message count
    --------------------------*/
    TransactionQueueBean transObj = transQueueService.select( phone );
    transObj.setMessageCount( transObj.getMessageCount() + vPhones.size() );
    transQueueService.update( transObj );

    /*--------------------------
      success
    --------------------------*/
    updateHits( nickname , password , sponsorCode );
    closeTransaction( phone );
    return "Thanks for spreading the word.";

  } // broadcast()

  /*****************************************************************************
   * List nicknames for a given phone.
   * <p>
   * 
   * @param ExchangeBean
   * @return List of nicknames.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String listNicknames( ExchangeBean exchange ) throws IOException {

    // X L

    String nicknames = "";

    /*--------------------------
      get nicknames
    --------------------------*/
    String phone = exchange.getPhone();
    Vector v = new NicknameBean().selectNicknames( phone );
    for ( int i = 0 ; i < v.size() ; i++ )
      nicknames += (String) v.elementAt( i ) + "\n";

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    String response = "";
    if ( !nicknames.equals( "" ) ) {
      if ( v.size() == 1 )
        response = "Your nickname is:\n" + nicknames;
      else
        response = "Your nicknames are:\n" + nicknames;
    }
    return response;

  } // listNicknames()

  /*****************************************************************************
   * List passwords for a given nickname.
   * <p>
   * 
   * @param ExchangeBean
   * @return List of password.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String listPasswords( ExchangeBean exchange ) throws IOException {

    // X L <nickname>

    String passwords = "";

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();

    /*--------------------------
      validate nickname owner
    --------------------------*/
    NicknameBean nicknameBean = new NicknameBean().select( nickname );
    if ( nicknameBean == null || !nicknameBean.getPhone().equals( phone ) ) {
      closeTransaction( phone );
      return null;
    }

    /*--------------------------
      get passwords
    --------------------------*/
    Vector v = new ExchangeBean().selectPasswords( nickname );
    for ( int i = 0 ; i < v.size() ; i++ )
      passwords += (String) v.elementAt( i ) + "\n";

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "The passcodes for [" + nickname + "] are:\n" + passwords;

  } // listPasswords()

  /*****************************************************************************
   * Rename nickname.
   * <p>
   * 
   * @param ExchangeBean
   * @return Confirmation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String renameNickname( ExchangeBean exchange ) throws IOException {

    // X N <old nickname> <new nickname>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String oldNickname = Xparams[1].toUpperCase();
    String newNickname = Xparams[2].toUpperCase();

    /*--------------------------
      validate old nickname
    --------------------------*/
    NicknameBean nicknameBean = new NicknameBean().select( oldNickname );
    if ( nicknameBean == null || !nicknameBean.getPhone().equals( phone ) ) {
      closeTransaction( phone );
      return null;
    }

    /*--------------------------
      validate new nickname
    --------------------------*/
    nicknameBean = new NicknameBean().select( newNickname );
    if ( nicknameBean != null ) {
      String suggestedNickname = new MobileUserSupport( null )
          .suggestedNickname( newNickname );
      if ( suggestedNickname == null )
        return "Nickname [" + newNickname
            + "] already used. To try again, reply back with X N "
            + oldNickname + " <new nickname>";
      else
        return "Nickname [" + newNickname
            + "] already used. To try again, reply back with X N "
            + oldNickname + " " + suggestedNickname;
    }

    /*--------------------------
      rename in nickname table
    --------------------------*/
    nicknameBean = new NicknameBean().select( oldNickname );
    nicknameBean.setNickname( newNickname );
    nicknameBean.update();

    /*--------------------------
      rename in exchange table
    --------------------------*/
    Vector v = new ExchangeBean().selectPasswords( oldNickname );
    for ( int i = 0 ; i < v.size() ; i++ ) {
      String password = (String) v.elementAt( i );
      exchange = new ExchangeBean().select( oldNickname , password );
      if ( exchange != null ) {
        exchange.setNickname( newNickname );
        exchange.update();
      }
    }

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "Done. Your old nickname [" + oldNickname
        + "] has been renamed to [" + newNickname + "].";

  } // renameNickname()

  /*****************************************************************************
   * Rename password.
   * <p>
   * 
   * @param ExchangeBean
   * @return Confirmation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String renamePassword( ExchangeBean exchange ) throws IOException {

    // X P <nickname> <old password> <new password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[1].toUpperCase();
    String oldPassword = Xparams[2].toUpperCase();
    String newPassword = Xparams[3].toUpperCase();

    /*--------------------------
      validate nickname
    --------------------------*/
    String errMsg = validateNickname( nickname , phone );
    if ( errMsg != null )
      return errMsg;

    /*--------------------------
      validate old password
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , oldPassword );
    if ( exchange == null ) {
      closeTransaction( phone );
      return "Hi " + nickname + ". You do not have a passcode [" + oldPassword
          + "]. To create a new passcode, reply back with X NEW " + nickname
          + " <passcode>";
    }

    /*--------------------------
      validate new password
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , newPassword );
    if ( exchange != null ) {
      closeTransaction( phone );
      return "Hi " + nickname + ". You already have a passcode [" + newPassword
          + "]. To try again, reply back with X P " + nickname + " "
          + oldPassword + " <new passcode>";
    }

    /*--------------------------
      rename password
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , oldPassword );
    exchange.setPassword( newPassword );
    exchange.update();

    /*--------------------------
      success
    --------------------------*/
    closeTransaction( phone );
    return "Hi " + nickname + ". Your passcode [" + oldPassword
        + "] has been renamed to [" + newPassword + "].";

  } // renamePassword()

  /*****************************************************************************
   * Tell a friend.
   * <p>
   * 
   * @param ExchangeBean
   * @param ReceiveBufferBean
   * @return Confirmation message.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String tellAFriend( int clientId , ExchangeBean exchange ,
      String provider ) throws IOException {

    // X <nickname> <password> <phone> <phone>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[0].toUpperCase();
    String password = Xparams[1].toUpperCase();

    /*--------------------------
      get message content
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , password );
    if ( exchange == null ) {
      closeTransaction( phone );
      return null;
    }
    String message = exchange.getMessage();
    String sponsorCode = getSponsorCode( phone );
    message += "\n<Reply " + sponsorCode + ">";

    /*--------------------------
      for each friend...
    --------------------------*/
    for ( int i = 2 ; i < Xparams.length ; i++ ) {
      String friendPhone = Xparams[i];
      int prefixLength = phone.length() - friendPhone.length();
      if ( prefixLength > 0 )
        friendPhone = phone.substring( 0 , prefixLength ) + friendPhone;
      if ( StringUtils.isNumeric( friendPhone ) ) {

        /*--------------------------
          create friend
        --------------------------*/
        MobileUserService mobileUserService = new MobileUserService();
        MobileUserBean mobileUserBean = mobileUserService.select( clientId ,
            friendPhone );
        if ( mobileUserBean == null ) {
          mobileUserBean = new MobileUserSupport( null ).createMobileUser(
              clientId , friendPhone , null , null );
          // transObj.setNewUser(true);
        }

        /*--------------------------
          send message to friend
        --------------------------*/
        routerApp.sendMtMessage(
            TransactionMessageFactory.generateMessageId( "INT" ) ,
            MessageType.TEXT_TYPE , 1 , message , 1.0 , friendPhone , provider ,
            0 , 0 , null , 0 , 0 , new Date() , null );

        /*--------------------------
          connect sender to friend
        --------------------------*/
        FriendBean friend = new FriendBean();
        friend.setSenderPhone( phone );
        friend.setFriendPhone( friendPhone );
        friend.setExchangeID( exchange.getExchangeID() );
        friend.insert();

        /*--------------------------
          increment message count
        --------------------------*/
        TransactionQueueBean transObj = transQueueService.select( phone );
        transObj.setMessageCount( transObj.getMessageCount() + 1 );
        transQueueService.update( transObj );
      }
    }

    /*--------------------------
      success
    --------------------------*/
    updateHits( nickname , password , sponsorCode );
    closeTransaction( phone );
    return "Thanks for spreading the word.";

  } // tellAFriend()

  /*****************************************************************************
   * Get message.
   * <p>
   * 
   * @param ExchangeBean
   * @return Message content.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String getMessage( ExchangeBean exchange ) throws IOException {

    // X <nickname> <password>

    /*--------------------------
      get params
    --------------------------*/
    String phone = exchange.getPhone();
    String Xparams[] = exchange.getParams();
    String nickname = Xparams[0].toUpperCase();
    String password = Xparams[1].toUpperCase();

    /*--------------------------
      get message content
    --------------------------*/
    exchange = new ExchangeBean().select( nickname , password );
    if ( exchange == null ) {
      closeTransaction( phone );
      return null;
    }
    String message = exchange.getMessage();
    if ( message.equals( ExchangeBean.DEFAULT_MESSAGE ) ) {
      closeTransaction( phone );
      return null;
    }
    String sponsorCode = getSponsorCode( phone );
    message += "\n<Reply " + sponsorCode + ">";

    /*--------------------------
      success
    --------------------------*/
    updateHits( nickname , password , sponsorCode );
    closeTransaction( phone );
    return message;

  } // getMessage()

  /*****************************************************************************
   * Validate exchange.
   * <p>
   * 
   * @param nickname
   * @param password
   * @param phone
   * @return Error message if invalid, null if valid.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String validateExchange( String nickname , String password ,
      String phone ) throws IOException {

    /*--------------------------
      validate nickname
    --------------------------*/
    String errMsg = validateNickname( nickname , phone );
    if ( errMsg != null )
      return errMsg;

    /*--------------------------
      create exchange if it does not exist
    --------------------------*/
    createExchange( nickname , password );
    return null;

  } // validateExchange()

  /*****************************************************************************
   * Validate nickname.
   * <p>
   * 
   * @param nickname
   * @param phone
   * @return Error message if invalid, null if valid.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String validateNickname( String nickname , String phone )
      throws IOException {

    NicknameBean nicknameBean = new NicknameBean().select( nickname );

    /*------------------------
      new nickname never been used
    ------------------------*/
    if ( nicknameBean == null ) {
      nicknameBean = new NicknameBean();
      nicknameBean.setNickname( nickname );
      nicknameBean.setPhone( phone );
      nicknameBean.setLastHitDate( new Date() );
      nicknameBean.insert();
      return null;
    }

    /*------------------------
      phone already owns this nickname
    ------------------------*/
    else if ( nicknameBean.getPhone().equals( phone ) )
      return null;

    /*------------------------
      suggest a similar nickname
    ------------------------*/
    else {
      String suggestedNickname = new MobileUserSupport( null )
          .suggestedNickname( nickname );
      if ( suggestedNickname == null )
        return "Nickname [" + nickname
            + "] already used. To try again, reply back with X NEW <nickname>";
      else
        return "Nickname [" + nickname
            + "] already used. To try again, reply back with X NEW "
            + suggestedNickname;
    }

  } // validateNickname()

  /*****************************************************************************
   * Create exchange.
   * <p>
   * 
   * @param nickname
   * @param password
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void createExchange( String nickname , String password )
      throws IOException {

    ExchangeBean exchange = new ExchangeBean().select( nickname , password );
    if ( exchange == null ) {
      exchange = new ExchangeBean();
      exchange.setNickname( nickname );
      exchange.setPassword( password );
      exchange.setLastHitDate( new Date() );
      exchange.insert();
    }

  } // createExchange()

  /*****************************************************************************
   * Get sponsor.
   * <p>
   * 
   * @return Random sponsor prime code, or null if no sponsors.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String getSponsorCode( String phone ) throws IOException {

    /*------------------------
      setup sponsor
    ------------------------*/
    String sponsorCode = randomSponsorCode();
    createSponsorEvent( sponsorCode );

    /*--------------------------
      charge sponsor
    --------------------------*/
    BeepcodeBean beepcode = new BeepcodeService().select( sponsorCode );
    long clientID = beepcode.getClientID();
    EventBean _event = eventService.select( "X Sponsor" , clientID );
    TransactionQueueBean transObj = transQueueService.select( phone );
    if ( transObj == null ) {
      transObj = new TransactionQueueBean();
      transObj.setPhone( phone );
      transObj.setDateTm( new Date() );
      transQueueService.insert( transObj );
      transObj = transQueueService.select( phone );
    }
    transObj.setEventID( _event.getEventID() );
    transObj.setClientID( _event.getClientID() );
    transObj.setCode( _event.getCodes() );
    transQueueService.update( transObj );

    // success
    return sponsorCode;

  } // getSponsorCode()

  /*****************************************************************************
   * Get random sponsor prime code.
   * <p>
   * 
   * @return Random sponsor prime code, or null if no sponsors.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public String randomSponsorCode() throws IOException {

    Vector vCodes = new SponsorsBean().getCodes();
    if ( vCodes.size() == 0 )
      return "BEEPME";
    int randomIndex = (int) ( Math.random() * vCodes.size() );
    return (String) vCodes.elementAt( randomIndex );

  } // randomSponsorCode()

  /*****************************************************************************
   * Create sponsor event.
   * <p>
   * 
   * @param sponsorCode
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void createSponsorEvent( String sponsorCode ) throws IOException {

    /*-----------------------
      sponsor event exists?
    -----------------------*/
    BeepcodeBean beepcode = new BeepcodeService().select( sponsorCode );
    long clientID = beepcode.getClientID();
    EventBean _event = eventService.select( "X Sponsor" , clientID );
    if ( _event != null )
      return;

    /*-----------------------
      setup event elements
    -----------------------*/
    Date startDate = new Date();
    Calendar c = new GregorianCalendar();
    c.setTime( startDate );
    c.add( Calendar.YEAR , 10 );
    Date endDate = c.getTime();

    String code = "X_" + clientID;
    long catagoryID = new CatagoryBean().select( "EXCHANGE" ).getCatagoryID();

    /*-----------------------
      create event
    -----------------------*/
    _event = new EventBean();
    _event.setEventName( "X Sponsor" );
    _event.setClientID( clientID );
    _event.setCatagoryID( catagoryID );
    _event.setCodes( code );
    _event.setNumCodes( 1 );
    _event.setCodeLength( code.length() );
    _event.setStartDate( startDate );
    _event.setEndDate( endDate );
    _event.setRemindDate( startDate );
    _event.setRemindFreq( "NEVER" );
    _event
        .setComment( "DO NOT DELETE THIS EVENT.  This event is used to charge exchange sponsors." );
    _event.setProcessType( EventBean.BASIC_TYPE );
    _event.setProcess( "1:1^CODE^(N/A)^(N/A)^^END^" );
    long eventID = eventService.insert( _event );

    /*-----------------------
      create beepcode
    -----------------------*/
    beepcode = new BeepcodeBean();
    beepcode.setCode( code );
    beepcode.setCodeLength( code.length() );
    beepcode.setEventID( eventID );
    beepcode.setClientID( clientID );
    beepcode.setCatagoryID( catagoryID );
    beepcode.setActive( true );
    beepcode.setReserved( true );
    beepcode.setLastHitDate( new Date() );
    new BeepcodeService().insert( beepcode );

  } // createSponsorEvent()

  /*****************************************************************************
   * Close transaction.
   * <p>
   * 
   * @param phone
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void closeTransaction( String phone ) throws IOException {

    TransactionQueueBean transObj = transQueueService.select( phone );
    if ( transObj != null ) {
      transLogService.logTransaction( transObj ,
          TransactionLogConstanta.CLOSED_REASON_NORMAL );
      transQueueService.delete( transObj );
    }

  } // closeTransaction()

  /*****************************************************************************
   * Update nickname hits.
   * <p>
   * 
   * @param nickname
   * @param password
   * @param sponsorCode
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void updateHits( String nickname , String password , String sponsorCode )
      throws IOException {

    /*--------------------------
      update nickname hits
    --------------------------*/
    NicknameBean nicknameBean = new NicknameBean().select( nickname );
    if ( nicknameBean != null ) {
      nicknameBean.setHitCount( nicknameBean.getHitCount() + 1 );
      nicknameBean.setLastHitDate( new Date() );
      nicknameBean.update();
    }

    /*--------------------------
      update exchange hits
    --------------------------*/
    ExchangeBean exchange = new ExchangeBean().select( nickname , password );
    if ( exchange != null ) {
      exchange.setHitCount( exchange.getHitCount() + 1 );
      exchange.setLastHitDate( new Date() );
      exchange.update();
    }

    /*--------------------------
      update sponsor hits
    --------------------------*/
    SponsorBean sponsor = new SponsorBean().select( sponsorCode );
    if ( sponsor != null ) {
      sponsor.setHitCount( sponsor.getHitCount() + 1 );
      sponsor.setLastHitDate( new Date() );
      sponsor.update();
    }

  } // updateHits()

} // eof
