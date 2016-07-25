package com.beepcast.model.transaction;

import java.util.LinkedList;

public interface Process {

  public int begin( TransactionInputMessage imsg , LinkedList omsgs );

  public int run( TransactionInputMessage imsg , LinkedList omsgs );

  public int end( TransactionInputMessage imsg , LinkedList omsgs );

}
