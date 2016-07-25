# dtm-transaction
Directtomobile Transaction Module

v1.1.40

- There is a change in file ./conf/baGlobal.properties

    platform.qr-generator.base=http://localhost:8080/admin/servlet/QRCodeGenerator
    platform.qr-generator.size=177

- Add new reserved variable to display qr image link

- Keep track to use client subscriber profile from transaction input message
  to keep persistent until the outgoing message

- Put group subscriber bean in the transaction persist list method

- Add feature to resolve xipme reserved keyword for subscribe list script

- Add new event step : RANDOM_CODE with VAR as variable session name
  with the following response parameters below :
  * code.format = 'alphanumeric' ;
  * code.length = 6 ;
  
- Add new library :
  . router2beepcast-v1.0.44.jar
  . 
    
v1.1.39

- Found bugs :

    {RouterMOWorkerThread-0}Transaction [MDM20246000] [Step-0] Performing webhook step
    {RouterMOWorkerThread-0}Transaction [MDM20246000] [Step-0] Trying to replace processBean's response with reserved vars
    {RouterMOWorkerThread-0}Transaction [MDM20246000] [Step-0] Trying to replace processBean's response with reserved vars
    {RouterMOWorkerThread-0}Transaction [MDM20246000] Built response from process bean ( 156 chars ) = request.method = 'POST' ;\nrequest.uri = 'http:\/\/ax4.djak.co\/test.html' ;\nrequest.parameters['field1'] = 'value1' ;\nrequest.parameters['field2'] = 'value2' ;
    {RouterMOWorkerThread-0}Transaction [MDM20246000] [Step-0] Resolved script source : request.method = 'POST' ;\nrequest.uri = 'http:\/\/ax4.djak.co\/test.html' ;\nrequest.parameters['field1'] = 'value1' ;\nrequest.parameters['field2'] = 'value2' ;
    {RouterMOWorkerThread-0}ScriptWebhookExec [Step-0] Failed to execute , org.mozilla.javascript.EvaluatorException: Java class "java.util.HashMap" has no public instance field or method named "field1". (ScriptWebhookExec#3)
    {RouterMOWorkerThread-0}Transaction [MDM20246000] [Step-0] Failed to process webhook , found failed to execute the script
    {RouterMOWorkerThread-0}Transaction [MDM20246000] [Step-0] Failed to process webhook , found failed to generate outbound message
    {RouterMOWorkerThread-0}Transaction [MDM20246000] [Step-0] Failed to perform webhook step

- Support to process event step "WEBHOOK" : Set URL in the header , and Post data in the message .
  But treat as javascript like SUBSCRIBE LIST did
  
- Centralized jar libraries

- Add new library :
  . beepcast_session-v1.0.03.jar
  . subscriber2beepcast-v1.2.41.jar
  . util2beepcast-v1.0.01.jar
  . model2beepcast-v1.0.83.jar
  . client2beepcast-v2.3.10.jar
  . msglink2beepcast-v1.0.25.jar
  . channel2beepcast-v1.0.69.jar
  . 

v1.1.38

- Found bug that can not send to modem as provider

    DEBUG   {RouterMOWorkerThread-1}Transaction [MDM20237001] [#3 Attempt] Resolving destination provider from number to provider map
    DEBUG   {RouterMOWorkerThread-1}Transaction [MDM20237001] [#3 Attempt] Resolving destination provider based on : groupConnId = 5 , longNumber = 90102337 , countryCode = SG , prefixNumber = +65983 , listTelcoCodes = []
    DEBUG   {RouterMOWorkerThread-1}Transaction [MDM20237001] [#3 Attempt] Resolved list prohibit provider ids : null
    DEBUG   {RouterMOWorkerThread-1}Transaction [MDM20237001] [#3 Attempt] Resolving provider from the group providers , based on : longNumber = 90102337 , countryCode = SG , listTelcoCodes = [] , groupConnectionId = 5 , listProhibitProviderIds = null , maxLevel = 3
    DEBUG   {RouterMOWorkerThread-1}Transaction [MDM20237001] [#3 Attempt] Resolved list active provider ids : [EE, EE4, EE3, EE2, EE1]

- Support to send sms to modem as provider

- Add feature to override the reserved variable from parameter , like below code

    imsg.addMessageParam(
        TransactionMessageParam.HDR_PREFIX_SET_RESERVED_VARIABLE
            .concat( "USER_NAME" ) , "Benny Iskandar" );
    
    imsg.addMessageParam(
        TransactionMessageParam.HDR_PREFIX_SET_RESERVED_VARIABLE
            .concat( "TMSG_MESSAGE_CONTENT" ) , "Test Message Content" );

- Add new library :
  . beepcast_clientapi-v1.0.05.jar
  . router2beepcast-v1.0.43.jar
  . client2beepcast-v2.3.08.jar
  . provider2beepcast-v2.2.31.jar

v1.1.37

- When found expected event , it will dig out also in the session log

- Found bug when persist session with more than one transaction with same number and same event

    {ChannelProcessorThread-3}Transaction [INT11188141] Define a guest code as a beepcode : code = ADADADQD , clientId = 1 , eventId = 208
    {ChannelProcessorThread-3}Transaction [INT11188141] Added input message param for beepcodeBean : code = ADADADQD , clientId = 1 , eventId = 208
    {ChannelProcessorThread-3}Transaction [INT11188141] Updated beepcode last hit date
    {ChannelProcessorThread-3}Transaction [INT11188141] Added input message param for codeType = beepcode
    {ChannelProcessorThread-3}Transaction [INT11188141] Defined code ADADADQD as beepcode
    {ChannelProcessorThread-3}Transaction [INT11188141] Trying to find current trans queue bean from the trans queue table , based on : phoneNumber = +6500006305
    {ChannelProcessorThread-3}Transaction [INT11188141] Trying to get last session , based on : phoneNumber = +6500006305 , eventId = -1
    {ChannelProcessorThread-3}Transaction [INT11188141] Found a current trans queue bean with : lastAccessDate = 2015-12-02 18:24:48 , lastCode = ADADADQD , eventId = 208
    {ChannelProcessorThread-3}Transaction [INT11188141] Found last session profile : EventId = 208 , Code = ADADADQD , ClientId = 1
    {ChannelProcessorThread-3}Transaction [INT11188141] This message does not contain of exchange event , found the event code is not equal to X
    {ChannelProcessorThread-3}Transaction [INT11188141] Defined beep client id = 1 and beep event id = 208
    {ChannelProcessorThread-3}Transaction [INT11188141] Defined to persist the session
    {ChannelProcessorThread-3}Transaction [INT11188141] Trying to close current session , with CLOSED_REASON_RENEW_EVENT
    {ChannelProcessorThread-3}Transaction [INT11188141] Failed to delete trans queue , but still force to insert into trans log
    {ChannelProcessorThread-3}Transaction [INT11188141] The current session is successfully closed 
    {ChannelProcessorThread-3}Transaction [INT11188141] Created transaction queue bean from factory : eventId = 208 , clientId = 1 , pendingEventId = 0 , dateTm = Wed Dec 02 18:24:48 SGT 2015 , phoneNumber = +6500006305 , providerId =  , nextStep = 1 , messageCount = 0 , code = ADADADQD , params =  , updateProfile = false , newUser = false , jumpCount = 0
    {ChannelProcessorThread-3}Transaction [INT11188141] Failed to create session , found failed insert into trans queue table
    {ChannelProcessorThread-3}Transaction [INT11188141] Failed to create new session , based on params from beepcode
    {ChannelProcessorThread-3}Transaction [INT11188141] Resolved session as message code , took 28 ms
    {ChannelProcessorThread-3}Transaction [INT11188141] Failed to resolve trans queue bean
    {ChannelProcessorThread-3}Transaction [INT11188141] Found empty messageStatus , set default as DELIVERED
    {ChannelProcessorThread-3}Transaction [INT11188141] Inserted an input message into gateway log
    {ChannelProcessorThread-3}Transaction [INT11188141] Converted an input message as bogus message
    {ChannelProcessorThread-3}Transaction [INT11188141] Inserting a new bogus message , phone = +6500006305 , message = ADADADQD
    {ChannelProcessorThread-3}Transaction [INT11188141] Sending total 1 outbound message(s)
    {ChannelProcessorThread-3}Transaction [INT11188141] [OutMsg-1] Processing as invalid message

- Found bug transaction from api send message always come to bogus table

    INFO    {RouterMOWorkerThread-1}Transaction [API20206004] Processing inbound message : messageType = 0 
      , messageCount = 0 , messageContent = ADCK , oriNode = MGW , oriAddress = +6598394294 
      , oriMaskingAddress = null , oriProvider = CLIENT_API , dstNode = null , dstAddress = null 
      , dstMaskingAddress = null , dstProvider = null , eventId = 1 , clientId = 1 , channelSessionId = 0 
      , noProcessResponse = false , replyMessageContent = test from benny iskandar http:\/\/xip.me\/QJNLw 
      , messageParams = {}
    (cut)
    DEBUG   {RouterMOWorkerThread-1}Transaction [API20206004] Created transaction queue bean from 
      factory : eventId = 1 , clientId = 1 , pendingEventId = 0 , dateTm = Tue Dec 01 17:41:01 SGT 2015 
      , phoneNumber = +6598394294 , providerId = CLIENT_API , nextStep = 1 , messageCount = 0 , code = ADCK 
      , params =  , updateProfile = false , newUser = false , jumpCount = 0
    WARNING {RouterMOWorkerThread-1}TransactionQueueDAO Failed to insert record into trans queue table 
      , found phoneNumber = +6598394294 is already exist
    WARNING {RouterMOWorkerThread-1}Transaction [API20206004] Failed to create session 
      , found failed insert into trans queue table
    WARNING {RouterMOWorkerThread-1}Transaction [API20206004] Failed to create new session 
      , based on params from beepcode
    DEBUG   {RouterMOWorkerThread-1}Transaction [API20206004] Resolved session as message code 
      , took 13 ms
    WARNING {RouterMOWorkerThread-1}Transaction [API20206004] Failed to resolve trans queue bean

- Add new library :
  . 

v1.1.36

- Found bug that always failed to process message when found "$" symbol in the inbound message

    http://stackoverflow.com/questions/14152811/java-string-replaceall-and-replacefirst-fails-at-symbol-at-replacement-text
    
    Exception in thread "main" java.lang.IllegalArgumentException: Illegal group reference
      at java.util.regex.Matcher.appendReplacement(Matcher.java:808)
      at com.beepcast.model.event.EventTransQueueReservedVariables.replaceReservedVariables(EventTransQueueReservedVariables.java:209)
      at com.beepcast.model.transaction.test.TestEventTransQueueReservedVariables02.main(TestEventTransQueueReservedVariables02.java:185)
      
    Note that backslashes (\) and dollar signs ($) in the replacement string may cause the 
    results to be different than if it were being treated as a literal replacement string; 
    see Matcher.replaceAll. Use Matcher.quoteReplacement(java.lang.String) to suppress the 
    special meaning of these characters, if desired.
    
    http://www.bennadel.com/blog/1826-java-matcher-s-quotereplacement-and-java-6-vs-java-1-4-2.htm
    
    Including a dollar sign in the replacement string
    To actually include a dollar in the replacement string, 
    we need to put a backslash before the dollar symbol:
    str = str.replaceAll("USD", "\\$");
    The static method Matcher.quoteReplacement() will replace instances of dollar signs 
    and backslashes in a given string with the correct form to allow 
    them to be used as literal replacements:
    str = str.replaceAll("USD",Matcher.quoteReplacement("$"));    
      
- Add new library :
  . channel2beepcast-v1.0.67.jar
  . beepcast_loadmanagement-v1.2.05.jar
  . 

v1.1.35

- Found a bug none deductable for delivered message 

    15.11.2015 09.53.28:004 353144 DEBUG   {RouterMOWorkerThread-1}Transaction [MDM26577020] 
      Trying to perform debit at MT Leg 
      , total debit amount = 1.0 unit(s) defined , based on : provider.id = EE4 , provider.creditCost = 1.0 
      , country.name = Singapore , country.creditCost = 1.0 , and total = 1 msg(s)
    15.11.2015 09.53.28:004 353145 DEBUG   {RouterMOWorkerThread-1}Transaction [MDM26577020] 
      Found client level name = Tier 1 (1 to 10K)
    15.11.2015 09.53.28:004 353146 DEBUG   {RouterMOWorkerThread-1}Transaction [MDM26577020] 
      Found client payment type = POSTPAID
    15.11.2015 09.53.28:004 353147 DEBUG   {RouterMOWorkerThread-1}Transaction [MDM26577020] 
      Succeed to perform client debit , defined balance before = 1463916.96 unit(s) 
      , balance after = 1463917.96 unit(s)
    15.11.2015 09.53.28:005 353148 DEBUG   {RouterMOWorkerThread-1}Transaction [MDM26577020] 
      [OutMsg-1] Successfully perform debit at MT Leg
    15.11.2015 09.53.28:005 353149 DEBUG   {RouterMOWorkerThread-1}Transaction [MDM26577020] 
      Prepared total 2 param(s) from outgoing message before go to send buffer : {oriProvider=STARHUBMDM01, 
        gatewayXipmeId=MDM26577020}
    15.11.2015 09.53.28:005 353150 DEBUG   {RouterMOWorkerThread-1}RouterMTWorker [RouterMessage-MDM26577020] 
      Send Mt Message : messageId = MDM26577020 , gatewayMessageType = 0 , messageCount = 1 
      , messageContent = Test Incoming Basic Provider Syniverse 4E0 - Testee4 , debitAmount = 0.0  <<-- THIS IS THE BUG
      , phoneNumber = +6596328785 , providerId = EE4 , eventId = 2960 , channelSessionId = 0 
      , senderId = +6591093429 , priority = 5 , retry = 0 , dateSend = null 
      , mapParams = {oriProvider=STARHUBMDM01, gatewayXipmeId=MDM26577020}

- Add new library :
  . subscriber2beepcast-v1.2.38.jar

v1.1.34

- Please clean this log :

    WARNING {ChannelProcessorThread-9}XipmeTranslator [INT9134634] 
      Processed gateway xipme bean : gatewayXipmeId = CLG13572618 , xipmeMasterCode = 3VcpP 
      , xipmeCode = 3EPhj , xipmeCodeEncrypted = 3EPhj , result = true

- For the next step ( after first step ) , always have new unique message id ,
  because new message id will link to gateway_xipme transaction .

- New process step "Create QR Image"

- Use database server MySQL 5.6 Port 3307

- Add Message Type : QR_PNG , QR_GIF , QR_JPG

- Use jdk 1.7

- Add new library :

  . stax-api-1.0.1.jar
  . stax-1.2.0.jar
  . jettison-1.2.jar
  . xpp3_min-1.1.4c.jar
  . jdom-1.1.3.jar
  . dom4j-1.6.1.jar
  . xom-1.1.jar
  . cglib-nodep-2.2.jar
  . xmlpull-1.1.3.1.jar
  . xstream-1.4.8.jar

  . commons-logging-1.2.jar
  . commons-pool2-2.4.2.jar
  . commons-dbcp2-2.1.1.jar
  . mysql-connector-java-5.1.35-bin.jar
  . beepcast_database-v1.2.00.jar
  
  . beepcast_clientapi-v1.0.04.jar
  . xipme_api-v1.0.29.jar
  . model2beepcast-v1.0.82.jar
  . router2beepcast-v1.0.40.jar
  . lookup2beepcast-v1.0.00.jar
  . provider2beepcast-v2.2.28.jar
  
v1.1.33

- Change at xipme action update at XipmeTranslator class

      listActions.add( ActionFactory.createActionUpdateIfBlank(
          Action.TBLNM_MAP_TO_DTM , Action.FLDNM_SENDERID ,
          evBean.getOutgoingNumber() ) );
      listActions.add( ActionFactory.createActionUpdateIfBlank(
          Action.TBLNM_MAP_TO_DTM , Action.FLDNM_EVENTID ,
          Long.toString( evBean.getEventID() ) ) );

- Change the way to dig out last session if there is event id pre defined 

- Add new library :
  . xipme_api-v1.0.28.jar

v1.1.32

- Include character "_" and "-" as guestCode

- Migrate all the existing projects to use IDE MyEclipse Pro 2014
  . Build as java application ( not as web project )

- Add new library :
  . channel2beepcast-v1.0.66.jar
  . router2beepcast-v1.0.38.jar
  . provider2beepcast-v2.2.26.jar
  . msglink2beepcast-v1.0.22.jar
  . beepcast_unique_code-v1.0.00.jar
  . beepcast_onm-v1.2.09.jar
  . http-v1.0.01.jar
  . 

v1.1.31

- There is a change inside file ./conf/oproperties.xml

      <property field="Transaction.RouteProvider.PrefixNumberLength" value="6"
        description="Length of phone number will take for the provider routing" />

- Added destination provider simulator

- Add new library :
  . beepcast_keyword-v1.0.07.jar
  . model2beepcast-v1.0.80.jar
  . xipme_api-v1.0.27.jar
  . channel2beepcast-v1.0.65.jar
  . provider2beepcast-v2.2.25.jar
  . msglink2beepcast-v1.0.21.jar
  . client_request2beepcast-v1.0.05.jar
  . subscriber2beepcast-v1.2.36.jar
  . dwh_model-v1.0.33.jar
  . beepcast_dbmanager-v1.1.36.jar
  . router2beepcast-v1.0.37.jar
  . http-v1.0.00.jar

v1.1.30

- There is a change inside file ./conf/oproperties.xml

      <property field="Transaction.ProcessStep.EMAIL.addressFrom" value="do-not-reply@directtomobile.com"
        description="Default value for event process step email's address from" />
      <property field="Transaction.ProcessStep.EMAIL_CLIENT.addressFrom" value="do-not-reply@directtomobile.com"
        description="Default value for event process step email_client's address from" />
      <property field="Transaction.ProcessStep.EMAIL_TO.addressFrom" value="do-not-reply@directtomobile.com"
        description="Default value for event process step email_to's address from" />
      <property field="Transaction.ProcessStep.SMS_TO_EMAIL.addressFrom" value="do-not-reply@directtomobile.com"
        description="Default value for event process step sms_to_email's address from" />

- Add new library :
  . 

v1.1.29

- There is a change inside file ./conf/transaction.xml

    <propertyProcessSteps>
      <propertyProcessStep name="EMAIL">
        <property name="addressFrom" value="${transaction.property-process-step.address-from}" />
      </propertyProcessStep>

- Add feature to clean emailSenderAddress

- Add new library :
  . 

v1.1.28

- Found bug didn't store into gateway log table

    {ChannelProcessorThread-4}Transaction [INT23551518] Trying to persist contact list
    {ChannelProcessorThread-4}Transaction [INT23551518] Added input msg params of client subscriber profile , with id = 27391281 , phone = +61417999049
    {ChannelProcessorThread-4}Transaction [INT23551518] Added input msg params of client subscriber custom profile , with id = 15777590 , clientSubscriberId = 27391281
    {ChannelProcessorThread-4}Transaction [INT23551518] Reject message , found can't find match client to country profile

- Add function string to int for transaction message type

- Put more validation on resolve destination provider alternatif 

- Add new library :
  . provider2beepcast-v2.2.24.jar
  . channel2beepcast-v1.0.64.jar
  . router2beepcast-v1.0.36.jar
  . model2beepcast-v1.0.76.jar
  . subscriber2beepcast-v1.2.35.jar
  . dwh_model-v1.0.32.jar
  . beepcast_dbmanager-v1.1.35.jar

v1.1.27

- For GSM 7 Bit , the following characters need to counted as 2 character and not 1.
    |, ^,{, }, €, [, ~, ] and \

- Found sluggishness in this records :

    ./log/beepadmin-20141203-35.log:03.12.2014 20.42.02:325 19611205 DEBUG   
      {ChannelProcessorThread-22}Transaction [INT2080889] 
        Resolving trans queue bean , with : sessionPersist = true , guestCode = null
    ./log/beepadmin-20141203-35.log:03.12.2014 20.42.21:155 19614822 DEBUG   
      {ChannelProcessorThread-22}Transaction [INT2080889] 
        Found empty guest code , trying get the one from message content
    
  and if found that there is a synchronized lock object java inside , and this one , make it slow .
  
  Solution : make it paralel based on array lock objects , it filter based on the last character
  of mobile phone number
  
- Adjust clean expiry subscriber session to make it often .
    
- Add new library :
  . 

v1.1.26

- Change in table global_properties

    Transaction.ShortenerLink
    https?://(xip\.me|xipme\.com)[:0-9]*(\S*)
    https?://(xip\.me|xipme\.com)[-a-zA-Z0-9+&@#/%?=~_|!:,.;<>]*[-a-zA-Z0-9+&@#/%=~_|<>]

- Support for xipme encryption 

- Add new library :
  . xipme_api-v1.0.26.jar
  . channel2beepcast-v1.0.61.jar
  . router2beepcast-v1.0.34.jar
  . provider2beepcast-v2.2.21.jar
  . msglink2beepcast-v1.0.20.jar
  . client_request2beepcast-v1.0.04.jar
  . model2beepcast-v1.0.74.jar
  . 

v1.1.25

- Add new reserved keyword "XIPME" with the details following requirements
  . Use the format XIPME( XIPME_LINK , TARGET_LINK ) 
    example <#XIPME(http://xip.me/abcde)#>
    example <#XIPME(http://xip.me/abcde, http://www.directtomobile.com)#>
  . 

- Add new library :
  . 

v1.1.24

- Found bug that when event's response is empty , it cant do the sendMessage
  / sendBulkMessage

- Found bug that <#TMSG_MESSAGE_CONTENT#> doesnt work with unicode character

- Found bug to send unicode message from sendBulkMessages Api

- Add new library :
  . xipme_api-v1.0.25.jar
  . provider2beepcast-v2.2.19.jar
  . msglink2beepcast-v1.0.19.jar
  . model2beepcast-v1.0.71.jar
  . beepcast_encrypt-v1.0.04.jar
  . 

v1.1.23

- Add feature "SEND IF" event step , it will send message when
  the condition is valid and go to the next step
  
  (A) (operator) (B*)
  
  operator can be eq / equal / neq / notequal / contain / 

- Add feature "DELAY_SEND" event step , with example params below 

  +months:2 +days:2 hours:13 minutes:00
  +months:2 hours:13 minutes:00
  +weeks:3 days:3 hours:15 minutes:25
  +days:5 hours:15 minutes:30
  +hours:2 minutes:25

- There is a bug for "Var" event step

- Add new library :
  . channel2beepcast-v1.0.60.jar
  . provider2beepcast-v2.2.18.jar
  . client2beepcast-v2.3.06.jar
  . beepcast_dbmanager-v1.1.33.jar

v1.1.22

- Forward list custom field to xipme 1 - 20 ...

- Found bug that the transaction process doesn't populated the <#LIST_CUSTOMX#> in the xipme link,
  as long as the message doesn't have any <#LIST_CUSTOMX#> to populate ...

- Add new library :
  . xipme_api-v1.0.24.jar
  . channel2beepcast-v1.0.58.jar
  . router2beepcast-v1.0.33.jar
  . msglink2beepcast-v1.0.18.jar
  . client2beepcast-v2.3.05.jar
  . model2beepcast-v1.0.70.jar
  . subscriber2beepcast-v1.2.34.jar
  . dwh_model-v1.0.30.jar
  . beepcast_online_properties-v1.0.05.jar
  . beepcast_dbmanager-v1.1.32.jar
  . beepcast_encrypt-v1.0.03.jar
  . beepcast_onm-v1.2.08.jar
  . 

v1.1.21

- Found bug during the email step

    DEBUG   {RouterMOWorkerThread-1}Transaction [MDM3560002] [Step-0] Perform email step
    DEBUG   {RouterMOWorkerThread-1}Transaction [MDM3560002] [Step-0] Define base upload 
      path = /opt/beepcast/beepadmin-test/beepfiles/uploads/
    {always stop here} 
    
- Add new library :
  . msglink2beepcast-v1.0.15.jar
  . model2beepcast-v1.0.69.jar
  . subscriber2beepcast-v1.2.29.jar
  . beepcast_session-v1.0.02.jar
  . 

v1.1.20

- Support reserved variable till 30 custom fields 

- Revalidate the transaction process for simulation to support duplicate numbers

- Add multiple worker on the transaction simulation process

- All the email sending will use thru email app

- Add new library :
  . beepcast_onm-v1.2.07.jar
  . model2beepcast-v1.0.68.jar
  . subscriber2beepcast-v1.2.28.jar
  . channel2beepcast-v1.0.56.jar
  . provider2beepcast-v2.2.17.jar
  . msglink2beepcast-v1.0.14.jar
  . client_request2beepcast-v1.0.03.jar
  . billing2beepcast-v1.1.07.jar
  . beepcast_loadmanagement-v1.2.04.jar
  . beepcast_keyword-v1.0.06.jar
  . 

v1.1.19

- Clean the EventProcessEmailTo and EventProcessEmail

- Add new library :
  . channel2beepcast-v1.0.55.jar
  . router2beepcast-v1.0.31.jar
  . provider2beepcast-v2.2.16.jar
  . msglink2beepcast-v1.0.13.jar
  . client2beepcast-v2.3.03.jar
  . subscriber2beepcast-v1.2.27.jar
  . dwh_model-v1.0.29.jar
  . beepcast_online_properties-v1.0.04.jar
  . beepcast_encrypt-v1.0.02.jar
  . model2beepcast-v1.0.67.jar

v1.1.18

- Found bug that global variable "<#TMSG_MESSAGE_CONTENT#>" shown 
  mo message with strip off whitespaces

- Store mo message into transaction table with message content 
  that shown as it is

- Forward mo message for the client thru api "fetch_message"
  the message content shall be shown as it is ( no strip off whitespaces )

- Add new library :
  . 

v1.1.17

- Found bug that can't resolve message count for unicode message

- Found bug that can't resolve chinese characters message

- Add new library :
  . 

v1.1.16

- Disable to update event's ping count for every transaction for performance wise

- Disable debug to show the xml of the xipme request or response

- Add feature to trap load management 

- Add feature to resolve xipme code with additional information , like gatewayXipmeId param

- Found bugs inside the params of clone xipme map :
  . it should use insert action for the map_params 
  
- Add new library :
  . xipme_api-v1.0.23.jar
  . model2beepcast-v1.0.66.jar
  . beepcast_xipme-v1.0.00.jar

v1.1.15

- Clean all the phone in the trans queue and trans log

- Add feature to dig out the last session event with no end 

- Add new library :
  . ...

v1.1.14

- Found bug when there is no active / available providers , the message doesn't store into gateway log table :

  beepadmin-20131113-1.log	555359	13.11.2013 15.13.08:662 555350 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [1] Trying to resolve destination provider
  beepadmin-20131113-1.log	555360	13.11.2013 15.13.08:662 555351 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [#1 Attempt] Resolving destination provider from outbound message param
  beepadmin-20131113-1.log	555361	13.11.2013 15.13.08:662 555352 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [#1 Attempt] Failed to resolve destination provider from outbound message param
  beepadmin-20131113-1.log	555362	13.11.2013 15.13.08:662 555353 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [#2 Attempt] Resolving destination provider from country to provider map
  beepadmin-20131113-1.log	555363	13.11.2013 15.13.08:662 555354 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Trying to resolve provider based from country : id = 1 , code = SG , name = Singapore
  beepadmin-20131113-1.log	555364	13.11.2013 15.13.08:662 555355 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Failed to resolve provider from the country destination number , found empty map
  beepadmin-20131113-1.log	555365	13.11.2013 15.13.08:662 555356 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [#3 Attempt] Resolving destination provider from number to provider map
  beepadmin-20131113-1.log	555366	13.11.2013 15.13.08:662 555357 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Resolving destination provider based on : groupConnId = 5 , longNumber = 91093429 , countryCode = SG
  beepadmin-20131113-1.log	555367	13.11.2013 15.13.08:662 555358 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Perform to get provider from group with maximum level = 3
  beepadmin-20131113-1.log	555368	13.11.2013 15.13.08:662 555359 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Get provider from the group providers , level #0
  beepadmin-20131113-1.log	555369	13.11.2013 15.13.08:662 555360 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Failed to find group providers based on country code = SG , will search on the default map instead .
  beepadmin-20131113-1.log	555370	13.11.2013 15.13.08:662 555361 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Found a group providers with : longNumber = 91093429 , level = 0 , groupConnId = 5 , total = 1 provider(s)
  beepadmin-20131113-1.log	555377	13.11.2013 15.13.08:662 555368 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Read list active providers [EE, SB, EE3, EE1, EE2, SB5]
  beepadmin-20131113-1.log	555378	13.11.2013 15.13.08:662 555369 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Clean list providers read from outgoingNumberToProvider table : validList = [] , invalidList = [EE4]
  beepadmin-20131113-1.log	555379	13.11.2013 15.13.08:662 555370 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] Failed to get provider , found empty list of map outgoing number to provider
  beepadmin-20131113-1.log	555380	13.11.2013 15.13.08:662 555371 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Get provider from the group providers , level #1
  beepadmin-20131113-1.log	555381	13.11.2013 15.13.08:663 555372 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Failed to find group providers based on country code = SG , will search on the default map instead .
  beepadmin-20131113-1.log	555385	13.11.2013 15.13.08:663 555376 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Found a group providers with : longNumber = 91093429 , level = 1 , groupConnId = 5 , total = 1 provider(s)
  beepadmin-20131113-1.log	555399	13.11.2013 15.13.08:663 555390 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Read list active providers [EE, SB, EE3, EE1, EE2, SB5]
  beepadmin-20131113-1.log	555400	13.11.2013 15.13.08:663 555391 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Clean list providers read from outgoingNumberToProvider table : validList = [] , invalidList = [SB1]
  beepadmin-20131113-1.log	555401	13.11.2013 15.13.08:663 555392 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] Failed to get provider , found empty list of map outgoing number to provider
  beepadmin-20131113-1.log	555402	13.11.2013 15.13.08:663 555393 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Get provider from the group providers , level #2
  beepadmin-20131113-1.log	555409	13.11.2013 15.13.08:663 555400 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Failed to find group providers based on country code = SG , will search on the default map instead .
  beepadmin-20131113-1.log	555410	13.11.2013 15.13.08:664 555401 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Found a group providers with : longNumber = 91093429 , level = 2 , groupConnId = 5 , total = 1 provider(s)
  beepadmin-20131113-1.log	555441	13.11.2013 15.13.08:664 555432 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Read list active providers [EE, SB, EE3, EE1, EE2, SB5]
  beepadmin-20131113-1.log	555442	13.11.2013 15.13.08:664 555433 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Clean list providers read from outgoingNumberToProvider table : validList = [] , invalidList = [MB1]
  beepadmin-20131113-1.log	555443	13.11.2013 15.13.08:664 555434 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] Failed to get provider , found empty list of map outgoing number to provider
  beepadmin-20131113-1.log	555444	13.11.2013 15.13.08:664 555435 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] Failed to resolve destination provider based on outgoing number to provider map
  beepadmin-20131113-1.log	555445	13.11.2013 15.13.08:664 555436 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [#3 Attempt] Failed to resolve destination provider from number to provider map
  beepadmin-20131113-1.log	555446	13.11.2013 15.13.08:664 555437 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [#4 Attempt] Resolving destination provider from inbound provider
  beepadmin-20131113-1.log	555474	13.11.2013 15.13.08:664 555465 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] Found blank imsg original provider
  beepadmin-20131113-1.log	555492	13.11.2013 15.13.08:664 555483 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Read list active providers [EE, SB, EE3, EE1, EE2, SB5]
  beepadmin-20131113-1.log	555494	13.11.2013 15.13.08:665 555485 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Can not find router order name based on inbound provider = null , trying to use default name = default
  beepadmin-20131113-1.log	555495	13.11.2013 15.13.08:665 555486 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Defined router order name = default
  beepadmin-20131113-1.log	555506	13.11.2013 15.13.08:665 555497 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] Defined router order : RouteOrder ( name = default listPriorityProviderIds = [EE4, MB1, SB1]  )
  beepadmin-20131113-1.log	555509	13.11.2013 15.13.08:665 555500 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] Failed to get candidate winner from random priority , found zero total providers loaded into random list
  beepadmin-20131113-1.log	555510	13.11.2013 15.13.08:665 555501 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] Failed to resolve destination provider from inbound provider , found failed to resolve next provider id = null
  beepadmin-20131113-1.log	555511	13.11.2013 15.13.08:665 555502 DEBUG   {ChannelProcessorThread-9}Transaction [INT14757417] [#4 Attempt] Failed to resolve destination provider from inbound provider
  beepadmin-20131113-1.log	555512	13.11.2013 15.13.08:665 555503 WARNING {ChannelProcessorThread-9}Transaction [INT14757417] [1] Failed to finalized , found failed to resolve destination provider

- Found bug to save the unstored queue mt message into the gateway log , please find log below :

  ../../log/beepadmin-20131111-52.log:11.11.2013 17.51.29:876 15939414 DEBUG   {ChannelProcessorThread-13}RouterMTWorker [RouterMessage-INT18849772] Send Mt Message : messageId = INT18849772 , gatewayMessageType = 0 , messageCount = 2 , messageContent = Test SG 100000 Numbers B CO4_00037411 CO5_00037411  http:\/\/xip.me\/Q0bnf 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 End , debitAmount = 2.0 , phoneNumber = +6500037411 , providerId = BC1 , eventId = 2559 , channelSessionId = 746 , senderId = +6591093429 , priority = 1 , retry = 0 , dateSend = null , mapParams = {}
  ../../log/beepadmin-20131111-52.log:11.11.2013 17.51.29:876 15939415 DEBUG   {ChannelProcessorThread-13}RouterMessageCommon [RouterMessage-INT18849772] Found null submit date time , meant it will send straight away
  ../../log/beepadmin-20131111-52.log:11.11.2013 17.51.29:876 15939416 DEBUG   {ChannelProcessorThread-13}RouterMTWorker [RouterMessage-INT18849772] Found process queue is full
  ../../log/beepadmin-20131111-52.log:11.11.2013 17.51.29:876 15939417 DEBUG   {ChannelProcessorThread-13}RouterMTWorker [RouterMessage-INT18849772] Stored the mt message into input batch queue
  ../../log/beepadmin-20131111-52.log:11.11.2013 17.51.30:378 15941699 DEBUG   {ChannelProcessorThread-13}Transaction [INT18849772] Successfully inserted an outgoing message into send buffer
  ../../log/beepadmin-20131111-52.log:11.11.2013 17.51.30:378 15941700 DEBUG   {ChannelProcessorThread-13}Transaction [INT18849772] [1] Successfully processed as normal message
  ../../log/beepadmin-20131111-52.log:11.11.2013 17.51.55:002 16051148 DEBUG   {ChannelProcessorThread-6}RouterMTWorker Read total 288 send buffer bean(s) ready to insert into send buffer table : INT18849677,INT18849678,INT18849680,INT18849679,INT18849681,INT18849682,INT18849683,INT18849684,INT18849685,INT18849686,INT18849687,INT18849688,INT18849689,INT18849690,INT18849698,INT18849699,INT18849702,INT18849703,INT18849705,INT18849704,INT18849715,INT18849763,INT18849764,INT18849765,INT18849766,INT18849767,INT18849769,INT18849770,INT18849768,INT18849771,INT18849774,INT18849772,INT18849773,INT18849775,INT18849776,INT18849777,INT18849778,INT18849779,INT18849780,INT18849781,INT18849783,INT18849784,INT18849785,INT18849786,INT18849795,INT18849796,INT18849799,INT18849800,INT18849826,INT18849827,INT18849822,INT18849829,INT18849830,INT18849832,INT18849833,INT18849835,INT18849836,INT18849837,INT18849838,INT18849840,INT18849841,INT18849842,INT18849843,INT18849844,INT18849845,INT18849848,INT18849849,INT18849850,INT18849851,INT18849852,INT18849853,INT18849854,INT18849855,INT18849856,INT18849857,INT18849858,INT18849861,INT18849863,INT18849866,INT18849867,INT18849870,INT18849871,INT18849872,INT18849882,INT18849883,INT18849885,INT18849886,INT18849889,INT18849890,INT18849891,INT18849892,INT18849893,INT18849894,INT18849895,INT18849896,INT18849897,INT18849898,INT18849899,INT18849900,INT18849901,INT18849902,INT18849908,INT18849907,INT18849909,INT18849910,INT18849912,INT18849942,INT18849943,INT18849944,INT18849946,INT18849947,INT18849948,INT18849958,INT18849960,INT18849976,INT18849978,INT18849985,INT18849986,INT18849988,INT18849989,INT18849990,INT18849991,INT18849993,INT18849994,INT18850004,INT18850005,INT18850006,INT18850008,INT18850017,INT18850018,INT18850020,INT18850021,INT18850022,INT18850023,INT18850032,INT18850034,INT18850035,INT18850036,INT18850037,INT18850047,INT18850048,INT18850050,INT18850051,INT18850052,INT18850053,INT18850064,INT18850070,INT18850075,INT18850077,INT18850078,INT18850085,INT18850086,INT18850087,INT18850088,INT18850089,INT18850091,INT18850092,INT18850099,INT18850104,INT18850105,INT18850106,INT18850107,INT18850114,INT18850115,INT18850116,INT18850117,INT18850119,INT18850120,INT18850121,INT18850122,INT18850131,INT18850132,INT18850134,INT18850135,INT18850136,INT18850137,INT18850148,INT18850149,INT18850150,INT18850151,INT18850152,INT18850155,INT18850156,INT18850158,INT18850159,INT18850160,INT18850161,INT18850162,INT18850163,INT18850164,INT18850173,INT18850175,INT18850176,INT18850177,INT18850178,INT18850188,INT18850190,INT18850191,INT18850192,INT18850193,INT18850199,INT18850200,INT18850201,INT18850203,INT18850205,INT18850206,INT18850207,INT18850208,INT18850221,INT18850219,INT18850220,INT18850222,INT18850233,INT18850234,INT18850235,INT18850236,INT18850237,INT18850246,INT18850247,INT18850248,INT18850249,INT18850250,INT18850251,INT18850252,INT18850253,INT18850274,INT18850282,INT18850283,INT18850284,INT18850285,INT18850286,INT18850287,INT18850288,INT18850289,INT18850290,INT18850291,INT18850292,INT18850293,INT18850294,INT18850295,INT18850296,INT18850297,INT18850298,INT18850299,INT18850300,INT18850301,INT18850302,INT18850303,INT18850304,INT18850309,INT18850311,INT18850310,INT18850312,INT18850314,INT18850315,INT18850316,INT18850317,INT18850327,INT18850329,INT18850330,INT18850339,INT18850341,INT18850342,INT18850343,INT18850344,INT18850345,INT18850346,INT18850347,INT18850348,INT18850349,INT18850350,INT18850351,INT18850352,INT18850353,INT18850354,INT18850355,INT18850356,INT18850357,INT18850358,INT18850359,INT18850360,INT18850361,INT18850362,INT18850363,INT18850364,INT18850365,INT18850366,INT18850367,
  ../../log/beepadmin-20131111-57.log:11.11.2013 18.00.19:001 17905350 DEBUG   {RouterMTDbOuWorker}ProviderApp [ProviderMessage-INT18849772] Send message : dstAddr = +6500037411 , oriAddr = null , oriAddrMask = +6591093429 , debitAmount = 2.0 , cntType = 0 , msgCount = 2 , msgContent = Test SG 100000 Numbers B CO4_00037411 CO5_00037411  http:\/\/xip.me\/Q0bnf 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 End
  ../../log/beepadmin-20131111-57.log:11.11.2013 18.00.19:001 17905351 DEBUG   {RouterMTDbOuWorker}ProviderUtil [ProviderMessage-INT18849772] Resolved master provider : id = 36 , providerId = BC , direction = OU , type = EXTERNAL , description = Beepcast Mach Simulator
  ../../log/beepadmin-20131111-57.log:11.11.2013 18.00.22:991 17950770 WARNING {RouterMTDbOuWorker}MTSendWorker [Provider-BC1] [ProviderMessage-INT18849772] Failed to store provider message into provider agent channel , with : BC , +6500037411 , SMS.MT , SMSTEXT , Test SG 100000 Numbers B CO4_0 , takes = 3990 ms
  ../../log/beepadmin-20131111-57.log:11.11.2013 18.00.22:993 17950777 WARNING {RouterMTDbOuWorker}ProviderService [RouterMessage-INT18849772] Failed to process mt message , found failed to send message thru provider app
  ../../log/beepadmin-20131111-57.log:11.11.2013 18.00.22:993 17950778 WARNING {RouterMTDbOuWorker}RouterMTWorker [RouterMessage-INT18849772] Failed to submit a mt message to provider app

  If found failed to store into send buffer , 
  the transaction process standard should store into gateway log table ,
  So it's persistance .
  
- Add new library :
  . model2beepcast-v1.0.64.jar
  . router2beepcast-v1.0.30.jar

v1.1.13

- Change the order of default keyword , put after directcode and beepcode finding .

- Add new library :
  . channel2beepcast-v1.0.51.jar
  . router2beepcast-v1.0.29.jar
  . provider2beepcast-v2.2.14.jar
  . msglink2beepcast-v1.0.11.jar
  . client2beepcast-v2.3.02.jar
  . model2beepcast-v1.0.63.jar
  . dwh_model-v1.0.27.jar
  . beepcast_keyword-v1.0.05.jar
  . beepcast_onm-v1.2.06.jar
  . 

v1.1.12

- Fix the autosend event process step , seems doesn't work properly

- Clean up the gateway and mobile user table ( phone and message )

  gatewayLogDAO
  mobileUserDAO

- Disabled all the sql log related to trans_log and trans_queue

- Encrypt phone number in the trans_log and trans_queue table
  
  Add encrypt_phone field after phone field
  
    ALTER TABLE `trans_log`
      ADD COLUMN `encrypt_phone` VARBINARY(512) AFTER `phone` ;
    ALTER TABLE `trans_queue`
      ADD COLUMN `encrypt_phone` VARBINARY(512) AFTER `phone` ;

    ALTER TABLE `trans_log`
      MODIFY COLUMN `date_tm` DATETIME ,
      ADD INDEX `encrypt_phone`(`encrypt_phone`) ;
    ALTER TABLE `trans_queue` 
      ADD INDEX `encrypt_phone`(`encrypt_phone`) ;
      
  Fill in the encrypt_phone field
      
    UPDATE `trans_log` SET encrypt_phone = AES_ENCRYPT( phone , 'jkiZmu0s575xFbgGFkdQ' ) ;
    UPDATE `trans_queue` SET encrypt_phone = AES_ENCRYPT( phone , 'jkiZmu0s575xFbgGFkdQ' ) ;
    
  Change the primary key field on trans queue table

    ALTER TABLE `trans_queue`
      DROP PRIMARY KEY , ROW_FORMAT = DYNAMIC ;

    ALTER TABLE `trans_queue`
      ADD COLUMN `queue_id` INTEGER AUTO_INCREMENT PRIMARY KEY FIRST ;

    ALTER TABLE `trans_queue`
      ADD INDEX `phone`(`phone`) ;

- Add new library :
  . provider2beepcast-v2.2.12.jar
  . client2beepcast-v2.3.01.jar
  . subscriber2beepcast-v1.2.25.jar
  . beepcast_onm-v1.2.04.jar
  . beepcast_imgen-v1.0.03.jar
  . msglink2beepcast-v1.0.10.jar
  . model2beepcast-v1.0.62.jar
  . beepcast_keyword-v1.0.04.jar
  . beepcast_dbmanager-v1.1.31.jar
  . beepcast_clientapi-v1.0.03.jar

v1.1.11

- There is bug inside "Tell a friend" event , the message response doesn't come out .

  DEBUG   {RouterMOWorkerThread-1}Transaction [MDM17875010] Process message as Tell a Friend Type
  DEBUG   {RouterMOWorkerThread-1}Transaction [MDM17875010] Found process type names = [NAME,PHONE1,PHONE2,PHONE3,PHONE4,PHONE5]
  DEBUG   {RouterMOWorkerThread-1}Transaction [MDM17875010] Trying to handle special params
  DEBUG   {RouterMOWorkerThread-1}RouterMTWorker Send Mt Message : messageId = INT17875011 , gatewayMessageType = 0 , messageCount = 1 , messageContent = Thank you for referring your friends.\nWe will contact you to collect your voucher once your friends have confirmed their participation. , debitAmount = 1.0 , phoneNumber = +6596328785 , providerId = STARHUBMDM01 , eventId = 2445 , channelSessionId = 0 , senderId = null , priority = 0 , retry = 0 , dateSend = 2013-07-31 14:08:52 , mapParams = null
  WARNING {RouterMOWorkerThread-1}RouterMessageCommon [RouterMessage-INT17875011] Failed to verify mt message , found blank original address mask
  WARNING {RouterMOWorkerThread-1}RouterMTWorker Failed to send mt message , found invalid parameters inside
  DEBUG   {RouterMOWorkerThread-1}TransactionLogDAO Perform INSERT INTO trans_log (client_id,event_id,next_step,catagory_id,date_tm,phone,provider_id,message_count,code,params,jump_count,location_id,closed_reason_id) VALUES ( 1,2445,1,38,'2013-07-31 14:08:52','+6596328785','STARHUBMDM01',1,'AEOX','',0,0,0 )
  DEBUG   {RouterMOWorkerThread-1}TransactionQueueDAO Perform DELETE FROM trans_queue WHERE ( phone = '+6596328785' )
  DEBUG   {RouterMOWorkerThread-1}RouterMOProcessor [RouterMessage-MDM17875010] Finish transaction process , resultCode = PROCESS_SUCCEED , take = 48 ms

v1.1.10

- Add process step "IF DATE BEFORE" and "IF DATE AFTER"

- Add new library :
  . model2beepcast-v1.0.61.jar

v1.1.09

- Found bug inside transaction process with masking sender id applied ,
  the masking doesn't mask properly .

- Add new library :
  . channel2beepcast-v1.0.50.jar

v1.1.08

- Put additional function to extract xipme message codes

- Add new library :
  . xipme_api-v1.0.22.jar
  . client_request2beepcast-v1.0.02.jar
  . 

v1.1.07

- Bug: DTM Response Processing - It seems that the user reponses that include punctuation are being ignored, 
  and not pre-stipped of punctuation to process. Pls look into this. Suggest the following:- Strip puntuation 
  from reponses to process the intended reply- Store the actual responses in their raw format, so that we 
  acutally know what users are sending.- I thought this worked before, but does not seem to now. 
  
  Use guest code to compare the expect keyword ...

- Add new library :
  . channel2beepcast-v1.0.49.jar
  . router2beepcast-v1.0.27.jar
  . client2beepcast-v2.3.00.jar
  . subscriber2beepcast-v1.2.21.jar
  . dwh_model-v1.0.25.jar
  . beepcast_online_properties-v1.0.03.jar
  . beepcast_dbmanager-v1.1.30.jar
  . beepcast_onm-v1.2.02.jar
  . 

v1.1.06

- There is a changes inside file ./conf/oproperties.xml

      <property field="Transaction.RouteProvider.INTERNAL" value="default"
        description="Define route order name based on internal inbound provider" />
      <property field="Transaction.RouteProvider.DEFAULT" value="default"
        description="Define route order name based on default inbound provider" />
      <property field="Transaction.RouteOrder.default" value="EE4:MB1:SB1"
        description="Define list order provider ids based on default route order" />

      <property field="ProviderAgent.EnableErrorCheck.EE" value="true"
        description="Enable to perform verification of provider's connection periodically" />
      <property field="ProviderAgent.MaxErrorTolerant.EE" value="3"
        description="Maximum Error tolerance to confirm provider's connection status" />
        
- Add structure inside table outgoing number to provider

  ALTER TABLE `outgoing_number_to_provider` 
    ADD COLUMN `level` INT(10) UNSIGNED NOT NULL DEFAULT 0 AFTER `outgoing_number` ;

- Fix bug java.lang.NullPointerException
	at com.beepcast.model.transaction.xipme.XipmeTranslator.resolveMessageContent(XipmeTranslator.java:215)
	at com.beepcast.model.transaction.TransactionSupport.resolveShortenerLink(TransactionSupport.java:3106)
	at com.beepcast.model.transaction.TransactionProcessStandard.end(TransactionProcessStandard.java:521)
	at com.beepcast.model.transaction.TransactionProcess.main(TransactionProcess.java:64)
	at com.beepcast.model.test.TestTransactionApp07.main(TestTransactionApp07.java:235)

- Add new library :
  . provider2beepcast-v2.2.10.jar
  . beepcast_dbmanager-v1.1.29.jar

v1.1.05

- Don't do the suspend if found bypass suspend flag inside the message param

- Add new library :
  . 

v1.1.04

- Add feature to send message with sender id updated from input message
  put as message param to apply this function

- Add new library :
  . model2beepcast-v1.0.59.jar
  . beepcast_dbmanager-v1.1.27.jar

v1.1.03

- Found bugs XipmeTranslator 

    DEBUG   {RouterMOWorkerThread-0}HttpDispatch Create http post request ( http://192.168.2.111:8881/ ) = <request>\n  <authentication>\n    <username>benny<\/username>\n    <password>benny<\/password>\n  <\/authentication>\n  <commands>\n    <command>\n      <commandId>vBdiAQP8eM.0<\/commandId>\n      <commandName>CloneMap<\/commandName>\n      <commandParams>\n        <commandParam>\n          <key>mapId<\/key>\n          <value>s3ut<\/value>\n        <\/commandParam>\n        <commandParam>\n          <key>actions<\/key>\n          <values>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP_PARAM<\/tableName>\n              <fieldName>$User.FirstName<\/fieldName>\n              <newValue>Benny<\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP_PARAM<\/tableName>\n              <fieldName>$User.FamilyName<\/fieldName>\n              <newValue><\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP_PARAM<\/tableName>\n              <fieldName>$User.Email<\/fieldName>\n              <newValue><\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP_PARAM<\/tableName>\n              <fieldName>$User.ID<\/fieldName>\n              <newValue><\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP_TO_DTM<\/tableName>\n              <fieldName>SENDERID<\/fieldName>\n              <newValue>91093429<\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP_TO_DTM<\/tableName>\n              <fieldName>EVENTID<\/fieldName>\n              <newValue>20<\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP<\/tableName>\n              <fieldName>CHANNEL<\/fieldName>\n              <newValue>sm<\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP<\/tableName>\n              <fieldName>CUSTOMERID<\/fieldName>\n              <newValue>6590517715<\/newValue>\n            <\/action>\n            <action>\n              <type>UPDATE<\/type>\n              <tableName>MAP<\/tableName>\n              <fieldName>TAGS<\/fieldName>\n              <newValue>dtmeng,0<\/newValue>\n            <\/action>\n          <\/values>\n        <\/commandParam>\n      <\/commandParams>\n    <\/command>\n  <\/commands>\n<\/request>
    DEBUG   {RouterMOWorkerThread-0}HttpDispatch Read http post response ( 10 ms ) = <response>  <authentication>    <statusCode>OK<\/statusCode>    <statusDescription>Successfully Login<\/statusDescription>  <\/authentication>  <commands>    <command>      <commandId>vBdiAQP8eM.0<\/commandId>      <commandName>CloneMap<\/commandName>      <statusCode>OK<\/statusCode>      <statusDescription>Cloned<\/statusDescription>      <commandResults>        <commandResult>          <key>mapId<\/key>          <value>s3uu<\/value>        <\/commandResult>        <commandResult>          <key>masterMapId<\/key>          <value>s3ut<\/value>        <\/commandResult>        <commandResult>          <key>clientId<\/key>          <value>1<\/value>        <\/commandResult>        <commandResult>          <key>urlLink<\/key>          <value>http:\/\/192.168.2.111:8080\/beepadmin\/websendmsg\/request.jsp?tid=8565654b13c54b4fb7175df562f1a91<\/value>        <\/commandResult>        <commandResult>          <key>channel<\/key>          <value>sm<\/value>        <\/commandResult>        <commandResult>          <key>customerId<\/key>          <value>6590517715<\/value>        <\/commandResult>        <commandResult>          <key>state<\/key>          <value>Live<\/value>        <\/commandResult>        <commandResult>          <key>urlCodeFull<\/key>          <value>http:\/\/apus:8883\/s3uu<\/value>        <\/commandResult>        <commandResult>          <key>urlCodeShort<\/key>          <value>apus:8883\/s3uu<\/value>        <\/commandResult>        <commandResult>          <key>description<\/key>          <value>Test Outgoing Customized Xipme Event 05<\/value>        <\/commandResult>        <commandResult>          <key>tags<\/key>          <value>dtmeng,0<\/value>        <\/commandResult>        <commandResult>          <key>mapParams<\/key>          <values>            <mapParam>              <name>Mobile<\/name>              <value>$CUSTOMER_ID<\/value>            <\/mapParam>            <mapParam>              <name>_xc<\/name>              <value>$XIPME_CODE<\/value>            <\/mapParam>            <mapParam>              <name>_vi<\/name>              <value>$VISIT_ID<\/value>            <\/mapParam>            <mapParam>              <name>_db<\/name>              <value>$DEVPROF.BRAND_NAME<\/value>            <\/mapParam>            <mapParam>              <name>_dm<\/name>              <value>$DEVPROF.MODEL_NAME<\/value>            <\/mapParam>            <mapParam>              <name>_dw<\/name>              <value>$DEVPROF.RESOLUTION_WIDTH<\/value>            <\/mapParam>          <\/values>        <\/commandResult>        <commandResult>          <key>mapDirectToMobiles<\/key>          <values\/>        <\/commandResult>      <\/commandResults>    <\/command>  <\/commands><\/response>
    DEBUG   {RouterMOWorkerThread-0}XipmeSupport Converted iterCmdResps to mapResult ( take 0 ms ) : totalCmdResp =1 , totalCmdRespOk = 1 , totalChannelLogId = 1 , totalMapInnerCodes = 1 , totalMapCodes = 1 , totalDuplicatedChannelLogId = 0 , totalDuplicatedInnerCodes = 0
    DEBUG   {RouterMOWorkerThread-0}XipmeSupport Executed clone maps ( take 22 ms ) : listCmdResps.size = 1 , mapResult.size = 1 , mapResult.data = {0={s3ut=s3uu}}
    WARNING {RouterMOWorkerThread-0}XipmeSupport [MDM10826000] Failed to update message links , java.lang.ClassCastException
    WARNING {RouterMOWorkerThread-0}XipmeTranslator [MDM10826000] Failed to update any links inside the message content , stored back with original message

- Change format data inside XipmeSupport.convertIterCmdRespsToMap , it will use 
  Map inside the map instead of String , no need to parsing with "." 

- Take out xipmeTranslator.extractMessageLinks because it part of channel log library

- add new library :
  . 

v1.1.02

- add new provider XX1 as dummy provider 

  INSERT INTO provider
  ( master_id , provider_id , provider_name , direction , `type` , short_code , country_code
  , access_url , access_username , access_password , listener_url
  , in_credit_cost , ou_credit_cost , description , active , date_inserted , date_updated  )
  VALUES
  ( 0 , 'XX1' , 'XX1' , 'IO' , 'INTERNAL' , '' , ''
  , '' , '' , '' , ''
  , 0 , 0 , 'XX1' , 1 , NOW() , NOW() ) ;

- switch to different ( foo ) provider for log step transaction

- fix bug : sms content chop 160 characters for log step

- disable feature SplitLongSmsMessageByProvider inside the transaction process

- add new library :
  . 

v1.1.01

- Add new process step : SMS_TO_SMS_XSENDER

- Found bug inside sms to sms step :

    15.11.2012 11.06.28:844 6093954 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM16430278] Performing sms to sms step
    15.11.2012 11.06.28:844 6093955 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM16430278] Replace reserved variables , found code = AEPL
    15.11.2012 11.06.28:849 6093956 WARNING {RouterMOWorkerThread-0}Transaction [MDM16430278] Failed to replace reserved variable for contact list , found empty client subscriber bean
    15.11.2012 11.06.28:849 6093957 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM16430278] Replaced reserved variable : varName = LIST_CUSTOM1 , parName =  , funcName = null , funcParamA = null , funcParamB = null , strResult =
    15.11.2012 11.06.28:850 6093958 WARNING {RouterMOWorkerThread-0}Transaction [MDM16430278] Failed to replace reserved variable for contact list , found empty client subscriber bean
    15.11.2012 11.06.28:850 6093959 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM16430278] Replaced reserved variable : varName = LIST_CUSTOM2 , parName =  , funcName = null , funcParamA = null , funcParamB = null , strResult =
    15.11.2012 11.06.28:850 6093960 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM16430278] Built response from process bean ( 60 chars ) = The following person cannot attend today's EB11 event:\n from
    15.11.2012 11.06.28:850 6093961 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM16430278] Created message content : The following person cannot attend today's EB11 event:\n from
    15.11.2012 11.06.28:850 6093962 WARNING {RouterMOWorkerThread-0}Transaction [MDM16430278] Failed to process sms to sms step , found empty list phones
    15.11.2012 11.06.28:850 6093963 WARNING {RouterMOWorkerThread-0}Transaction [MDM16430278] Failed to perform sms to sms step

