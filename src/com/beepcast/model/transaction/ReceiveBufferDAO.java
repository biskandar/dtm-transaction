package com.beepcast.model.transaction;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

import com.beepcast.database.ConnectionWrapper;
import com.beepcast.database.DatabaseLibrary;
import com.firsthop.common.log.DLog;
import com.firsthop.common.log.DLogContext;
import com.firsthop.common.log.SimpleContext;

/*******************************************************************************
 * Receive Buffer DAO.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class ReceiveBufferDAO {

  static final DLogContext lctx = new SimpleContext( "ReceiveBufferDAO" );

  private DatabaseLibrary dbLib = DatabaseLibrary.getInstance();

  /*****************************************************************************
   * Insert a new receive buffer record.
   * <p>
   * 
   * @param receiveRecord
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void insert( ReceiveBufferBean receiveRecord ) throws IOException {

    /*--------------------------
      override existing phone
    --------------------------*/
    String phone = receiveRecord.getPhone();
    ReceiveBufferBean receiveRec = select( phone );
    if ( receiveRec != null )
      delete( receiveRec );

    /*--------------------------
      build SQL string
    --------------------------*/
    String sql = "insert into receive_buffer "
        + "(PHONE,MESSAGE,SIMULATION,SENDERID) " + "values ( '"
        + StringEscapeUtils.escapeSql( phone ) + "','"
        + StringEscapeUtils.escapeSql( receiveRecord.getMessage() ) + "','"
        + StringEscapeUtils.escapeSql( receiveRecord.getSenderID() ) + "',"
        + ( ( receiveRecord.isSimulation() == true ) ? 1 : 0 ) + " ) ";

    /*--------------------------
      execute query
    --------------------------*/
    DLog.debug( lctx , "Perform " + sql );
    dbLib.executeQuery( "transactiondb" , sql );
    // new SQL().executeUpdate( sql );

  } // insert()

  /*****************************************************************************
   * Select a receive buffer record.
   * <p>
   * 
   * @param phone
   * @return ReceiveBufferBean instance if found, null if not found.
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public ReceiveBufferBean select( String phone ) throws IOException {

    ReceiveBufferBean receiveRecord = null;

    /*--------------------------
      build SQL string
    --------------------------*/
    String sql = "SELECT * FROM receive_buffer WHERE phone = '"
        + StringEscapeUtils.escapeSql( phone ) + "' ";

    /*--------------------------
      execute query
    --------------------------*/
    ConnectionWrapper conn = dbLib.getReaderConnection( "profiledb" );
    try {
      Statement stmt = conn.createStatement();

      DLog.debug( lctx , "Perform " + sql );
      ResultSet rs = stmt.executeQuery( sql );
      while ( rs.next() ) {
        receiveRecord = populateBean( rs );
      }
      rs.close();
      stmt.close();
    } catch ( SQLException sqle ) {
      throw new IOException( sqle.getMessage() + " SQL=" + sql );
    } finally {
      conn.disconnect( true );
    }

    return receiveRecord;

  } // select(string)

  /*****************************************************************************
   * Select all receive buffer records.
   * <p>
   * 
   * @return Array of ReceiveBufferBean
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public ReceiveBufferBean[] select() throws IOException {

    ReceiveBufferBean receiveRecords[] = null;
    Vector vRecords = new Vector( 10 );

    /*--------------------------
      build SQL string
    --------------------------*/
    String sql = "SELECT * FROM receive_buffer ";

    /*--------------------------
      execute query
    --------------------------*/
    ConnectionWrapper conn = dbLib.getReaderConnection( "profiledb" );
    try {
      Statement stmt = conn.createStatement();

      DLog.debug( lctx , "Perform " + sql );
      ResultSet rs = stmt.executeQuery( sql );
      while ( rs.next() ) {
        ReceiveBufferBean receiveRecord = populateBean( rs );
        vRecords.addElement( receiveRecord );
      }
      rs.close();
      stmt.close();
    } catch ( SQLException sqle ) {
      throw new IOException( sqle.getMessage() + " SQL=" + sql );
    } finally {
      conn.disconnect( true );
    }

    /*--------------------------
      build array of receive records
    --------------------------*/
    receiveRecords = new ReceiveBufferBean[vRecords.size()];
    for ( int i = 0 ; i < receiveRecords.length ; i++ )
      receiveRecords[i] = (ReceiveBufferBean) vRecords.elementAt( i );

    return receiveRecords;

  } // select()

  /*****************************************************************************
   * Delete receive buffer record.
   * <p>
   * 
   * @param ReceiveBufferBean
   * @throws IOException
   *           if database connection problem.
   ****************************************************************************/
  public void delete( ReceiveBufferBean receiveRecord ) throws IOException {

    long receiveID = receiveRecord.getReceiveID();

    /*--------------------------
      build SQL string
    --------------------------*/
    String sql = "DELETE FROM receive_buffer WHERE receive_id = " + receiveID;

    /*--------------------------
      execute query
    --------------------------*/
    DLog.debug( lctx , "Perform " + sql );
    dbLib.executeQuery( "transactiondb" , sql );
    // new SQL().executeUpdate( sql );

  } // delete()

  /*****************************************************************************
   * Populate receive buffer bean.
   * <p>
   ****************************************************************************/
  private ReceiveBufferBean populateBean( ResultSet rs ) throws SQLException {
    ReceiveBufferBean receiveRecord = new ReceiveBufferBean();
    receiveRecord.setReceiveID( (long) rs.getDouble( "receive_id" ) );
    receiveRecord.setPhone( rs.getString( "phone" ) );
    receiveRecord.setMessage( rs.getString( "message" ) );
    receiveRecord.setSimulation( ( rs.getDouble( "simulation" ) == 1 ) ? true
        : false );
    receiveRecord.setSenderID( rs.getString( "senderID" ) );
    return receiveRecord;

  }

}
