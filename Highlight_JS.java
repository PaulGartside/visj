////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 29 Mar 2017 Paul J. Gartside                                 //
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

import java.util.ArrayList;

class Highlight_JS extends Highlight_Base
{
  Highlight_JS( FileBuf fb )
  {
    super( fb );
  }

  enum Hi_State
  {
    JS_None       ,
    JS_Define     ,
    JS_C_Comment  ,
    JS_CPP_Comment,
    JS_SingleQuote,
    JS_DoubleQuote,
    JS_NumberBeg  ,
    JS_NumberDec  ,
    JS_NumberHex  ,
    JS_NumberExponent,
    JS_NumberFraction,
    JS_NumberTypeSpec,

    Done
  }

  void Run_Range( final CrsPos st
                , final int    fn )
  {
    m_l = st.crsLine;
    m_p = st.crsChar;

    while( Hi_State.Done != m_state
        && m_l<fn )
    {
      final int st_l = m_l;
      final int st_p = m_p;

      Run_State();

      Find_Styles_Keys_In_Range( st_l, st_p, m_l+1 );
    }
  }

  // Find keys starting from st up to but not including fn line
  void Find_Styles_Keys_In_Range( final int st_line
                                , final int st_char
                                , final int fn )
  {
    Hi_FindKey_In_Range( m_JS_HiPairs, st_line, st_char, fn );
  }

  // In_None ----------------------------------
  // |<-> XML_Comment                       /|\
  // |<-> CloseTag                           |
  // |-> NumberBeg                           |
  // |   |-> NumberHex --------------------->|
  // |   \-> NumberDec --------------------->|
  // |       |-> NumberExponent ------------>|
  // |       \-> NumberFraction ------------>|
  // |           \-> NumberExponent -------->|
  // \-> OpenTag_ElemName ------------------>|
  //     \-> OpenTag_AttrName -------------->|
  //         |                     /|\       |
  //         \-> OpenTag_AttrVal ----------->/
  //             |<-> SingleQuote ->|
  //             \<-> DoubleQuote ->/
  //
  void Run_State()
  {
    switch( m_state )
    {
    case JS_None          : Hi_JS_None       (); break;
    case JS_Define        : Hi_JS_Define     (); break;
    case JS_SingleQuote   : Hi_SingleQuote   (); break;
    case JS_DoubleQuote   : Hi_DoubleQuote   (); break;
    case JS_C_Comment     : Hi_JS_C_Comment  (); break;
    case JS_CPP_Comment   : Hi_JS_CPP_Comment(); break;
    case JS_NumberBeg     : Hi_NumberBeg     (); break;
    case JS_NumberDec     : Hi_NumberDec     (); break;
    case JS_NumberHex     : Hi_NumberHex     (); break;
    case JS_NumberFraction: Hi_NumberFraction(); break;
    case JS_NumberExponent: Hi_NumberExponent(); break;
    case JS_NumberTypeSpec: Hi_NumberTypeSpec(); break;

    default:
      m_state = Hi_State.JS_None;
    }
  }