- add new library :
  . 

v1.1.00

- Insert original transaction message into the gateway log , take out all the strip off message .

- Create mediation transaction process steps , this class will handle the 
  execution of the process steps and make it centralized so that it can be
  used for its implementation class like : TransactionProcessStandard ,
  TransactionProcessSimulation , and TransactionProcessInteraction

- Support multiple output messages inside transaction process standard
  Change design api for transaction , need to upgrade with a new major version

- add new library :
  . xipme_api-v1.0.21.jar

v1.0.81

- Support bypass to send to provider agent

- Support bypass to perform mt debit

- Add log step process

- add new library :
  . 

v1.0.80

- add transaction simulation process

- add new library :
  . channel2beepcast-v1.0.43.jar

v1.0.79

- Update invalid number as valid number when found incoming sms traffic from the modem

- add new library :
  . xipme_api-v1.0.20.jar
  . channel2beepcast-v1.0.42.jar
  . router2beepcast-v1.0.24.jar
  . dwh_model-v1.0.20.jar
  . model2beepcast-v1.0.55.jar
  . client2beepcast-v2.2.13.jar
  . beepcast_database-v1.1.05.jar
  . beepcast_keyword-v1.0.03.jar

v1.0.78

- Found bug java null pointer assignment 

- Add new library :
  . 

