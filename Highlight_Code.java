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

abstract class Highlight_Code extends Highlight_Base
{
  enum Hi_State
  {
    In_None       ,
    In_Define     ,
    BegC_Comment  ,
    In_C_Comment  ,
    EndC_Comment  ,
    BegCPP_Comment,
    In_CPP_Comment,
    EndCPP_Comment,
    BegSingleQuote,
    In_SingleQuote,
    EndSingleQuote,
    BegDoubleQuote,
    In_DoubleQuote,
    EndDoubleQuote,
    NumberBeg     ,
    NumberIn      ,
    NumberHex     ,
    NumberFraction,
    NumberExponent,
    Done
  }
  Highlight_Code( FileBuf fb )
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
    case In_Define     : Hi_In_Define     (); break;
    case BegC_Comment  : Hi_BegC_Comment  (); break;
    case In_C_Comment  : Hi_In_C_Comment  (); break;
    case EndC_Comment  : Hi_EndC_Comment  (); break;
    case BegCPP_Comment: Hi_BegCPP_Comment(); break;
    case In_CPP_Comment: Hi_In_CPP_Comment(); break;
    case EndCPP_Comment: Hi_EndCPP_Comment(); break;
    case BegSingleQuote: Hi_BegSingleQuote(); break;
    case In_SingleQuote: Hi_In_SingleQuote(); break;
    case EndSingleQuote: Hi_EndSingleQuote(); break;
    case BegDoubleQuote: Hi_BegDoubleQuote(); break;
    case In_DoubleQuote: Hi_In_DoubleQuote(); break;
    case EndDoubleQuote: Hi_EndDoubleQuote(); break;
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
  void Hi_In_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int  LL = m_fb.LineLen( m_l );
    //final Line lr = m_fb.GetLine( m_l );
 
      for( ; m_p<LL; m_p++ )
      {
        m_fb.ClearSyntaxStyles( m_l, m_p );

        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if     ( c1=='/' && c0 == '/' ) { m_p--; m_state = Hi_State.BegCPP_Comment; }
        else if( c1=='/' && c0 == '*' ) { m_p--; m_state = Hi_State.BegC_Comment; }
        else if(            c0 == '#' ) { m_state = Hi_State.In_Define; }
        else if(            c0 == '\'') { m_state = Hi_State.BegSingleQuote; }
        else if(            c0 == '\"') { m_state = Hi_State.BegDoubleQuote; }
        else if( !Utils.IsIdent( c1 )
               && Character.isDigit(c0)){ m_state = Hi_State.NumberBeg; }
 
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
        if( Hi_State.In_None != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }
  void Hi_In_Define()
  {
    final int LL = m_fb.LineLen( m_l );

    for( ; m_p<LL; m_p++ )
    {
      final char c1 = 0<m_p ? m_fb.Get( m_l, m_p-1 ) : m_fb.Get( m_l, m_p );
      final char c0 = 0<m_p ? m_fb.Get( m_l, m_p   ) : 0;

      if( c1=='/' && c0=='/' )
      {
        m_state = Hi_State.BegCPP_Comment;
        m_p--;
      }
      else if( c1=='/' && c0=='*' )
      {
        m_state = Hi_State.BegC_Comment;
        m_p--;
      }
      else {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
      }
      if( Hi_State.In_Define != m_state ) return;
    }
    m_p=0; m_l++;
    m_state = Hi_State.In_None;
  }
  void Hi_BegC_Comment()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val ); m_p++;
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val ); m_p++;
    m_state = Hi_State.In_C_Comment;
  }
  void Hi_In_C_Comment()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      for( ; m_p<LL; m_p++ )
      {
        final char c1 = 0<m_p ? m_fb.Get( m_l, m_p-1 ) : m_fb.Get( m_l, m_p );
        final char c0 = 0<m_p ? m_fb.Get( m_l, m_p   ) : 0;

        if( c1=='*' && c0=='/' )
        {
          m_state = Hi_State.EndC_Comment;
        }
        else m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );

        if( Hi_State.In_C_Comment != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }
  void Hi_EndC_Comment()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
    m_p++;
    m_state = Hi_State.In_None;
  }
  void Hi_BegCPP_Comment()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val ); m_p++;
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val ); m_p++;
    m_state = Hi_State.In_CPP_Comment;
  }
  void Hi_In_CPP_Comment()
  {
    final int LL = m_fb.LineLen( m_l );

    for( ; m_p<LL; m_p++ )
    {
      m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
    }
    m_p--;
    m_state = Hi_State.EndCPP_Comment;
  }
  void Hi_EndCPP_Comment()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
    m_p=0; m_l++;
    m_state = Hi_State.In_None;
  }
  void Hi_BegSingleQuote()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
    m_p++;
    m_state = Hi_State.In_SingleQuote;
  }
  void Hi_In_SingleQuote()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      boolean slash_escaped = false;
      for( ; m_p<LL; m_p++ )
      {
        final char c1 = 0<m_p ? m_fb.Get( m_l, m_p-1 ) : m_fb.Get( m_l, m_p );
        final char c0 = 0<m_p ? m_fb.Get( m_l, m_p   ) : 0;

        if( (c1=='\'' && c0==0   )
         || (c1!='\\' && c0=='\'')
         || (c1=='\\' && c0=='\'' && slash_escaped) )
        {
          m_state = Hi_State.EndSingleQuote;
        }
        else {
          if( c1=='\\' && c0=='\\' ) slash_escaped = true;
          else                       slash_escaped = false;

          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        if( Hi_State.In_SingleQuote != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }
  void Hi_EndSingleQuote()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
    m_p++; //m_p++;
    m_state = Hi_State.In_None;
  }

  void Hi_BegDoubleQuote()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
    m_p++;
    m_state = Hi_State.In_DoubleQuote;
  }
  void Hi_In_DoubleQuote()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );
  
      boolean slash_escaped = false;
      for( ; m_p<LL; m_p++ )
      {
        final char c1 = 0<m_p ? m_fb.Get( m_l, m_p-1 ) : m_fb.Get( m_l, m_p );
        final char c0 = 0<m_p ? m_fb.Get( m_l, m_p   ) : 0;
  
        if( (c1=='\"' && c0==0   )
         || (c1!='\\' && c0=='\"')
         || (c1=='\\' && c0=='\"' && slash_escaped) )
        {
          m_state = Hi_State.EndDoubleQuote;
        }
        else {
          if( c1=='\\' && c0=='\\' ) slash_escaped = true;
          else                       slash_escaped = false;
  
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        }
        if( Hi_State.In_DoubleQuote != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }
  void Hi_EndDoubleQuote()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
    m_p++; //m_p++;
    m_state = Hi_State.In_None;
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
    //if( Character.isDigit(c1) ) // FIXME: Should be isHexDigit
      if( Utils.IsHexDigit(c1) ) // FIXME: Should be isHexDigit
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

  abstract void Find_Styles_Keys();
  abstract void Find_Styles_Keys_In_Range( final CrsPos st
                                         , final int    fn );
  Hi_State m_state = Hi_State.In_None;
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line
};

