package com.beepcast.model.transaction;

public class GSM7BitCharsets {

  private static final char[][] charsBasic = {
      { '@' , ' ' , ' ' , '0' , ' ' , 'P' , '¿' , 'p' } ,
      { '£' , '_' , '!' , '1' , 'A' , 'Q' , 'a' , 'q' } ,
      { '$' , 'Φ' , '"' , '2' , 'B' , 'R' , 'b' , 'r' } ,
      { '¥' , 'Γ' , '#' , '3' , 'C' , 'S' , 'c' , 's' } ,
      { 'è' , 'Λ' , '¤' , '4' , 'D' , 'T' , 'd' , 't' } ,
      { 'é' , 'Ω' , '%' , '5' , 'E' , 'U' , 'e' , 'u' } ,
      { 'ù' , 'Π' , '&' , '6' , 'F' , 'V' , 'f' , 'v' } ,
      { 'ì' , 'Ψ' , '\'' , '7' , 'G' , 'W' , 'g' , 'w' } ,
      { 'ò' , 'Σ' , '(' , '8' , 'H' , 'X' , 'h' , 'x' } ,
      { 'Ç' , 'Θ' , ')' , '9' , 'I' , 'Y' , 'i' , 'y' } ,
      { '\n' , 'Ξ' , '*' , ':' , 'J' , 'Z' , 'j' , 'z' } ,
      { 'Ø' , ' ' , '+' , ';' , 'K' , 'Ä' , 'k' , 'ä' } ,
      { 'ø' , 'Æ' , ',' , '<' , 'L' , 'Ö' , 'l' , 'ö' } ,
      { '\r' , 'æ' , '-' , '=' , 'M' , 'Ñ' , 'm' , 'ñ' } ,
      { 'Å' , 'ß' , '.' , '>' , 'N' , 'Ü' , 'n' , 'ü' } ,
      { 'å' , 'É' , '/' , '?' , 'O' , '§' , 'o' , 'à' } };

  private static final char[][] charsExtension = {
      { ' ' , ' ' , ' ' , ' ' , '|' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , '^' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , '€' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , '{' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , '}' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , '[' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , '~' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , ' ' , ']' , ' ' , ' ' , ' ' , ' ' } ,
      { ' ' , ' ' , '\\' , ' ' , ' ' , ' ' , ' ' , ' ' } };

  public static int isContain( char ch ) {
    for ( int i = 0 ; i < 8 ; i++ ) {
      for ( int j = 0 ; j < 16 ; j++ ) {
        if ( ch == charsBasic[j][i] ) {
          return 1;
        }
      }
    }
    for ( int i = 0 ; i < 8 ; i++ ) {
      for ( int j = 0 ; j < 16 ; j++ ) {
        if ( charsExtension[j][i] == ' ' ) {
          continue;
        }
        if ( ch == charsExtension[j][i] ) {
          return 2;
        }
      }
    }
    return 0;
  }

  public static int length( String strIn ) {
    int len = 0;
    if ( strIn == null ) {
      len = -1;
      return len;
    }
    if ( strIn.equals( "" ) ) {
      return len;
    }
    int idx , strInLen = strIn.length();
    for ( idx = 0 ; idx < strInLen ; idx++ ) {
      int val = isContain( strIn.charAt( idx ) );
      if ( val < 1 ) {
        len = -1;
        return len;
      }
      len = len + val;
    }
    return len;
  }

  public static boolean isValid( String strIn ) {
    return length( strIn ) > -1;
  }

  public static byte[] encode( String strIn ) {

    // this function still not support for
    // char extensions ...

    byte[] bytes = null;
    if ( strIn == null ) {
      return bytes;
    }
    int strInLen = strIn.length();
    bytes = new byte[strInLen];
    for ( int idx = 0 ; idx < strInLen ; idx++ ) {
      char chIn = strIn.charAt( idx );
      for ( byte i = 0 ; i < 8 ; i++ ) {
        for ( byte j = 0 ; j < 16 ; j++ ) {
          if ( chIn == charsBasic[j][i] ) {
            bytes[idx] = (byte) ( ( i << 4 ) | j );
          }
        }
      }
    }
    return bytes;
  }

  public static String encodeQueryString( String strIn ) {
    String strOu = null;
    if ( strIn == null ) {
      return strOu;
    }
    byte[] bytesOu = encode( strIn );
    StringBuffer sb = new StringBuffer();
    for ( int idx = 0 ; idx < bytesOu.length ; idx++ ) {
      byte byteOu = bytesOu[idx];
      if ( ( byteOu >= '0' ) && ( byteOu <= '9' ) ) {
        sb.append( (char) byteOu );
        continue;
      }
      if ( ( byteOu >= 'A' ) && ( byteOu <= 'Z' ) ) {
        sb.append( (char) byteOu );
        continue;
      }
      if ( ( byteOu >= 'a' ) && ( byteOu <= 'z' ) ) {
        sb.append( (char) byteOu );
        continue;
      }
      if ( byteOu == ' ' ) {
        sb.append( '+' );
        continue;
      }
      String byteOuStr = Integer.toHexString( byteOu );
      if ( byteOuStr == null ) {
        continue;
      }
      byteOuStr = byteOuStr.toUpperCase();
      if ( byteOuStr.length() < 2 ) {
        sb.append( "%0" );
      } else {
        sb.append( "%" );
      }
      sb.append( byteOuStr );
    }
    strOu = sb.toString();
    return strOu;
  }

}