v1.0.77

- Found bug for custom list data with like $900,000 , with dollar sign

- Add new library :
  . 

v1.0.76

- Add reserved keyword LIST_CUSTOM6 - LIST_CUSTOM10 inside the process step

- Add feature to store message response from subscriber into mobile user and/or contact list params
  will use reserved variable and put it after expect process step

- Add new library :
  . channel2beepcast-v1.0.41.jar
  . provider2beepcast-v2.2.04.jar
  . subscriber2beepcast-v1.2.19.jar
  . dwh_model-v1.0.19.jar
  . 

v1.0.75

- Found java exception error 

    06.07.2012 17.30.15:281 2390 WARNING {RouterMOWorkerThread-1}Transaction [MDM8385001] 
    Failed to replace variable with : funcName = LEFT , funcParamA = 20 , funcParamB =  , java.lang.StringIndexOutOfBoundsException: String index out of range: 20

- Found bug that SMS_TO_SMS doesnt set credit cost ( zero 0 ) into the gateway log table

- Add new library :
  . 

v1.0.74

- put substring format under reserved keyword , example 
  <#TEST_MSG.LEFT(4)#> , meant test message with get 4 characters from the left
  <#TEST_MSG.RIGHT(3)#> , meant test message with get 3 characters from the right
  <#TEST_MSG.SUB(2)#> , meant test message get substring start from index 2 characters
  <#TEST_MSG.SUB(1,2)#> , meant test message get substring start from index 1 till index 2 characters

