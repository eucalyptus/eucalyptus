package com.eucalyptus.auth.ldap;

import java.util.Stack;

public class LdapFilter {
  
  public enum Op {
    AND( "&" ),
    OR( "|" ),
    NOT( "!" );
    
    private final String str;
    
    private Op( String v ) {
      this.str = v;
    }
    
    public String toString( ) {
      return str;
    }
  }
  
  public enum Type {
    EQUAL( "=" ),
    APPROX( "~=" ),
    GE( ">=" ),
    LE( "<=" );
    
    private final String str;
    
    private Type( String v ) {
      this.str = v;
    }
    
    public String toString( ) {
      return str;
    }
  }
  
  private StringBuilder sb = new StringBuilder( );
  
  private Stack<Op> openOps = new Stack<Op>( );
  
  private int currentNumOfOperands;
  
  public LdapFilter opBegin( Op op ) {
    sb.append( "(" + op.toString( ) );
    openOps.push( op );
    currentNumOfOperands = 0;
    return this;
  }
  
  public LdapFilter opEnd( ) {
    if ( currentNumOfOperands == 0 ) {
      throw new RuntimeException( "Operator " + openOps.peek( ).toString( ) + " ends with no operands" );
    }
    sb.append( ')' );
    openOps.pop( );
    return this;
  }
  
  public LdapFilter operand( Type equal, String left, String right ) {
    if ( openOps.size( ) < 1 ) {
      throw new RuntimeException( "No operator to add operand to");
    }
    sb.append( '(' );
    sb.append( left );
    sb.append( equal.toString( ) );
    sb.append( right );
    sb.append( ')' );
    currentNumOfOperands++;
    return this;
  }
  
  public String toString( ) {
    if ( openOps.size() > 0 ) {
      throw new RuntimeException("Invalid filter expression: " + openOps.size( ) + " operators left open" );
    }
    return sb.toString( );
  }
  
  public static void main( String[] args ) {
    LdapFilter filter = new LdapFilter( );
    filter.opBegin( Op.OR ).opBegin( Op.AND ).operand( Type.EQUAL, "cn", "group" ).operand( Type.EQUAL, "timestamp", "1" ).opEnd( ).opEnd( );
    System.out.println( "Filter = " + filter.toString( ) );
  }
}
