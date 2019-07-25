////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 07 Sep 2015 Paul J. Gartside                                 //
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

class Highlight_Bash extends Highlight_Base
{
  enum Hi_State
  {
    In_None       ,
    In_Comment    ,
    In_SingleQuote,
    In_DoubleQuote,
    NumberBeg     ,
    NumberIn      ,
    NumberHex     ,
    NumberFraction,
    NumberExponent,
    Done
  }
  Highlight_Bash( FileBuf fb )
  {
    super( fb );
  }

//void Run()
//{
//  m_state = Hi_State.In_None;
//  m_l = 0;
//  m_p = 0;
//
//  while( Hi_State.Done != m_state ) Run_State();
//
//  Find_Styles_Keys();
//}
  void Run_State()
  {
    switch( m_state )
    {
    case In_None       : Hi_In_None       (); break;
    case In_Comment    : Hi_In_Comment    (); break;
    case In_SingleQuote: Hi_In_SingleQuote(); break;
    case In_DoubleQuote: Hi_In_DoubleQuote(); break;
    case NumberBeg     : Hi_NumberBeg     (); break;
    case NumberIn      : Hi_NumberIn      (); break;
    case NumberHex     : Hi_NumberHex     (); break;
    case NumberFraction: Hi_NumberFraction(); break;
    case NumberExponent: Hi_NumberExponent(); break;
    default:
      m_state = Hi_State.In_None;
    }
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
  boolean Quote_Start( final char qt
                     , final char c2
                     , final char c1
                     , final char c0 )
  {
    return (c1==0    && c0==qt ) //< Quote at beginning of line
        || (c1!='\\' && c0==qt ) //< Non-escaped quote
        || (c2=='\\' && c1=='\\' && c0==qt ); //< Escaped escape before quote
  }
  boolean OneVarType( final char c0 )
  {
    return c0=='&' || c0=='#'
        || c0=='.' || c0=='*'
        || c0=='[' || c0==']';
  }
  boolean OneControl( final char c0 )
  {
    return c0=='=' || c0=='@'
        || c0=='^' || c0=='~'
        || c0==':' || c0=='%'
        || c0=='+' || c0=='-'
        || c0=='<' || c0=='>'
        || c0=='!' || c0=='?'
        || c0=='(' || c0==')'
        || c0=='{' || c0=='}'
        || c0==',' || c0==';'
        || c0=='/' || c0=='|';
  }
  boolean TwoControl( final char c1, final char c0 )
  {
    return c1=='=' && c0=='='
        || c1=='&' && c0=='&'
        || c1=='|' && c0=='|'
        || c1=='|' && c0=='='
        || c1=='&' && c0=='='
        || c1=='!' && c0=='='
        || c1=='+' && c0=='='
        || c1=='-' && c0=='=';
  }
  void Hi_In_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int  LL = m_fb.LineLen( m_l );
 
      for( ; m_p<LL; m_p++ )
      {
        m_fb.ClearSyntaxStyles( m_l, m_p );

        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );
        final boolean comment = c0=='#' && (0==m_p || c1!='$');

        if     ( comment )                    { m_state = Hi_State.In_Comment; }
        else if( Quote_Start('\'',c2,c1,c0) ) { m_state = Hi_State.In_SingleQuote; }
        else if( Quote_Start('\"',c2,c1,c0) ) { m_state = Hi_State.In_DoubleQuote; }
        else if( !Utils.IsIdent(c1)
               && Character.isDigit(c0) ) { m_state = Hi_State.NumberBeg; }
 
        else if( TwoControl( c1, c0 ) )
        {
           m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.CONTROL.val );
           m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.CONTROL.val );
        }
        else if( c0=='$' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else if( OneVarType( c0 ) )
        {
           m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
        }
        else if( OneControl( c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
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
 
    for( ; m_p<LL; m_p++ )
    {
      m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
    }
    m_p=0; m_l++;
    m_state = Hi_State.In_None;
  }

//void Hi_In_SingleQuote()
//{
//  m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
//  m_p++;
//  for( ; m_l<m_fb.NumLines(); m_l++ )
//  {
//    final int LL = m_fb.LineLen( m_l );
//
//    boolean slash_escaped = false;
//    for( ; m_p<LL; m_p++ )
//    {
//      // c0 is ahead of c1: (c1,c0)
//      final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
//      final char c0 =           m_fb.Get( m_l, m_p );
//
//      if( (c1==0    && c0=='\'')
//       || (c1!='\\' && c0=='\'')
//       || (c1=='\\' && c0=='\'' && slash_escaped) )
//      {
//        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
//        m_p++;
//        m_state = Hi_State.In_None;
//      }
//      else {
//        if( c1=='\\' && c0=='\\' ) slash_escaped = !slash_escaped;
//        else                       slash_escaped = false;
//
//        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
//      }
//      if( Hi_State.In_SingleQuote != m_state ) return;
//    }
//    m_p = 0;
//  }
//  m_state = Hi_State.Done;
//}
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
          // End of single quote:
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++;
          m_state = Hi_State.In_None;
        }
        else {
          if( (c1!='\\' && c0=='$')
           || (c1=='\\' && c0=='$' && slash_escaped) )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          }
          slash_escaped = (c1=='\\' && c0=='\\') ? !slash_escaped : false;
        }
        if( Hi_State.In_SingleQuote != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

//void Hi_In_DoubleQuote()
//{
//  m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
//  m_p++;
//  for( ; m_l<m_fb.NumLines(); m_l++ )
//  {
//    final int LL = m_fb.LineLen( m_l );
//
//    boolean slash_escaped = false;
//    for( ; m_p<LL; m_p++ )
//    {
//      // c0 is ahead of c1: (c1,c0)
//      final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
//      final char c0 =           m_fb.Get( m_l, m_p );
//
//      if( (c1==0    && c0=='\"')
//       || (c1!='\\' && c0=='\"')
//       || (c1=='\\' && c0=='\"' && slash_escaped) )
//      {
//        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
//        m_p++;
//        m_state = Hi_State.In_None;
//      }
//      else {
//        if( c1=='\\' && c0=='\\' ) slash_escaped = !slash_escaped;
//        else                       slash_escaped = false;
//
//        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
//      }
//      if( Hi_State.In_DoubleQuote != m_state ) return;
//    }
//    m_p = 0;
//  }
//  m_state = Hi_State.Done;
//}
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
          // End of double quote:
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++;
          m_state = Hi_State.In_None;
        }
        else {
          if( (c1!='\\' && c0=='$')
           || (c1=='\\' && c0=='$' && slash_escaped) )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          }
          slash_escaped = (c1=='\\' && c0=='\\') ? !slash_escaped : false;
        }
        if( Hi_State.In_DoubleQuote != m_state ) return;
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

