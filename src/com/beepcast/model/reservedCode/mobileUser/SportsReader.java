package com.beepcast.model.reservedCode.mobileUser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import com.beepcast.model.reservedCode.ReservedCodeBean;
import com.beepcast.util.StrTok;

/*******************************************************************************
 * Sports Reader.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class SportsReader {

  private String response = "";

  /*****************************************************************************
   * Constructor.
   * <p>
   ****************************************************************************/
  public SportsReader( ReservedCodeBean reservedCode ) {
    String params[] = reservedCode.getParams();
    String team = "";
    if ( params.length == 2 )
      team = params[1];
    if ( params[0].equalsIgnoreCase( "mlb" ) )
      getMlbScores( team );
  }

  /*****************************************************************************
   * Get mlb sports scores from cnnsi.com.
   * <p>
   ****************************************************************************/
  private void getMlbScores( String team ) {

    Vector games = new Vector( 20 , 20 );

    try {
      /*---------------------------
        connect to web server
      ---------------------------*/
      URL url = new URL(
          "http://sportsillustrated.cnn.com/baseball/mlb/scoreboards/today" );
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setRequestMethod( "POST" );
      httpConn.setDoInput( true );
      httpConn.setDoOutput( true );
      httpConn.setUseCaches( false );

      /*---------------------------
        open input stream from web server
      ---------------------------*/
      BufferedReader in = new BufferedReader( new InputStreamReader(
          httpConn.getInputStream() ) );
      String line = "";

      /*---------------------------
        locate game
      ---------------------------*/
      while ( ( line = in.readLine() ) != null ) {
        if ( line.indexOf( "Final" ) != -1 || line.indexOf( "Top" ) != -1
            || line.indexOf( "Bottom" ) != -1 || line.indexOf( " ET" ) != -1
            || line.indexOf( "Delayed" ) != -1
            || line.indexOf( "Postponed" ) != -1 ) {

          // create game
          Game game = new Game();

          /*---------------------------
            locate game state
          ---------------------------*/
          StrTok st = new StrTok( line , ">" );
          st.nextTok();
          String temp = st.nextTok( "<" );
          temp = ( temp.startsWith( "Final" ) ) ? "Final" : temp;
          game.state = temp;

          /*---------------------------
            for each team ....
          ---------------------------*/
          for ( int i = 0 ; i < 2 ; i++ ) {

            /*---------------------------
              locate team
            ---------------------------*/
            while ( ( line = in.readLine() ) != null ) {
              int p1 = line.indexOf( "mlb/teams/" );
              if ( p1 != -1 ) {
                st = new StrTok( line.substring( p1 + 10 ) , "/" );
                temp = st.nextTok();
                temp = temp.substring( 0 , 1 ).toUpperCase()
                    + temp.substring( 1 );
                temp = temp.replace( '_' , ' ' );
                game.teams[i] = temp;
                break;
              }
            }

            /*---------------------------
              locate score
            ---------------------------*/
            while ( ( line = in.readLine() ) != null ) {
              if ( line.indexOf( "cnnSDScoreY" ) != -1 ) {
                st = new StrTok( line , ">" );
                st.nextTok();
                temp = st.nextTok( "<" );
                if ( temp.indexOf( "nbsp" ) != -1 )
                  temp = "";
                game.scores[i] = temp;
                break;
              }
            }
          }

          // add game
          games.addElement( game );
        }
      }

      /*---------------------------
        build response
      ---------------------------*/
      for ( int i = 0 ; i < games.size() ; i++ ) {
        Game game = (Game) games.elementAt( i );
        response += game.state + "\n";
        for ( int t = 0 ; t < 2 ; t++ ) {
          response += game.teams[t] + " ";
          response += game.scores[t] + "\n";
        }
        response += "\n";
        if ( !team.equals( "" ) ) {
          if ( game.teams[0].toUpperCase().startsWith( team.toUpperCase() )
              || game.teams[1].toUpperCase().startsWith( team.toUpperCase() ) )
            break;
          else
            response = "";
        }
      }
      if ( response.length() > 0 )
        response = response.substring( 0 , response.length() - 1 );
      else
        response = "No game";

      // close input stream
      in.close();

      /*---------------------------
        handle exceptions
      ---------------------------*/
    } catch ( Exception e ) {
    }

  }

  /*****************************************************************************
   * Get mlb sports scores from cnnsi.com.
   * <p>
   ****************************************************************************/
  public String getScores() {

    return response;

  } // getScores()

  /*
   * ============================================================= game
   * =============================================================
   */
  private class Game {
    String state = "";
    String teams[] = new String[2];
    String scores[] = new String[2];
  }

  /*
   * ============================================================= game
   * =============================================================
   */
  public static void main( String argv[] ) {

    if ( argv.length < 1 || argv.length > 2 ) {
      System.out.println( "\nUsage: SportsReader <sport> [team]\n" );
      System.out.println( "<sport> = mlb,nfl,nba\n" );
      System.out.println( "Example: SportsReader mlb (for all scores)" );
      System.out
          .println( "Example: SportsReader mlb gi (for team that start with \"gi\")" );
      System.exit( 0 );
    }

    String team = "";
    if ( argv.length == 2 )
      team = argv[1];

    String params[] = new String[2];
    params[0] = argv[0];
    params[1] = team;
    ReservedCodeBean reservedCode = new ReservedCodeBean();
    reservedCode.setParams( params );

    SportsReader sr = new SportsReader( reservedCode );
    System.out.println( sr.getScores() );

  }

} // eof

