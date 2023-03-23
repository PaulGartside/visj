////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 28 Jun 2018 Paul J. Gartside                                 //
////////////////////////////////////////////////////////////////////////////////
// Permission is hereby granted, free of charge, to any person obtaining a    //
// copy of this software and associated documentation files (the "Software"), //
// to deal in the Software without restriction, including without  limitation //
// the rights to use, copy, modify, merge, publish, distribute, sublicense,   //
// and/or sell copies of the Software, and to permit persons to whom the      //
// Software is furnished to do so, subject to the following conditions:       //
//                                                                            //
// The above copyright notice and this permission notice shall be included in //
// all copies or substantial portions of the Software.                        //
//                                                                            //
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR //
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   //
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL    //
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER //
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    //
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER        //
// DEALINGS IN THE SOFTWARE.                                                  //
////////////////////////////////////////////////////////////////////////////////

class Highlight_MIB extends Highlight_Base
{
  enum Hi_State
  {
    In_None       ,
    In_Comment    ,
    In_SingleQuote,
    In_DoubleQuote,
    In_96_Quote   ,
    NumberBeg     ,
    NumberIn      ,
    NumberHex     ,
    NumberFraction,
    NumberExponent,
    Done
  }

  Highlight_MIB( FileBuf fb )
  {
    super( fb );
  }

  void Run_Range( final CrsPos st
                , final int    fn )
  {
    m_state = Hi_State.In_None;

    m_l = st.crsLine;
    m_p = st.crsChar;

    while( Hi_State.Done != m_state
        && m_l<fn )
    {
      Run_State();
    }
    Find_Styles_Keys_In_Range( st, fn );
  }

  void Run_State()
  {
    switch( m_state )
    {
    case In_None       : Hi_In_None       (); break;
    case In_Comment    : Hi_In_Comment    (); break;
    case In_SingleQuote: Hi_In_SingleQuote(); break;
    case In_DoubleQuote: Hi_In_DoubleQuote(); break;
    case In_96_Quote   : Hi_In_96_Quote   (); break;
    case NumberBeg     : Hi_NumberBeg     (); break;
    case NumberIn      : Hi_NumberIn      (); break;
    case NumberHex     : Hi_NumberHex     (); break;
    case NumberFraction: Hi_NumberFraction(); break;
    case NumberExponent: Hi_NumberExponent(); break;
    default:
      m_state = Hi_State.In_None;
    }
  }
  boolean Quote_Start( final char qt
                     , final char c2
                     , final char c1
                     , final char c0 )
  {
    return (c1==0    && c0==qt ) //< Quote at beginning of line
        || (c1!='\\' && c0==qt ) //< Non-escaped quote
        || (c2=='\\' && c1=='\\' && c0==qt ); //< Escaped escape before quote
  }
  boolean OneControl( final char c0 )
  {
    return c0=='(' || c0==')'
        || c0=='{' || c0=='}'
        || c0==',' || c0==';'
        || c0=='|';
  }
  void Hi_In_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int  LL = m_fb.LineLen( m_l );
      final Line lr = m_fb.GetLine( m_l );
 