  void Hi_NumberBeg()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );

    final char c1 = m_fb.Get( m_l, m_p );
    m_p++; //< Move past first digit
    m_state = Hi_State.JS_NumberDec;

    final int LL = m_fb.LineLen( m_l );
    if( '0' == c1 && (m_p+1)<LL )
    {
      final char c0 = m_fb.Get( m_l, m_p );
      if( 'x' == c0 ) {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.JS_NumberHex;
        m_p++; //< Move past 'x'
      }
    }
  }

  void Hi_NumberHex()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.JS_None;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
      if( Utils.IsHexDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else {
        m_state = Hi_State.JS_None;
      }
    }
  }

  void Hi_NumberDec()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.JS_None;
    else {
      final char c1 = m_fb.Get( m_l, m_p );

      if( '.'==c1 )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.JS_NumberFraction;
        m_p++;
      }
      else if( 'e'==c1 || 'E'==c1 )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.JS_NumberExponent;
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
      else if( c1=='L' || c1=='F' || c1=='U' )
      {
        m_state = Hi_State.JS_NumberTypeSpec;
      }
      else {
        m_state = Hi_State.JS_None;
      }
    }
  }

  void Hi_NumberExponent()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.JS_None;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
      if( Character.isDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else {
        m_state = Hi_State.JS_None;
      }
    }
  }

  void Hi_NumberFraction()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = Hi_State.JS_None;
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
        m_state = Hi_State.JS_NumberExponent;
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
        m_state = Hi_State.JS_None;
      }
    }
  }

  void Hi_NumberTypeSpec()
  {
    final int LL = m_fb.LineLen( m_l );

    if( m_p < LL )
    {
      final char c0 = m_fb.Get( m_l, m_p );

      if( c0=='L' )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
        m_p++;
        m_state = Hi_State.JS_None;
      }
      else if( c0=='F' )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
        m_p++;
        m_state = Hi_State.JS_None;
      }
      else if( c0=='U' )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val ); m_p++;
        if( m_p<LL ) {
          final char c1 = m_fb.Get( m_l, m_p );
          if( c1=='L' ) { // UL
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val ); m_p++;
            if( m_p<LL ) {
              final char c2 = m_fb.Get( m_l, m_p );
              if( c2=='L' ) { // ULL
                m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val ); m_p++;
              }
            }
          }
        }
        m_state = Hi_State.JS_None;
      }
    }
  }

  void Hi_SingleQuote()
  {
    boolean exit = false;
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
          m_p++; // Move past '\''
          m_state = Hi_State.JS_None;
          exit = true;
        }
        else {
          if( c1=='\\' && c0=='\\' ) slash_escaped = !slash_escaped;
          else                       slash_escaped = false;

          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        if( exit ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_DoubleQuote()
  {
    boolean exit = false;
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
          m_p++; //< Move past '\"'
          m_state = Hi_State.JS_None;
          exit = true;
        }
        else {
          if( c1=='\\' && c0=='\\' ) slash_escaped = !slash_escaped;
          else                       slash_escaped = false;

          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        if( exit ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  boolean OneVarType( final char c0 )
  {
    return c0=='&'
        || c0=='.' || c0=='*'
        || c0=='[' || c0==']';
  }
  boolean OneControl( final char c0 )
  {
    return c0=='=' || c0=='^' || c0=='~'
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

  void Hi_JS_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      String lr = m_fb.GetLine( m_l ).toString();
      final int LL = m_fb.LineLen( m_l );
 
      for( ; m_p<LL; m_p++ )
      {
        m_fb.ClearSyntaxStyles( m_l, m_p );

        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if     ( c1=='/' && c0 == '/' ) { m_p--; m_state = Hi_State.JS_CPP_Comment; }
        else if( c1=='/' && c0 == '*' ) { m_p--; m_state = Hi_State.JS_C_Comment; }
        else if(            c0 == '#' ) { m_state = Hi_State.JS_Define; }
        else if( c0 == '\'')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.JS_SingleQuote;
        }
        else if( c0 == '\"')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.JS_DoubleQuote;
        }
        else if( !Utils.IsIdent( c1 )
               && Character.isDigit(c0))
        {
          m_state = Hi_State.JS_NumberBeg;
        }
        else if( c1==':' && c0==':'
              || c1=='-' && c0=='>' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.VARTYPE.val );
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.VARTYPE.val );
        }
        else if( TwoControl( c1, c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.CONTROL.val );
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.CONTROL.val );
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
        if( Hi_State.JS_None != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_JS_Define()
  {
    final int LL = m_fb.LineLen( m_l );

    for( ; m_p<LL; m_p++ )
    {
      // c0 is ahead of c1: (c1,c0)
      final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
      final char c0 =           m_fb.Get( m_l, m_p );

      if( c1=='/' && c0=='/' )
      {
        m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.COMMENT.val );
        m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.COMMENT.val );
        m_p++;
        m_state = Hi_State.JS_CPP_Comment;
      }
      else if( c1=='/' && c0=='*' )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
        m_p++;
        m_state = Hi_State.JS_C_Comment;
      }
      else {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
      }
      if( Hi_State.JS_Define != m_state ) return;
    }
    m_p=0; m_l++;
    m_state = Hi_State.JS_None;
  }
  void Hi_JS_C_Comment()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1: (c1,c0)
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );

        if( c1=='*' && c0=='/' )
        {
          m_p++; //< Move past '/'
          m_state = Hi_State.JS_None;
        }
        if( Hi_State.JS_C_Comment != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }
  void Hi_JS_CPP_Comment()
  {
    final int LL = m_fb.LineLen( m_l );

    for( ; m_p<LL; m_p++ )
    {
      m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
    }
    m_l++;
    m_p=0;
    m_state = Hi_State.JS_None;
  }

  Hi_State m_state = Hi_State.JS_None;
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line


  HiKeyVal[] m_JS_HiPairs =
  {
    // Keywords:
    new HiKeyVal( "break"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "break"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "catch"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "continue"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "debugger"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "default"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "delete"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "do"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "else"      , Highlight_Type.CONTROL ),
    new HiKeyVal( "finally"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "for"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "function"  , Highlight_Type.CONTROL ),
    new HiKeyVal( "if"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "in"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "instanceof", Highlight_Type.CONTROL ),
    new HiKeyVal( "new"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "return"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "switch"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "throw"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "try"       , Highlight_Type.CONTROL ),
    new HiKeyVal( "typeof"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "var"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "void"      , Highlight_Type.VARTYPE ),
    new HiKeyVal( "while"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "with"      , Highlight_Type.CONTROL ),

    // Keywords in strict mode:
    new HiKeyVal( "implements", Highlight_Type.CONTROL ),
    new HiKeyVal( "interface" , Highlight_Type.CONTROL ),
    new HiKeyVal( "let"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "package"   , Highlight_Type.DEFINE  ),
    new HiKeyVal( "private"   , Highlight_Type.CONTROL ),
    new HiKeyVal( "protected" , Highlight_Type.CONTROL ),
    new HiKeyVal( "public"    , Highlight_Type.CONTROL ),
    new HiKeyVal( "static"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "yield"     , Highlight_Type.CONTROL ),

    // Constants:
    new HiKeyVal( "false", Highlight_Type.CONST ),
    new HiKeyVal( "null" , Highlight_Type.CONST ),
    new HiKeyVal( "true" , Highlight_Type.CONST ),

    // Global variables and functions:
    new HiKeyVal( "arguments"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Array"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Boolean"           , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Date"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "decodeURI"         , Highlight_Type.CONTROL ),
    new HiKeyVal( "decodeURIComponent", Highlight_Type.CONTROL ),
    new HiKeyVal( "encodeURI"         , Highlight_Type.CONTROL ),
    new HiKeyVal( "encodeURIComponent", Highlight_Type.CONTROL ),
    new HiKeyVal( "Error"             , Highlight_Type.VARTYPE ),
    new HiKeyVal( "eval"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "EvalError"         , Highlight_Type.CONTROL ),
    new HiKeyVal( "Function"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "Infinity"          , Highlight_Type.CONST   ),
    new HiKeyVal( "isFinite"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "isNaN"             , Highlight_Type.CONTROL ),
    new HiKeyVal( "JSON"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "Math"              , Highlight_Type.CONTROL ),
    new HiKeyVal( "NaN"               , Highlight_Type.CONST   ),
    new HiKeyVal( "Number"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "Object"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "parseFloat"        , Highlight_Type.CONTROL ),
    new HiKeyVal( "parseInt"          , Highlight_Type.CONTROL ),
    new HiKeyVal( "RangeError"        , Highlight_Type.VARTYPE ),
    new HiKeyVal( "ReferenceError"    , Highlight_Type.VARTYPE ),
    new HiKeyVal( "RegExp"            , Highlight_Type.CONTROL ),
    new HiKeyVal( "String"            , Highlight_Type.VARTYPE ),
    new HiKeyVal( "SyntaxError"       , Highlight_Type.VARTYPE ),
    new HiKeyVal( "TypeError"         , Highlight_Type.VARTYPE ),
    new HiKeyVal( "undefined"         , Highlight_Type.CONST   ),
    new HiKeyVal( "URIError"          , Highlight_Type.VARTYPE ),
  };
};

