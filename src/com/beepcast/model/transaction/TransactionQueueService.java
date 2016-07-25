package com.beepcast.model.transaction;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

public class TransactionQueueService {

  private TransactionQueueDAO dao;

  public TransactionQueueService() {
    dao = new TransactionQueueDAO();
  }

  public boolean insert( TransactionQueueBean bean ) {
    return dao.insert( bean );
  }

  public TransactionQueueBean select( String phone ) {
    return dao.select( phone , -1 );
  }

  public TransactionQueueBean select( String phone , int eventId ) {
    return dao.select( phone , eventId );
  }

  public Vector select( Date fromDate , Date toDate , String criteria )
      throws IOException {
    return dao.select( fromDate , toDate , criteria , false , 1000 );
  }

  public Vector select( Date fromDate , Date toDate , String criteria ,
      boolean desc ) throws IOException {
    return dao.select( fromDate , toDate , criteria , desc , 1000 );
  }

  public Vector select( Date fromDate , Date toDate , String criteria ,
      boolean desc , int limit ) throws IOException {
    return dao.select( fromDate , toDate , criteria , desc , limit );
  }

  public boolean update( TransactionQueueBean bean ) {
    return dao.update( bean );
  }

  public boolean delete( TransactionQueueBean bean ) {
    return dao.delete( bean );
  }

  public int delete( String criteria , int limit ) {
    return dao.delete( criteria , limit );
  }

  public int getMessageCount( long clientID , long eventID , Date fromDate ,
      Date toDate , int iter ) throws IOException {
    return dao.getMessageCount( clientID , eventID , fromDate , toDate , iter ,
        null );
  }

  public int getMessageCount( long clientID , long eventID , Date fromDate ,
      Date toDate , int iter , boolean jumpTo ) throws IOException {
    return dao.getMessageCount( clientID , eventID , fromDate , toDate , iter ,
        new Boolean( jumpTo ) );
  }

  public int getRowCount( long clientID , long eventID , Date fromDate ,
      Date toDate , String code ) throws IOException {
    return dao.getRowCount( clientID , eventID , fromDate , toDate , code ,
        null );
  }

  public int getRowCount( long clientID , long eventID , Date fromDate ,
      Date toDate , String code , boolean jumpTo ) throws IOException {
    return dao.getRowCount( clientID , eventID , fromDate , toDate , code ,
        new Boolean( jumpTo ) );
  }

  public int getMobileUserCount( long clientID , long eventID , Date fromDate ,
      Date toDate ) throws IOException {
    return dao.getMobileUserCount( clientID , eventID , fromDate , toDate );
  }

  public Vector getMobileUsers( long clientID , long eventID , Date fromDate ,
      Date toDate ) throws IOException {
    return dao.getMobileUsers( clientID , eventID , fromDate , toDate , null );
  }

  public Vector getMobileUsers( long clientID , long eventID , Date fromDate ,
      Date toDate , String code ) throws IOException {
    return dao.getMobileUsers( clientID , eventID , fromDate , toDate , code );
  }

}