- Add new library :
  . 

v1.0.73

- Support multiple keyword under one request sms for interactive event

- Add new library :
  . model2beepcast-v1.0.53.jar

v1.0.72

- Feature to populate contact list information params under interation broadcast message

- Add new process step "SMS_TO_SMS" will forward incoming message into specific destination numbers

- Add new library :
  . router2beepcast-v1.0.23.jar
  . channel2beepcast-v1.0.40.jar

v1.0.71

- Add feature to update phoneCountryId into gateway log table

- Add new library :
  . channel2beepcast-v1.0.39.jar
  . provider2beepcast-v2.2.02.jar
  . model2beepcast-v1.0.52.jar
  . dwh_model-v1.0.17.jar
  . beepcast_encrypt-v1.0.01.jar
  . beepcast_onm-v1.1.09.jar

v1.0.70

- When perform unsubscribed and found no match number inside the list , 
  it will store as new unsubscribed number into that particular list still .

- Add new library :
  . subscriber2beepcast-v1.2.18.jar

v1.0.69

- Add new header for offline xipme code so no need to request to xipme server if found any

- Adjust XipMeSupport to support also for channel library ,
  exposing the generic functions inside .

- Add new library :
  . xipme_api-v1.0.19.jar

v1.0.68

- Add alias reserved keyword

    response = StringUtils.replace( response , "<#LIST_CUSTREFID#>" ,
        StringUtils.trimToEmpty( csBean.getCustomerReferenceId() ) );
    response = StringUtils.replace( response , "<#LIST_CUSTREFCODE#>" ,
        StringUtils.trimToEmpty( csBean.getCustomerReferenceCode() ) );