//void Find_Styles_Keys()
//{
//  Hi_FindKey( m_HiPairs );
//}
  // Find keys starting on st up to but not including fn line
  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range( m_HiPairs, st, fn );
  }

  HiKeyVal[] m_HiPairs =
  {
    new HiKeyVal( "if"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "fi"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "else"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "elif"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "for"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "done"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "while"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "do"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "return"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "switch"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "case"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "break"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "then"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "bg"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "bind"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "builtin" , Highlight_Type.CONTROL ),
    new HiKeyVal( "caller"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "cd"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "command" , Highlight_Type.CONTROL ),
    new HiKeyVal( "compgen" , Highlight_Type.CONTROL ),
    new HiKeyVal( "complete", Highlight_Type.CONTROL ),
    new HiKeyVal( "compopt" , Highlight_Type.CONTROL ),
    new HiKeyVal( "continue", Highlight_Type.CONTROL ),
    new HiKeyVal( "echo"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "enable"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "eval"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "exec"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "exit"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "export"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "fc"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "fg"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "hash"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "help"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "history" , Highlight_Type.CONTROL ),
    new HiKeyVal( "jobs"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "kill"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "logout"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "popd"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "printf"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "pushd"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "pwd"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "return"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "set"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "shift"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "shopt"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "source"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "suspend" , Highlight_Type.CONTROL ),
    new HiKeyVal( "test"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "times"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "trap"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "ulimit"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "umask"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "unalias" , Highlight_Type.CONTROL ),
    new HiKeyVal( "unset"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "wait"    , Highlight_Type.CONTROL ),

    new HiKeyVal( "declare" , Highlight_Type.VARTYPE ),
    new HiKeyVal( "dirs"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "disown"  , Highlight_Type.VARTYPE ),
    new HiKeyVal( "getopts" , Highlight_Type.VARTYPE ),
    new HiKeyVal( "let"     , Highlight_Type.VARTYPE ),
    new HiKeyVal( "local"   , Highlight_Type.VARTYPE ),
    new HiKeyVal( "mapfile" , Highlight_Type.VARTYPE ),
    new HiKeyVal( "read"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "readonly", Highlight_Type.VARTYPE ),
    new HiKeyVal( "type"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "typeset" , Highlight_Type.VARTYPE ),

    new HiKeyVal( "false"   , Highlight_Type.CONST   ),
    new HiKeyVal( "true"    , Highlight_Type.CONST   ),

    new HiKeyVal( "alias"   , Highlight_Type.DEFINE  ),
  };

  Hi_State m_state = Hi_State.In_None;
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line
}

