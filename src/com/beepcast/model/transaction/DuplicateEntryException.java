package com.beepcast.model.transaction;

public class DuplicateEntryException extends Exception {

  public DuplicateEntryException( String reason ) {
    super( reason );
  }

  public DuplicateEntryException() {
    super();
  }

}