- Add new library :
  . channel2beepcast-v1.0.37.jar
  . subscriber2beepcast-v1.2.17.jar
  . msglink2beepcast-v1.0.08.jar
  . throttle-v1.0.01.jar

v1.0.67

- Take out old style landing page default params

    try {
      String urlEncodedMobileNumber = URLEncoder
          .encode( mobileNumber , "UTF-8" );
      String qsMobileNumber = "&usr=" + urlEncodedMobileNumber;
      listActions.add( ActionFactory.createActionReplace( Action.TBLNM_MAP ,
          Action.FLDNM_RESOLVEDURL , "&usr." , qsMobileNumber ) );
    } catch ( UnsupportedEncodingException e ) {
      DLog.warning( lctx , headerLog + "Failed to resolve link "
          + ", found failed to put url query string for usr , " + e );
    }

- support xipme translation from subscriber profile

- Add alias reserved keyword

    response = StringUtils.replace( response , "<#USER_ID#>" ,
        StringUtils.trimToEmpty( mobileUserBean.getIc() ) );
    response = StringUtils.replace( response , "<#USER_FIRST_NAME#>" ,
        StringUtils.trimToEmpty( mobileUserBean.getName() ) );
    response = StringUtils.replace( response , "<#USER_FAMILY_NAME#>" ,
        StringUtils.trimToEmpty( mobileUserBean.getLastName() ) );

- Add new library :
  . subscriber2beepcast-v1.2.16.jar
  . xipme_api-v1.0.18.jar

v1.0.66

- Support the multiple thread inside the worker 

- Add new library :
  . channel2beepcast-v1.0.33.jar
  . provider2beepcast-v2.2.01.jar
  . client2beepcast-v2.2.12.jar
  . subscriber2beepcast-v1.2.13.jar
  . beepcast_loadmanagement-v1.2.03.jar
  . 

v1.0.65

- Support all gsm 7 bit characters , only sybase is support and the rest like mach and tyntec , don't support yet .

- Add new library :
  . 

v1.0.64

- Clone during the message process shall put different tags like dtmeng

- The xipme parser shall be flexible enough to change only on code side

- Add new library :
  . channel2beepcast-v1.0.32.jar
  . dwh_model-v1.0.15.jar

v1.0.63

- Apply additional event id information under un/subscribed mechanism

- Add new library :
  . xipme_api-v1.0.15.jar
  . router2beepcast-v1.0.21.jar
  . msglink2beepcast-v1.0.07.jar
  . dwh_model-v1.0.14.jar
  . 

v1.0.62

- Expose channel parameter value inside trans action get gateway log records api .

- Add new library :
  . xipme_api-v1.0.14.jar
  . model2beepcast-v1.0.51.jar

v1.0.61

- When do the clone shortener url , please add channel sms info inside .

- Add new library :
  . xipme_api-v1.0.12.jar
  . msglink2beepcast-v1.0.05.jar

v1.0.60

- Don't reject the transaction when there is no message response from the event ,
  try to verify also from the third party client api sendMessage / sendBulkMessage .

- Add new library :
  . xipme_api-v1.0.11.jar
  . client2beepcast-v2.2.11.jar

v1.0.59

- Change the wording of monthly summary billing for postpaid clients

- Add new library :
  . channel2beepcast-v1.0.31.jar
  . router2beepcast-v1.0.20.jar
  . msglink2beepcast-v1.0.03.jar
  . client2beepcast-v2.2.10.jar
  . billing2beepcast-v1.1.05.jar
  . model2beepcast-v1.0.50.jar
  . subscriber2beepcast-v1.2.10.jar
  . beepcast_online_properties-v1.0.01.jar
  . beepcast_onm-v1.1.08.jar

v1.0.58

- Found bugs :

    beepadmin.log	273111	21.10.2011 17.27.56:781 7262128 WARNING {Thread-6}Transaction 
    [INT2689155] Found invalid incoming provider id = +6500099000
    beepadmin.log	273197	21.10.2011 17.27.56:781 7262214 WARNING {Thread-6}Transaction 
    [INT2689155] Failed to replace variables, found blank rfa

  Please verify why there is checking incoming provider for broadcast message ? 
  and eventhough why checking with phone number not sender id ?
  , and take out warning failed to replace variables , found blank rfa

- Add feature to block all traffic for suspended or suspended_traffic client state

- Add new library :
  . model2beepcast-v1.0.49.jar

v1.0.57

- Added new monthly task to read and summary current debit amount on every postpaid clients

- Add new library :
  . model2beepcast-v1.0.48.jar

v1.0.56

- There are changes inside SpecialMessage structure functions, need to fix in this library also

- Fixed bug event menu expect $email , doesn't work properly .

- There is a virtual provider that can be anywhere running under real provider agent,
  the transaction shall validate properly

    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Failed to resolve provider from the country destination number
    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Resolving destination provider based on : clientId = 1 , groupConnId = 5 , groupConnName = Beepcast , countryCode = SG , longNumber = 91093429
    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Defined current list all active provider ids = [MB1, SB1, TT1, EE1, EE2]
    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Failed to find provider based on country code = SG , will search on the default map instead .
    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Defined list candidate provider ids = [SB5]
    WARNING {RouterMOWorkerThread-0}Transaction [MDM1611000] Found invalid map outgoing number to provider , found provider SB5 is not in the active list
    WARNING {RouterMOWorkerThread-0}Transaction [MDM1611000] Failed to get candidate winner from random priority , found invalid winner provider
    WARNING {RouterMOWorkerThread-0}Transaction [MDM1611000] Failed to get candidate winner from sequence priority , found empty candidate
    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Defined provider winner from first candidate = SB5
    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Resolved destination provider : providerId = SB5 , priority = 60 , masked = 91093429 , countryCode = * , description = Beepcast
    DEBUG   {RouterMOWorkerThread-0}Transaction [MDM1611000] Successfully updated omsg destination provider = SB5

