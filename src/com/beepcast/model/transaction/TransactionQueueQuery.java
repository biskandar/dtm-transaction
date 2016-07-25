package com.beepcast.model.transaction;

import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.beepcast.database.DatabaseLibrary;
import com.beepcast.database.DatabaseLibrary.QueryItem;
import com.beepcast.database.DatabaseLibrary.QueryResult;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

public class TransactionQueueQuery {

  static final DLogContext lctx = new SimpleContext( "TransactionQueueQuery" );

  public static int getIntFirstValueRecord( String sql ) {
    int result = -1;
    try {
      result = Integer.parseInt( getStrFirstValueRecord( sql ) );
    } catch ( NumberFormatException e ) {
    }
    return result;
  }

  public static String getStrFirstValueRecord( String sql ) {
    String result = null;
    if ( StringUtils.isBlank( sql ) ) {
      return result;
    }

    // DLog.debug( lctx , "Perform " + sql );
    QueryResult qr = DatabaseLibrary.getInstance().simpleQuery(
        "transactiondb" , sql );
    if ( qr == null ) {
      return result;
    }

    Iterator it = qr.iterator();
    if ( !it.hasNext() ) {
      return result;
    }

    QueryItem qi = (QueryItem) it.next();
    if ( qi == null ) {
      return result;
    }

    result = qi.getFirstValue();
    return result;
  }

  public static Vector getStrFirstValueRecords( String sql ) {
    Vector result = new Vector();

    if ( StringUtils.isBlank( sql ) ) {
      return result;
    }

    // DLog.debug( lctx , "Perform " + sql );
    QueryResult qr = DatabaseLibrary.getInstance().simpleQuery(
        "transactiondb" , sql );
    if ( qr == null ) {
      return result;
    }

    Iterator it = qr.iterator();
    while ( it.hasNext() ) {
      QueryItem qi = (QueryItem) it.next();
      if ( qi == null ) {
        continue;
      }
      result.add( qi.getFirstValue() );
    }

    return result;
  }

}