      for( ; m_p<LL; m_p++ )
      {
        m_fb.ClearSyntaxStyles( m_l, m_p );

        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if     ( c1 == '-' && c0=='-'       ) { m_state = Hi_State.In_Comment; }
        else if( Quote_Start('\'',c2,c1,c0) ) { m_state = Hi_State.In_SingleQuote; }
        else if( Quote_Start('\"',c2,c1,c0) ) { m_state = Hi_State.In_DoubleQuote; }
        else if( Quote_Start('`' ,c2,c1,c0) ) { m_state = Hi_State.In_96_Quote; }
        else if( !Utils.IsIdent(c1)
               && Character.isDigit(c0) ) { m_state = Hi_State.NumberBeg; }
 
        else if( c2==':' && c1==':' && c0=='=' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p-2, Highlight_Type.CONTROL.val );
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.CONTROL.val );
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.VARTYPE.val );
        }
        else if( OneControl( c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
        }
        else if( c0=='.' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        else if( c0 < 32 || 126 < c0 )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
        }
        if( Hi_State.In_None != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_In_Comment()
  {
    final int LL = m_fb.LineLen( m_l );
 
    if( 0 < m_p ) m_p--;

    for( ; m_p<LL; m_p++ )
    {
      m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
    }
    m_p=0; m_l++;
    m_state = Hi_State.In_None;
  }

  void Hi_In_SingleQuote()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
    m_p++;
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      boolean slash_escaped = false;
      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1: (c1,c0)
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if( (c1==0    && c0=='\'')
         || (c1!='\\' && c0=='\'')
         || (c1=='\\' && c0=='\'' && slash_escaped) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++;
          m_state = Hi_State.In_None;
        }
        else {
          if( c1=='\\' && c0=='\\' ) slash_escaped = !slash_escaped;
          else                       slash_escaped = false;

          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        if( Hi_State.In_SingleQuote != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_In_DoubleQuote()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
    m_p++;
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      boolean slash_escaped = false;
      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1: (c1,c0)
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if( (c1==0    && c0=='\"')
         || (c1!='\\' && c0=='\"')
         || (c1=='\\' && c0=='\"' && slash_escaped) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++;
          m_state = Hi_State.In_None;
        }
        else {
          if( c1=='\\' && c0=='\\' ) slash_escaped = !slash_escaped;
          else                       slash_escaped = false;

          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        if( Hi_State.In_DoubleQuote != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_In_96_Quote()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
    m_p++;
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      boolean slash_escaped = false;
      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1: (c1,c0)
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if( (c1==0    && c0=='`')
         || (c1!='\\' && c0=='`')
         || (c1=='\\' && c0=='`' && slash_escaped) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++;
          m_state = Hi_State.In_None;
        }
        else {
          if( c1=='\\' && c0=='\\' ) slash_escaped = !slash_escaped;
          else                       slash_escaped = false;

          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        if( Hi_State.In_96_Quote != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_NumberBeg()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );

    final char c1 = m_fb.Get( m_l, m_p );
    m_p++;
    m_state = Hi_State.NumberIn;

    final int LL = m_fb.LineLen( m_l );
    if( '0' == c1 && (m_p+1)<LL )
    {
      final char c0 = m_fb.Get( m_l, m_p );
      if( 'x' == c0 ) {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.NumberHex;
        m_p++;
      }
    }
  }

  void Hi_NumberIn()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.In_None;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
  
      if( '.'==c1 )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.NumberFraction;
        m_p++;
      }
      else if( 'e'==c1 || 'E'==c1 )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.NumberExponent;
        m_p++;
        if( m_p<LL )
        {
          final char c0 = m_fb.Get( m_l, m_p );
          if( '+' == c0 || '-' == c0 ) {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
            m_p++;
          }
        }
      }
      else if( Character.isDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else {
        m_state = Hi_State.In_None;
      }
    }
  }

  void Hi_NumberHex()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.In_None;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
      if( Utils.IsHexDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else {
        m_state = Hi_State.In_None;
      }
    }
  }

  void Hi_NumberFraction()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.In_None;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
      if( Character.isDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else if( 'e'==c1 || 'E'==c1 )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.NumberExponent;
        m_p++;
        if( m_p<LL )
        {
          final char c0 = m_fb.Get( m_l, m_p );
          if( '+' == c0 || '-' == c0 ) {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
            m_p++;
          }
        }
      }
      else {
        m_state = Hi_State.In_None;
      }
    }
  }

  void Hi_NumberExponent()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.In_None;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
      if( Character.isDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else {
        m_state = Hi_State.In_None;
      }
    }
  }

  // Find keys starting on st up to but not including fn line
  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range( m_HiPairs, st, fn );
  }

  HiKeyVal[] m_HiPairs =
  {
    new HiKeyVal("OBJECT IDENTIFIER", Highlight_Type.VARTYPE ),
    new HiKeyVal("MODULE-COMPLIANCE", Highlight_Type.VARTYPE ),
    new HiKeyVal("MANDATORY-GROUPS" , Highlight_Type.CONTROL ),

    new HiKeyVal("SYNTAX"     , Highlight_Type.CONTROL ),
    new HiKeyVal("UNITS"      , Highlight_Type.CONTROL ),
    new HiKeyVal("MAX-ACCESS" , Highlight_Type.CONTROL ),
    new HiKeyVal("BEGIN"      , Highlight_Type.CONTROL ),
    new HiKeyVal("FROM"       , Highlight_Type.CONTROL ),
    new HiKeyVal("END"        , Highlight_Type.CONTROL ),
    new HiKeyVal("IMPORTS"    , Highlight_Type.CONTROL ),
    new HiKeyVal("OBJECT-TYPE", Highlight_Type.CONTROL ),
    new HiKeyVal("STATUS"     , Highlight_Type.CONTROL ),
    new HiKeyVal("INDEX"      , Highlight_Type.CONTROL ),
    new HiKeyVal("DEFVAL"     , Highlight_Type.CONTROL ),
    new HiKeyVal("DISPLAY-HINT", Highlight_Type.CONTROL ),
    new HiKeyVal("DESCRIPTION", Highlight_Type.CONTROL ),
    new HiKeyVal("DEFINITIONS", Highlight_Type.CONTROL ),
    new HiKeyVal("REVISION"   , Highlight_Type.CONTROL ),
    new HiKeyVal("OBJECTS"    , Highlight_Type.CONTROL ),
    new HiKeyVal("OBJECT-GROUP", Highlight_Type.CONTROL ),
    new HiKeyVal("OBJECT"     , Highlight_Type.CONTROL ),
    new HiKeyVal("REFERENCE"  , Highlight_Type.VARTYPE ),
    new HiKeyVal("WRITE-SYNTAX", Highlight_Type.CONTROL ),
    new HiKeyVal("NOTIFICATIONS", Highlight_Type.CONTROL ),
    new HiKeyVal("LAST-UPDATED", Highlight_Type.CONTROL ),
    new HiKeyVal("ORGANIZATION", Highlight_Type.CONTROL ),
    new HiKeyVal("CONTACT-INFO", Highlight_Type.CONTROL ),

    new HiKeyVal("Counter32"  , Highlight_Type.VARTYPE ),
    new HiKeyVal("Counter64"  , Highlight_Type.VARTYPE ),
    new HiKeyVal("INTEGER"    , Highlight_Type.VARTYPE ),
    new HiKeyVal("Integer32"  , Highlight_Type.VARTYPE ),
    new HiKeyVal("IpAddress"  , Highlight_Type.VARTYPE ),
    new HiKeyVal("InetAddress", Highlight_Type.VARTYPE ),
    new HiKeyVal("InetAddressType", Highlight_Type.VARTYPE ),
    new HiKeyVal("Unsigned32" , Highlight_Type.VARTYPE ),
    new HiKeyVal("Gauge32"    , Highlight_Type.VARTYPE ),
    new HiKeyVal("InterfaceIndex", Highlight_Type.VARTYPE ),
    new HiKeyVal("MacAddress" , Highlight_Type.VARTYPE ),
    new HiKeyVal("TimeTicks"  , Highlight_Type.VARTYPE ),
    new HiKeyVal("TimeStamp"  , Highlight_Type.VARTYPE ),
  //new HiKeyVal("Timeout"    , Highlight_Type.VARTYPE ),
    new HiKeyVal("SnmpAdminString", Highlight_Type.VARTYPE ),
    new HiKeyVal("BITS"       , Highlight_Type.VARTYPE ),
    new HiKeyVal("OCTET"      , Highlight_Type.VARTYPE ),
    new HiKeyVal("SIZE"       , Highlight_Type.VARTYPE ),
    new HiKeyVal("STRING"     , Highlight_Type.VARTYPE ),
    new HiKeyVal("SEQUENCE OF", Highlight_Type.VARTYPE ),
    new HiKeyVal("SEQUENCE"   , Highlight_Type.CONTROL ),
    new HiKeyVal("IDENTIFIER" , Highlight_Type.VARTYPE ),
    new HiKeyVal("MIN-ACCESS" , Highlight_Type.VARTYPE ),
    new HiKeyVal("MODULE-IDENTITY", Highlight_Type.VARTYPE ),
    new HiKeyVal("MODULE"     , Highlight_Type.VARTYPE ),
    new HiKeyVal("TEXTUAL-CONVENTION", Highlight_Type.VARTYPE ),
    new HiKeyVal("NOTIFICATION-GROUP", Highlight_Type.VARTYPE ),
    new HiKeyVal("NOTIFICATION-TYPE", Highlight_Type.VARTYPE ),
    new HiKeyVal("GROUP"      , Highlight_Type.VARTYPE ),
    new HiKeyVal("RowStatus", Highlight_Type.VARTYPE ),
    new HiKeyVal("TruthValue", Highlight_Type.VARTYPE ),
    new HiKeyVal("current"    , Highlight_Type.VARTYPE ),
    new HiKeyVal("read-only"  , Highlight_Type.VARTYPE ),
    new HiKeyVal("read-create", Highlight_Type.VARTYPE ),
    new HiKeyVal("read-write" , Highlight_Type.VARTYPE ),
    new HiKeyVal("not-accessible" , Highlight_Type.VARTYPE ),
  //new HiKeyVal("", Highlight_Type.VARTYPE ),
  };

  Hi_State m_state = Hi_State.In_None;
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line
}