- Add new library :
  . beepcast_dbmanager-v1.1.26.jar
  . provider2beepcast-v2.2.00.jar
  . model2beepcast-v1.0.47.jar

v1.0.55

- Use credit cost value from provider to deduct at mo leg

- Use credit cost value from country to deduct at mt leg
  
- Add new library :
  . beepcast_dbmanager-v1.1.25.jar

v1.0.54

- Resolved provider based on country code and outgoing number instead .

- Resolved the bug of the VAR process step

- Add new library :
  . beepcast_dbmanager-v1.1.24.jar

v1.0.53

- Support keyword to replace the custom fields .

- add feature to resolve provider based on country prefix number ,
  this is as the first priority before to resolved from outgoing number map .
    
- execute sql below :

    ALTER TABLE `channel_log` 
      ADD COLUMN `client_subscriber_id` INTEGER UNSIGNED DEFAULT 0 AFTER `subscribed` ,
      ADD COLUMN `client_subscriber_custom_id` INTEGER UNSIGNED DEFAULT 0 AFTER `client_subscriber_id` ;

- need to be able to customize the following credit alert, 
  so that for partner customers, the partners details are provided, and not BeepCast?'s.
  NOTES: For starters we should change "$10.0" to "10 credits"

- Add new library :
  . subscriber2beepcast-v1.2.05.jar
  . beepcast_loadmanagement-v1.2.02.jar
  . dwh_model-v1.0.10.jar
  . beepcast_session-v1.0.01.jar
  . billing2beepcast-v1.1.04.jar
  . channel2beepcast-v1.0.27.jar
  
v1.0.52

- Add feature to track by storing every information for every person that click the web interaction .

- Add new library :
  . model2beepcast-v1.0.43.jar
  
v1.0.51

- Exclude the plus on the mobile number format for every transaction that goes to xipme

- Support for the new clone map ( xipme ) mechanism :
  * will generate list of action , so it's more dynamic .

- Add feature to support event menu by click under transcaction interact
  * put additional flag to know the message is under the menu type or not .

- Integrated with the new running number library

- Add new library :
  . beepcast_idgen-v1.0.00.jar
  . client2beepcast-v2.2.09.jar
  . router2beepcast-v1.0.18.jar
  . xipme_api-v1.0.07.jar
  
v1.0.50

- Support long message for EE1 / EE2 provider 

- Add feature to resolve shortener url like xip.me/xxxx

- There is a change inside the oproperties.xml file
  
      <property field="Transaction.ShortenerLink" value="(http|https)://xip\\.me/(\\w+)"
        description="The regex expression for xipme to parse inside the message content" />

- Add new library :
  . router2beepcast-v1.0.17.jar
  . msglink2beepcast-v1.0.01.jar
  . client2beepcast-v2.2.08.jar
  . xipme_api-v1.0.06.jar
  
v1.0.49

- Found bugs when process the ping count type of event .

    18.04.2011 17.23.37:406 1488 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM10407000] Process message as Ping Count Type
    18.04.2011 17.23.37:406 1489 WARNING {RouterMOWorkerThread-0}Transaction [MDM10407000] Failed to process transaction , java.lang.NumberFormatException: For input string: ""

- Add new library :
  . 

v1.0.48

- Add feature inside transaction output message , to know about the availability 
  of the next step with expect or not

- Add feature to create transaction interaction process , with requirements below :
  . no message out into provider
  . the response will straight away get from event ( synchronious )

- Add new library :
  . channel2beepcast-v1.0.26.jar
  . router2beepcast-v1.0.16.jar
  . provider2beepcast-v2.1.23.jar
  . client2beepcast-v2.2.07.jar
  . model2beepcast-v1.0.42.jar
  . subscriber2beepcast-v1.2.03.jar
  . beepcast_loadmanagement-v1.2.01.jar
  . beepcast_dbmanager-v1.1.22.jar

v1.0.47

- Found bug at resolve message identifier .

- Add new transaction process type 

    public static final int BULK = 2;

- Add feature to send bulk message :
  . the transaction still need to define client and event
  . the event can be used as the incoming / outgoing basic type
  . there is no session tracking
  . the balance deduction is performed during the transaction

- Add new library :
  . 

v1.0.46

- Will reject all incoming message when found client state as suspend

- Test and simulate transLogService.getMenuSelections function

- Give more info log on event support's library

- Add new library :
  . channel2beepcast-v1.0.24.jar
  . client2beepcast-v2.2.04.jar
  . model2beepcast-v1.0.40.jar
  . subscriber2beepcast-v1.2.00.jar
  . beepcast_keyword-v1.0.02.jar
  . beepcast_dbmanager-v1.1.20.jar
  . beepcast_encrypt-v1.0.00.jar

v1.0.45

- When doing the result it will dig up trans queue and log based on event id
  
  Will optimized the tables :
  
    ALTER TABLE `trans_queue` ADD INDEX `event_id`(`event_id`) ;
    ALTER TABLE `trans_log` ADD INDEX `event_id`(`event_id`) ;

- Fixing the poll event bugs 

  Because the system exclude the startDate and endDate , but the poll / survey
  will use the period date still .
  
  Force to exclude the period date filter when doing the result of survey .

- Add new library :
  . 

v1.0.44

- Support variant of country prefix number 
  1 digit = USA , RUSIA
  2 digit = Commons
  3 digit = Small Countries

- Add new library :
  . router2beepcast-v1.0.15.jar
  . beepcast_dbmanager-v1.1.19.jar
  . model2beepcast-v1.0.37.jar
  . provider2beepcast-v2.1.21.jar
  
v1.0.43

- Restructure the transaction class to please interaction process .

- The example of execution , below :

    // create transaction
    int transactionProcessType = TransactionProcessType.STANDARD;
    TransactionProcess transProcess = null;
    transProcess = TransactionProcessFactory.generateTransactionProcess(
        transactionProcessType , true );
    System.out.println( "trans process info = " + transProcess.info() );
    // run transaction and show result
    int processCode = transProcess.main( imsg );

- Add new library :
  . 

v1.0.42

- Add alias for process steps below :
  . REMINDER RSVP PENDING = REMINDER-RSVP PENDING
  . REMINDER RSVP YES = REMINDER-RSVP YES
  . NO REMINDER RSVP NO = NO REMINDER-RSVP NO

- Bypass to add reminder with past date from the text date .

- When found no reminder record and execute the one .
  it still consider result as true .
  
- Add new library :
  . reminder2beepcast-v1.0.04.jar

v1.0.41

- Change structure trans_queue table to put date of insert and update :

    ALTER TABLE `trans_queue`
      MODIFY COLUMN `date_tm` DATETIME ,
      ADD COLUMN `date_inserted` DATETIME AFTER `date_tm` ,
      ADD COLUMN `date_updated` DATETIME AFTER `date_inserted` ;

    UPDATE `trans_queue` SET 
      `date_inserted` = `date_tm` , `date_updated` = `date_tm` ;

    ALTER TABLE `trans_queue` 
      ADD COLUMN `provider_id` VARCHAR(45) AFTER `phone` ;
    
    ALTER TABLE `trans_log` 
      ADD COLUMN `provider_id` VARCHAR(45) AFTER `phone` ;
          
- Add feature to filter shared number for digging out the session .

- Additional feature to clean all expired session .

  The expiry period is configurable .
  
  Changed inside transaction xml :
  
    <sessionParam name="expiryDays" value="30" />
  
    <scheduleTask id="CleanExpirySubscriberSession"
      javacls="com.beepcast.model.transaction.scheduleTask.CleanExpirySubscriberSession"
      cronexp="0 0 0 * * ?" />

- Add new library :
  . beepcast_dbmanager-v1.1.18.jar

v1.0.40

- Support to process multiple steps

v1.0.39

- Fixed bugs :
  routerApp.getSendBufferService(); -> routerApp.getMTWorker().getSendBufferService() ;

- Add new library :
  . router2beepcast-v1.0.13.jar

v1.0.38

- Add reverse keyword :
  . <#TMSG_MESSAGE_CONTENT#>      : Transaction Message Content
  . <#TMSG_MESSAGE_ID#>           : Transaction Message Id
  . <#TMSG_ORIGINAL_ADDRESS#>     : Transaction Message Original Address
  . <#TMSG_DESTINATION_ADDRESS#>  : Transaction Message Destination Address

- Change feature inside reminder process step :
  . REMINDER
  . REMINDER RSVP PENDING
  . REMINDER RSVP YES
  . NO REMINDER
  . NO REMINDER RSVP NO

- Inside EMAIL_TO process step , support for multiple email address .
  And if there is no message response , will used the incoming message to forward .
  
- Add new library :
  . client_request2beepcast-v1.0.01.jar
  . model2beepcast-v1.0.36.jar
  . reminder2beepcast-v1.0.03.jar

v1.0.37

- Integrated channel session id into reminder function

- Use add reminder function with subscribe into the list ,
  so there is a track how many people do for reminder

- Take out deducation function inside reminder module

- Add keyword module :
  . will check at the keyword level first and then beepid and beepcode 
  
  . setup global properties
  
      <property field="Transaction.VerifyMessageCodeFromKeywordList" value="true"
        description="Verify message code from list of keyword" />
  
- Add new library :
  . beepcast_keyword-v1.0.01.jar  
  . reminder2beepcast-v1.0.02.jar
  . beepcast_dbmanager-v1.1.17.jar
  . subscriber2beepcast-v1.1.04.jar
  . channel2beepcast-v1.0.22.jar

v1.0.36

- Reject all incoming message when found suspended event .
  . still stored inside gateway log with message status FAILED-SUSPENDED

- Found a bugs inside menu item :

    01.09.2010 09.44.45:609 1053 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5930000] Trying to resolve next process bean
    01.09.2010 09.44.45:609 1054 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5930000] Found message request = beepcast
    01.09.2010 09.44.45:609 1055 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5930000] Define next step index from trans queue = 0
    01.09.2010 09.44.45:609 1057 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5930000] No closure event defined << ????
    01.09.2010 09.44.45:609 1058 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5930000] Defined candidate process bean from trans queue : index = 0 , type = PARAM , step = 1
    01.09.2010 09.44.45:609 1059 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5930000] Found next process type as param , will process menu item
    01.09.2010 09.44.45:609 1060 WARNING {RouterMOWorkerThread-0}Transaction [MDM5930000] Failed to process menu item , found empty parameters
    01.09.2010 09.44.45:625 1064 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5930000] Found as first step , processing the process type codes
    01.09.2010 09.44.45:625 1065 WARNING {RouterMOWorkerThread-0}Transaction [MDM5930000] Failed to resolved next process bean codes , found invalid process type = PARAM
    01.09.2010 09.44.45:625 1066 WARNING {RouterMOWorkerThread-0}Transaction [MDM5930000] Failed to resolved next process bean
    01.09.2010 09.44.45:625 1067 WARNING {RouterMOWorkerThread-0}Transaction [MDM5930000] Found empty next process step
    01.09.2010 09.44.45:625 1068 WARNING {RouterMOWorkerThread-0}Transaction [MDM5930000] Can not resolve menu choice , process as bogus message

- Add new library :
  . model2beepcast-v1.0.35.jar
 
v1.0.35

- Change inside feature provider to event mapping , 
  add pre and post validation
  
- Add new library :
  . beepcast_dbmanager-v1.1.16.jar
  . subscriber2beepcast-v1.1.03.jar

v1.0.34

- Take out send buffer service , module and bean .
  These will move into router module .

- Add additional param for sybase session id : SYBASE_SESSION_ID
  this field will put a value with <msg_id>XXXX</msg_id> from 
  incoming message from sybase provider .

- Add feature to process reminder process type :
  . required when the datetime message will send
  . the date time format using : YYYY-MM-DD HH:mm
  
- Change the flow of the reminder that the subscriber
  can also receive a reponse message "thanks..." instead of reminder message
  
- Add feature "no remind" service , in order to 
  take out the reminder message from the table .

- Integrated with reminder service module

- Put message params : RemindDateSent inside outgoing message
  in order to differentiate that this msg should be store 
  into reminder service first ( not directly to send buffer ) .
    
- Use and change the existing reminder service 
  . restructure table 
  
- Add new library :
  . reminder2beepcast-v1.0.00.jar
  . model2beepcast-v1.0.34.jar
  . router2beepcast-v1.0.12.jar
  . provider2beepcast-v2.1.20.jar

v1.0.33

- Add online properties to set to disable mo leg debits and store into gateway log

  There is a change inside oproperties xml file
  
      <property field="Transaction.BypassDebitMoLegForProviderIds" value="CLIENT_API"
        description="Bypass debit at mo leg for specific provider ids" />

- No mo leg billing charges for incoming message with provider = "CLIENT_API"

- Support with new feature from client library , that can push client message into client third party application

- Used integrated event bean , service , and dao .

- Add new library :
  . client2beepcast-v2.2.02.jar
  . provider2beepcast-v2.1.19.jar
  . model2beepcast-v1.0.31.jar

v1.0.32

