package com.beepcast.model.reservedCode.mobileUser;

import java.io.IOException;

import com.beepcast.model.mobileUser.MobileUserBean;
import com.beepcast.model.mobileUser.MobileUserService;
import com.beepcast.model.reservedCode.ReservedCodeBean;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

/*******************************************************************************
 * BEEPid.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class BeepID {

  static final DLogContext lctx = new SimpleContext( "BeepID" );

  private int clientId;
  private String phone;

  /*****************************************************************************
   * Constructor.
   * <p>
   ****************************************************************************/
  public BeepID( int clientId , ReservedCodeBean reservedCode ) {
    this.clientId = clientId;
    phone = reservedCode.getPhone();
  }

  /*****************************************************************************
   * Get BEEPid.
   * <p>
   * 
   * @return The starting menu.
   ****************************************************************************/
  public String getBeepID() throws IOException {

    String beepID = "";

    /*-----------------------
      get beep id from mobile user bean
    -----------------------*/
    try {
      MobileUserService service = new MobileUserService();
      MobileUserBean bean = service.select( clientId , phone );

      // initialize new mobile user
      if ( bean == null ) {
        DLog.warning( lctx , "Failed to get beep id "
            + ", found phone not in the mobile user = " + phone );
        return beepID;
      }

      String personalBeepID = bean.getPersonalBeepID();
      String companyBeepID = bean.getClientBeepID();

      // build beep id return string
      beepID += "Personal BEEPid: " + personalBeepID;
      if ( companyBeepID != null && !companyBeepID.equals( "" ) ) {
        beepID += "\nCompany BEEPid: " + companyBeepID;
      }

      /*-----------------------
        handle exceptions
      -----------------------*/
    } catch ( Exception e ) {
      throw new IOException( "BeepID.getBeepID(): " + e.getMessage() );
    }

    // success
    return beepID;

  } // getBeepcastInfo()

} // eof

