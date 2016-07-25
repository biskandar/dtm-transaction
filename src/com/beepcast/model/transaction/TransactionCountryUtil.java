package com.beepcast.model.transaction;

import org.apache.commons.lang.StringUtils;

import com.beepcast.dbmanager.common.CountryCommon;
import com.beepcast.dbmanager.table.TCountry;

public class TransactionCountryUtil {

  public static int getCountryId( String phoneNumber ) {
    int countryId = 0;

    TCountry countryOut = getCountryBean( phoneNumber );
    if ( countryOut == null ) {
      return countryId;
    }

    countryId = countryOut.getId();

    return countryId;
  }

  public static TCountry getCountryBean( String phoneNumber ) {
    TCountry countryBean = null;

    // verify phoneNumber
    if ( StringUtils.isBlank( phoneNumber ) ) {
      return countryBean;
    }

    // clean phoneNumber
    if ( StringUtils.startsWith( phoneNumber , "+" ) ) {
      phoneNumber = phoneNumber.substring( 1 );
    }

    // set array phone prefix digits
    int[] arrPhonePrefixDigits = new int[] { 2 , 1 , 3 };

    // searching thru all the digits
    for ( int i = 0 ; i < arrPhonePrefixDigits.length ; i++ ) {

      // get prefix
      String phonePrefix = StringUtils.left( phoneNumber ,
          arrPhonePrefixDigits[i] );

      // validate prefix country
      if ( StringUtils.isBlank( phonePrefix ) ) {
        continue;
      }

      // generate countryBean based on phoneNumber prefix
      countryBean = CountryCommon.getCountryByPhonePrefix( phonePrefix );
      if ( countryBean == null ) {
        continue;
      }

      break;
    }

    return countryBean;
  }

}