- Found bugs :

  20.07.2010 15.28.27:968 1012 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Found message request = AEGN 1528
  20.07.2010 15.28.27:968 1013 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Define next step index from trans queue = 0
  20.07.2010 15.28.27:968 1015 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] No closure event defined
  20.07.2010 15.28.27:968 1016 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Defined candidate process bean from trans queue : index = 0 , type = CODE , step = 1
  20.07.2010 15.28.27:984 1017 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Found as first step , processing the process type codes
  20.07.2010 15.28.27:984 1018 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Defined scope process steps , step : total = 3 , left = 0 , right = 2 ; type names = [CODE : (N/A)] , [CODE ON WEEKDAYS : (N/A)] , [CODE ON WEEKEND : (N/A)] 
  20.07.2010 15.28.27:984 1019 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Extract to resolve next process bean based on codes
  20.07.2010 15.28.27:984 1020 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Found matched message request with process step : type = CODE ON WEEKDAYS , name = (N/A)
  20.07.2010 15.28.27:984 1021 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Found match bean process , with : step = 2 , names = (N/A) , message = Test Code Week 01 - Week Days
  20.07.2010 15.28.27:984 1022 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Finished extract scope of process beans
  20.07.2010 15.28.27:984 1023 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Found next process : type = CODE ON WEEKDAYS , response = Test Code Week 01 - Week Days
  20.07.2010 15.28.27:984 1024 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Trying to append trans queue parameters
  20.07.2010 15.28.27:984 1025 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Found process type names = [(N/A)]
  20.07.2010 15.28.27:984 1026 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Trying to handle special params
  20.07.2010 15.28.27:984 1027 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Append trans queue parameters = (N/A)=AEGN 1528
  20.07.2010 15.28.27:984 1028 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Trying to get pending event from mobile user profile
  20.07.2010 15.28.27:984 1029 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] There are no pending event
  20.07.2010 15.28.27:984 1030 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Trying to update response message , by replacing variables <%...%>
  20.07.2010 15.28.27:984 1031 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Trying to update response message , by replacing reserved variables <#...#>
  20.07.2010 15.28.27:984 1032 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM5067000] Replace reserved variables , found code = AEGN
  20.07.2010 15.28.27:984 1033 WARNING {RouterMOWorkerThread-0}Transaction [MDM5067000] Failed to replace variables, found blank rfa

  It suppose found as Code not Code On WeekDays
  
  Fixed , validated next step inside candidate process bean

- Apply new process step :
  . CODE ON WEEKDAYS - will process the code on weekdays
  . CODE ON WEEKEND - will process the code on weekend

v1.0.31

- Bypass code validation and apply module of "dedicated provider to specific event"

- add new library :
  . beepcast_dbmanager-v1.1.15.jar

v1.0.30

- Calculated total debit for long unicode message ( max each 140 characters ) .

- Add new library :
  . subscriber2beepcast-v1.1.02.jar

v1.0.29

- Found a bugs null pointer assign when send message with <#USER_NAME#>

    [MDM5053000] Prepare input message  , 
    originalAddress = +6590827925 , 
    destinationAddress = +6590102337 , 
    messageContent = adi 09.34 , 
    originalProvider = MODEM , 
    replyMessageContent = null , 
    priority = 5

    28.06.2010 10.03.41:687 562 DEBUG   {SendMessageThread-0}Transaction [INT382000] Found nextProcess : type = CODE , step = 1
    28.06.2010 10.03.41:687 563 DEBUG   {SendMessageThread-0}Transaction [INT382000] Found process type as not parsing word , just continue the process based on next step
    28.06.2010 10.03.41:687 564 DEBUG   {SendMessageThread-0}Transaction [INT382000] Found next process : type = CODE , response = Hi <#USER_NAME#>,\r\nThis message confirms that you have received a response from BEEPCAST for testing.
    28.06.2010 10.03.41:687 565 DEBUG   {SendMessageThread-0}Transaction [INT382000] Trying to append trans queue parameters
    28.06.2010 10.03.41:687 566 DEBUG   {SendMessageThread-0}Transaction [INT382000] Found event process type as CODE , bypass event append parameters
    28.06.2010 10.03.41:687 567 DEBUG   {SendMessageThread-0}Transaction [INT382000] Append trans queue parameters = 
    28.06.2010 10.03.41:687 568 DEBUG   {SendMessageThread-0}Transaction [INT382000] Trying to get pending event from mobile user profile
    28.06.2010 10.03.41:687 569 DEBUG   {SendMessageThread-0}Transaction [INT382000] There are no pending event
    28.06.2010 10.03.41:687 570 DEBUG   {SendMessageThread-0}Transaction [INT382000] Trying to update response message , by replacing variables <%...%>
    28.06.2010 10.03.41:687 571 DEBUG   {SendMessageThread-0}Transaction [INT382000] Bypass replace variables , found CODE type
    28.06.2010 10.03.41:687 572 DEBUG   {SendMessageThread-0}Transaction [INT382000] Trying to update response message , by replacing reserved variables <#...#>
    28.06.2010 10.03.41:687 573 WARNING {SendMessageThread-0}Transaction [INT382000] Failed to process transaction , java.lang.NullPointerException
  
  . Apply StringUtils.trimToEmpty before do StringUtils.replace function .
  . Ensure all not string return will force to convert to string , like datetime result .  
    
- Add new library :
  . model2beepcast-v1.0.29.jar
  . dwh_model-v1.0.09.jar

v1.0.28

- For Mo Message from Provider will do : ( in progress )

  . Put Destination Number as Provider's ShortCode

- Resolve how to send thru modem :

  . Change the provider criteria when request from modem 

  . Change the logic inside HttpSupport's getSendQueue method :
  
      provider = StringUtils.trimToEmpty( provider );
      if ( !provider.equals( "" ) ) {
        if ( provider.equalsIgnoreCase( "MODEM" ) ) {
          criteria += " AND ( provider like '%MDM%'  ) ";
        } else {
          criteria += " AND ( provider like '" + provider + "%' ) ";
        }
      }  

- add new library :
  . beepcast_dbmanager-v1.1.14.jar
  . client2beepcast-v2.1.11.jar
      
            
v1.0.27

- Integrate "JumpTo" event with client api .

- Apply new "JumpTo" with "NoReply" feature .

- Apply more log inside trans queue and log management .

v1.0.26

- Apply "Band Discount" factor to produce total credit cost ( << -- by pass it ) ... to be continued ...
  ( There is no relationship between "Band Discount" and "Credit Cost" ...

- Change inside transaction xml file

    <propertyProcessSteps>
      <propertyProcessStep name="EMAIL">
      </propertyProcessStep>
      <propertyProcessStep name="SMS_TO_EMAIL">
        <property name="addressFrom" value="${transaction.property-process-step.address-from}" />
      </propertyProcessStep>
      <propertyProcessStep name="EMAIL_CLIENT">
        <property name="addressFrom" value="${transaction.property-process-step.address-from}" />
      </propertyProcessStep>
      <propertyProcessStep name="EMAIL_TO">
        <property name="addressFrom" value="${transaction.property-process-step.address-from}" />
      </propertyProcessStep>
    </propertyProcessSteps>


- Apply "Credit Cost" deduction for every inbound and outbound transaction

- Rebuild country table and add "credit_cost" field , with sql below :

  ALTER TABLE `country` 
    ADD COLUMN `credit_cost` DOUBLE NOT NULL DEFAULT 1.0 AFTER `currency_code` ;

- add new library :
  . beepcast_dbmanager-v1.1.11.jar
  . beepcast_onm-v1.1.03.jar
  . subscriber2beepcast-v1.1.01.jar
  . model2beepcast-v1.0.28.jar
  . client_request2beepcast-v1.0.00.jar
  . provider2beepcast-v2.1.18.jar
  . channel2beepcast-v1.0.20.jar
    
v1.0.25

- add new process step , named : EMAIL_TO , the mechanism works is same with EMAIL_CLIENT ,
  but the destination is variable ( not from client's email ) and compose subject from
  summary of email content .

- update client configuration file , in order to configure the process step variable 
  and default property .

- verify about sending email , below :

  30.04.2010 15.08.28:453 1010 DEBUG   {EmailSenderThread}Email Sending email , host = localhost , from = services@beepcast.com , to = benny@beepcast.com , subject = HELLO
  30.04.2010 15.08.28:515 1011 DEBUG   {EmailSenderThread}EmailAuthenticator Authenticating emailUsername = benny@bcdev.com , emailPassword = benny
  30.04.2010 15.08.28:656 1012 DEBUG   {EmailSenderThread}Email Successfully sent email , host = localhost , from = services@beepcast.com , to = benny@beepcast.com , subject = HELLO
  
  . use client app to send email mechanism , 
    because it's already used queue and thread and more simplified . ( in progress )

- no need to perform mo debit when the message cames from api and no need to store into client msg api .

- change the way to store client message thru client app , by using client api .

- add new library :
  . client2beepcast-v2.1.08.jar

v1.0.24

- change the way of the client bean works , please use ClientService

- add new library :
  . model2beepcast-v1.0.26.jar

v1.0.23

- recompose http response for gateway log viewer :
  . add retry
  . add external status
  . add external message id

- break apart transaction support , especially for to resolve trans queue .

- add new library :
  . billing2beepcast-v1.1.03.jar
  . model2beepcast-v1.0.24.jar
  . beepcast_dbmanager-v1.1.09.jar
  
v1.0.22

- doesn't do un/subscribed in the channel log again .
  will use in subscriberApp api only .

- support for the new api from gateway log model

- add new library :
  . model2beepcast-v1.0.23.jar
  . subscriber2beepcast-v1.0.01.jar
  . dwh_model-v1.0.08.jar
  . channel2beepcast-v1.0.16.jar

v1.0.21

- block request when found the number is : 
  . not in the list of countries
  . not in the list of client_to_countries

- add configuration in the online properties :

      <property field="Transaction.RejectInvalidCountryMessage" value="true"
        description="Drop message with prefix country not in the country list" />
      <property field="Transaction.RejectUnregisterCountryMessage" value="true"
        description="Drop message with prefix country not in the map client country list" />
  
- add new library :
  . model2beepcast-v1.0.21.jar  
  . provider2beepcast-v2.1.16.jar
  . beepcast_dbmanager-v1.1.08.jar
  . dwh_model-v1.0.07.jar
    
v1.0.20

- use and integrate with the new subscriber module

- add new library :
  . subscriber2beepcast-v1.0.00.jar
  . model2beepcast-v1.0.19.jar
  . beepcast_online_properties-v1.0.00.jar

v1.0.19

- when subscriber do the lucky draw it will stop there , not continue to process :

  DEBUG {RouterMOWorkerThread-0}Transaction [MDM673000] Process message as Lucky Draw Type
  DEBUG {RouterMOWorkerThread-0}TransactionQueueDAO Perform select * from trans_queue where date_tm>='2019-10-22 15:26:35' and date_tm<'2019-10-22 15:26:35' and event_id in (1892,1893,1894) order by date_tm
  DEBUG {RouterMOWorkerThread-0}TransactionLogDAO Perform SELECT * FROM trans_log WHERE ( date_tm >= '2019-10-22 15:26:35' ) AND ( date_tm < '2019-10-22 15:26:35' ) AND event_id in (1892,1893,1894) ORDER BY date_tm 
  DEBUG {RouterMOWorkerThread-0}SendBufferDAO Perform INSERT INTO send_buffer (message_id,message_type,message_count,message,debit_amount,phone,provider,event_id,channel_session_id,senderID,priority,suspended,simulation,date_tm)VALUE ('',0,0,'There are no participants at this time.',0.0,'+628161410822','MODEM',0,0,'',0,0,0,'2009-10-22 15:27:00')
  DEBUG {RouterMOWorkerThread-0}TransactionQueueDAO Perform DELETE FROM trans_queue WHERE ( phone = '+628161410822' ) 
  DEBUG {RouterMOWorkerThread-0}EventDAO Perform SELECT * FROM event WHERE ( event_id = 1926 ) 
  DEBUG {RouterMOWorkerThread-0}TransactionLogDAO Perform INSERT INTO trans_log (client_id,event_id,next_step,catagory_id,date_tm,phone,message_count,code,params,jump_count,location_id,closed_reason_id) VALUES ( 1,1926,1,38,'2009-10-22 15:27:00','+628161410822',1,'AEOR','',0,0,0 ) 
  DEBUG {RouterMOWorkerThread-0}RouterMOWorker [MDM673000] Finish transaction process , resultCode = PROCESS_SUCCEED , take = 156 ms

v1.0.18

- transaction conf need a debug flag , put debug attribute in the transaction conf file .

- when receive message from the client api , shall not do the debit mo leg

- support cc and bcc email alert support 

- add new library :
  . client2beepcast-v2.1.06.jar
  . beepcast_onm-v1.1.02.jar
  . beepcast_dbmanager-v1.1.07.jar

v1.0.17

- there is a new feature to check the balance threshold ,
  it the client reach the balance limit the system shall send the email .
  
  . will be changed inside the transaction.xml to configure the balance limit threshold
  
- add new library :
  . beepcast_onm-v1.1.01.jar

v1.0.16

- put thread safe inside session mechanism

- add new library :
  . model2beepcast-v1.0.15.jar

v1.0.15

- support randomized beepcode

- add new library :
  . model2beepcast-v1.0.13.jar
  . beepcast_loadmanagement-v1.2.00.jar

v1.0.14

- add new process type named : unsubscribe_list .

v1.0.13

- verify the sign character for alias keyword

- change the log level in the expected client id

- every calling to do the changes or query with MobileUserBean , the system
  shall provide client id param , because every client has own mobile user data
  
- restructure mobile user :
  . MobileUserBean - bean
  . MobileUserFactory - factory
  . MobileUserService - service
  . MobileUserDAO - dao  

- process a new param from input transaction message , named : channelLogBean

- failed to process transaction with beepid requested .

- update the response message before going to the provider agent ,
  replace every symbol with it .

- add new library :
  . model2beepcast-v1.0.11.jar

v1.0.12

- add new library :
  . model2beepcast-v1.0.10.jar

- when there is a mt leg transaction with failed debit , it will still to insert into gateway log
  but used the status failed - no balance .  
  
- Add feature to differentiate message type , message count , total debit amount in the gateway log records 

  . execute sql below :
  
    ALTER TABLE `gateway_log` 
      ADD COLUMN `message_type` VARCHAR(20) DEFAULT 'SMS_TEXT' AFTER `phone` ,
      ADD COLUMN `message_count` INTEGER UNSIGNED DEFAULT 1 AFTER `message_type` ,
      ADD COLUMN `debit_amount` DOUBLE DEFAULT 1.0 AFTER `message` ;
      
    ALTER TABLE `send_buffer`
      ADD COLUMN `message_count` INTEGER UNSIGNED DEFAULT 1 AFTER `message_type` ,
      ADD COLUMN `debit_amount` DOUBLE DEFAULT 1.0 AFTER `message` ;
  
  . change the model beepcast based on the alter table changed above
  
  . restructure send_buffer schema
  
    DROP TABLE IF EXISTS `send_buffer` ;
    
    CREATE TABLE `send_buffer` (
      `send_id` bigint(10) NOT NULL auto_increment,
      
      `message_id` varchar(20) collate utf8_unicode_ci default NULL,
      `message_type` decimal(2,0) NOT NULL default 0,
      `message_count` int(10) unsigned default 1,
      `message` mediumtext collate utf8_unicode_ci NOT NULL,

      `debit_amount` double default '1',      
      `phone` varchar(20) collate utf8_unicode_ci NOT NULL,
      `provider` varchar(10) collate utf8_unicode_ci NOT NULL,
      `event_id` decimal(10,0) NOT NULL default 0,
      `channel_session_id` int(10) unsigned NOT NULL default 0,
      `senderID` varchar(15) collate utf8_unicode_ci NOT NULL default '',
      
      `priority` decimal(2,0) default '0',
      `suspended` decimal(1,0) NOT NULL default '0',
      `simulation` decimal(1,0) NOT NULL default '0',
      `retry` decimal(2,0) NOT NULL default '0',

      `date_tm` datetime default NULL,
      `date_send` datetime default NULL,
      
      PRIMARY KEY  (`send_id`),
      KEY `suspended` (`suspended`),
      KEY `provider` (`provider`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
      
  . restructure the send_buffer bean  
      
v1.0.11

- fix bugs can not send email

- create rules for specifig provider in order to support long message and let the provider recompile ( split )

  . change inside the provider conf file
  
    <providerFeatures>
      <providerFeature name="SplitLongSmsMessageByProvider">
        <provider id="EE1" enable="true" />
        <provider id="SB1" enable="true" />
      </providerFeature>
    </providerFeatures>
    
- when found long plain message ( > 160 characters ) and let the provider do the concatenating , the billing
  shall be multiple by total message will be send , use by divided 153 based on the reference doc .
  
v1.0.10

- validate outgoing message with is blank function

- validate is the phone number format

- switch the mt leg debit flow just before insert into send buffer ( end of the transaction flow )

- change the provider.xml configuration , add tag split under the EE provider agent

- add new library :

  . provider v2.1.13

v1.0.09

- update function subscribeMobileUser and unsubscribeMobileUser in the transaction event support

- when found event as channel but performing unsubscribed / subscribed it will use that channel ( not from the channel list )

- there is no charging for internal message ( original provider = internal )

- add routing to provider configurable :
  . routeOrder for priority provider id
  . routeProvider for mapping with inbound provider

v1.0.08

- when subscriber tax a code ( beepcode / beepid ) shall also matched 
  with dedicated modem to particular client map .

- add new library :
  . onm v1.1.00
  . loadmanagement v1.1.04
  . provider v2.1.12
  . channel v1.0.04

v1.0.07

- support for "SUBSCRIBE_LIST" process steps

- add new library :
  . model2beepcast v1.0.06

v1.0.06

- support for "CONTAIN_WORD" and "FIRST_WORD" process steps

v1.0.05

- add duration limitation during the session read ( trans_log )

- found when the current session is null and there is expected dedicate modem ,
  the system shall still finding inside the history session ( trans_log ) table

v1.0.04

- write every message content in the log with escape java format

- add new library :
  . provider v2.1.11

v1.0.03

- when perform payment ( dodebit ) 
  if the message from broadcast , the debit will goes to event prepaid account 

- add new library named : quartz and commons-collection

- add management feature inside the transaction , in order to perform :
  . execute for every first hour of the day
  . execute for every first day of the week
  . execute for every first day of the month
  
- add a new configuration file for transaction  

- add feature to perform client_track for every transaction
- add new dbmanager library v2.1.05
- add new billing library v1.1.01

v1.0.02

- change the query during switch between trans_log and trans_queue , because found a mysql order by bug

- bring the MT Billing on the transaction's end function call

- do the MT Billing depend when there is a message content or not 

- if found failed to perform debit MT Leg than the message not forwarded to the provider

v1.0.01

- for incoming message must have a feature to track the status of incoming message 

- add the new java model library v1.0.02

- add the new java billing library v1.0.03

- perform debit billing in two leg ( MO & MT ) , except for the broadcast request

- when generate messageId of output message based on input message

- found bug during switched between trans log and queue , always restart a new event with step 1 ( reset ) again .

  It suppose follow the latest step , between trans log and queue must save the last next step field .
  
  . execute sql below :
  
    ALTER TABLE `trans_log` 
      ADD COLUMN `next_step` DECIMAL(2,0) NOT NULL DEFAULT 1 AFTER `event_id` ;
      
  . update during the create new session from trans log , it will follow next_step also .

- found bug during switched between trans log and queue :

  19.05.2009 14.31.35:546 31512 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM192408] Found modemNumber = +6590102337
  19.05.2009 14.31.35:546 31513 DEBUG   {RouterMOWorkerThread-0}FetchModemNumberToClient_ModemNumber_Active Generated a new record TModemNumberToClient ( id = 1 modemNumber = +6590102337 clientId = 1 description = null active = true  )
  19.05.2009 14.31.35:546 31514 DEBUG   {RouterMOWorkerThread-0}FetchBase Added a new record in the data cache , with cacheKey = 0c8efc050ba55a9ed3deb10dd1de32db
  19.05.2009 14.31.35:546 31515 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM192408] Trying to match requested clientId with expected clientId = 1
  19.05.2009 14.31.35:546 31516 WARNING {RouterMOWorkerThread-0}Transaction [MDM192408] Failed to matched , found requested clientId = 152 , trying to find in the trans log
  19.05.2009 14.31.35:546 31517 DEBUG   {RouterMOWorkerThread-0}TransactionLogDAO Perform SELECT * FROM trans_log WHERE ( date_tm >= '2009-02-18 14:31:35' ) AND ( date_tm < '2009-05-19 14:31:35' ) AND ( closed_reason_id = 2 ) AND ( client_id = 1 ) ORDER BY log_id  DESC LIMIT 100 
  19.05.2009 14.31.35:546 31518 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM192408] Found candidate transLogId = 109 , will perform switch to current trans queue
  19.05.2009 14.31.35:546 31519 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM192408] Trying to switch the current session with old trans log
  19.05.2009 14.31.35:546 31520 DEBUG   {RouterMOWorkerThread-0}TransactionQueueDAO Perform DELETE FROM trans_queue WHERE ( phone = '+650000000001' ) 
  19.05.2009 14.31.35:562 31521 DEBUG   {RouterMOWorkerThread-0}EventDAO Perform select * from event where event_id=1859
  19.05.2009 14.31.35:562 31522 DEBUG   {RouterMOWorkerThread-0}TransactionLogDAO Perform INSERT INTO trans_log (client_id,event_id,catagory_id,date_tm,phone,message_count,code,params,jump_count,location_id,closed_reason_id) VALUES ( 152,1859,26,'2009-05-19 14:27:19','+650000000001',1,'ADADADYG','MENU1=mbuh',0,0,2 ) 
  19.05.2009 14.31.35:562 31523 DEBUG   {RouterMOWorkerThread-0}MobileUserDAO Perform SELECT * FROM mobile_user WHERE phone = '+650000000001' 
  19.05.2009 14.31.35:562 31524 DEBUG   {RouterMOWorkerThread-0}MobileUserDAO Perform update mobile_user set PASSWORD='475225',NAME='',EMAIL='',PERSONAL_BEEP_ID='',CLIENT_BEEP_ID='',BIRTH_DATE='2009-05-19 14:17:09',GENDER='',MARITAL_STATUS='',LAST_NAME='',LAST_CODE='ADADADYG',COMPANY_NAME='',IC='',MONTHLY_INCOME='',INDUSTRY='',OCCUPATION='',EDUCATION='',MOBILE_BRAND='',MOBILE_MODEL='',MOBILE_OPERATOR='',NUM_CHILDREN=0,COUNTRY='',DWELLING='',OFFICE_ZIP='',OFFICE_STREET='',OFFICE_UNIT='',OFFICE_BLK='',HOME_ZIP='',HOME_STREET='',HOME_UNIT='',HOME_BLK='',NATIONALITY='',SALUTATION='',CLIENT_DB_KEY_1='',CLIENT_DB_KEY_2='' where phone='+650000000001'
  19.05.2009 14.31.35:562 31525 DEBUG   {RouterMOWorkerThread-0}TransactionLogDAO Perform DELETE FROM trans_log WHERE ( log_id = 109 ) 
  19.05.2009 14.31.35:562 31526 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM192408] Creating a new session , with clone info params from trans log , transLogid = 109
  19.05.2009 14.31.35:562 31527 DEBUG   {RouterMOWorkerThread-0}TransactionQueueDAO Perform SELECT * FROM trans_queue WHERE ( phone = '+650000000100' ) ORDER BY date_tm ASC LIMIT 1 
  19.05.2009 14.31.35:562 31528 WARNING {RouterMOWorkerThread-0}Transaction [MDM192408] Failed to create a new session TransactionQueueBean ( eventID = 1711 clientID = 1 pendingEventID = 0 dateTm = Tue May 19 14:17:14 SGT 2009 phone = +650000000100 nextStep = 1 messageCount = 1 code = ADADADKC pendingCode = null params =  updateProfile = false newUser = false jumpCount = 0 locationID = 0 callingEventID = 0  )
  19.05.2009 14.31.35:562 31529 DEBUG   {RouterMOWorkerThread-0}GatewayLogDAO Perform INSERT INTO gateway_log ( status , external_status , message_id , event_id , provider , date_tm , mode , phone , message , short_code ) VALUES( '','','MDM192408',0,'MODEM','2009-05-19 14:31:35','RECV','+650000000001','mbuh','+6590102337' )
  19.05.2009 14.31.35:562 31530 DEBUG   {RouterMOWorkerThread-0}Transaction [MDM192408] Successfully log input message into gateway log
  19.05.2009 14.31.35:562 31531 WARNING {RouterMOWorkerThread-0}Transaction [MDM192408] Can not resolve trans queue object , process as bogus message

  Found solution , there invalid sql query 
  
    SELECT * FROM trans_log WHERE ( date_tm >= '2009-02-18 14:31:35' ) AND ( date_tm < '2009-05-19 14:31:35' ) 
    AND ( closed_reason_id = 2 ) AND ( client_id = 1 ) ORDER BY log_id  DESC LIMIT 100 
    
  There shall be include criteria which phoneNumber .
  
    ... AND ( phone = '' ) ...  

- add logTransaction with default reason closed id = 0 

- create optional constructor without any parameter , to support beepadmin gui

- prepare the test transaction unit

- Restructure the transaction flow 

- Add primary key in the bogus request message transaction 

  . execute sql below :

    ALTER TABLE `bogus_request`
      ADD COLUMN `id` INTEGER AUTO_INCREMENT PRIMARY KEY FIRST ;
            
- added an intelligent unsubscribed system  

  . add primary key in the trans_log table , execute sql below :
      
    ALTER TABLE `trans_log`
      DROP PRIMARY KEY , ROW_FORMAT = DYNAMIC ;
    
    ALTER TABLE `trans_log`
      ADD COLUMN `log_id` INTEGER AUTO_INCREMENT PRIMARY KEY FIRST ;
    
    CREATE INDEX `phone` ON `trans_log` ( `phone` ) ;
        
  . add new field named "closed_reason_id" in the trans_log table ,
    execute sql below :
    
    ALTER TABLE `trans_log` 
      ADD COLUMN `closed_reason_id` INTEGER NOT NULL DEFAULT 0 AFTER `code` ;

  . if found expected code , it will check :
    - based on dedicated number mapping to client
    - if use the different client it will lookup in the trans log
    - if found in the trans log it will perform the switch between trans queue and log
    - if not found than still use the current trans queue as the correct session          
  
v1.0.00

- added a new java library named provider2beepcast-v2.1.10.jar

- added a new java library named model2beepcast-v1.0.00.jar

- added a new java library named util2beepcast-v1.0.00.jar

- Just copy com.beepcast.model.transaction.* classes from beepadmin

