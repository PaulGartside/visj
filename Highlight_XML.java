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

class Highlight_XML extends Highlight_Base
{
  Highlight_XML( FileBuf fb )
  {
    super( fb );
  }

  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range( m_HiPairs, st, fn );
  }
  HiKeyVal[] m_HiPairs =
  {
    // HTML tags:
    new HiKeyVal( "xml"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "version" , Highlight_Type.CONTROL ),
    new HiKeyVal( "encoding", Highlight_Type.CONTROL ),
  };
  enum Hi_State
  {
    In_None       ,
    OpenTag_ElemName,
    OpenTag_AttrName,
    OpenTag_AttrVal,
    CloseTag      ,
    Comment       ,
    In_SingleQuote,
    In_DoubleQuote,
    NumberBeg     ,
    NumberIn      ,
    NumberHex     ,
    NumberFraction,
    NumberExponent,
    Done
  }

  void Run_State()
  {
    switch( m_state )
    {
    case In_None         : Hi_In_None         (); break;
    case OpenTag_ElemName: Hi_OpenTag_ElemName(); break;
    case OpenTag_AttrName: Hi_OpenTag_AttrName(); break;
    case OpenTag_AttrVal : Hi_OpenTag_AttrVal (); break;
    case CloseTag        : Hi_CloseTag        (); break;
    case Comment         : Hi_Comment         (); break;
    case In_SingleQuote  : Hi_In_SingleQuote  (); break;
    case In_DoubleQuote  : Hi_In_DoubleQuote  (); break;
    case NumberBeg       : Hi_NumberBeg       (); break;
    case NumberIn        : Hi_NumberIn        (); break;
    case NumberHex       : Hi_NumberHex       (); break;
    case NumberFraction  : Hi_NumberFraction  (); break;
    case NumberExponent  : Hi_NumberExponent  (); break;
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

  void Hi_In_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int  LL = m_fb.LineLen( m_l );
 
      for( ; m_p<LL; m_p++ )
      {
        m_fb.ClearSyntaxStyles( m_l, m_p );

        // c0 is ahead of c1 is ahead of c2: (c3,c2,c1,c0)
        final char c3 = (2<m_p) ? m_fb.Get( m_l, m_p-3 ) : 0;
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if( c1=='<' && c0!='!' && c0!='/')
        {
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.DEFINE.val );
          m_state = Hi_State.OpenTag_ElemName;
        }
        else if( c1=='<' && c0=='/')
        {
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.DEFINE.val ); //< '<'
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.DEFINE.val ); //< '/'
          m_p++; // Move past '/'
          m_state = Hi_State.OpenTag_ElemName;
        }
        else if( c3=='<' && c2=='!' && c1=='-' && c0=='-' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p-3, Highlight_Type.COMMENT.val ); //< '<'
          m_fb.SetSyntaxStyle( m_l, m_p-2, Highlight_Type.COMMENT.val ); //< '!'
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.COMMENT.val ); //< '-'
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.COMMENT.val ); //< '-'
          m_p++; // Move past '-'
          m_state = Hi_State.Comment;
        }
        else if( c0=='\'')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; // Move past '\''
          m_state = Hi_State.In_SingleQuote;
          m_qtXSt = Hi_State.In_None;
        }
        else if( c0=='\"')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; // Move past '\"'
          m_state = Hi_State.In_DoubleQuote;
          m_qtXSt = Hi_State.In_None;
        }
        else if( !Utils.IsIdent( c1 )
              && Character.isDigit( c0 ) )
        {
          m_state = Hi_State.NumberBeg;
        }
        else {
          ; //< No syntax highlighting on content outside of <>tags
        }

        if( Hi_State.In_None != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_OpenTag_ElemName()
  {
    boolean found_elem_name = false;

    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      for( ; m_p<LL; m_p++ )
      {
        final char c0 = m_fb.Get( m_l, m_p );

        if( c0=='>' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          m_p++; // Move past '>'
          m_state = Hi_State.In_None;
        }
        else if( c0=='/' || c0=='?' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else if( !found_elem_name )
        {
          if( Utils.IsXML_Ident( c0 ) )
          {
            found_elem_name = true;
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
          }
          else if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        else if( found_elem_name )
        {
          if( Utils.IsXML_Ident( c0 ) )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
          }
          else if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
            m_p++; //< Move past white space
            m_state = Hi_State.OpenTag_AttrName;
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        else {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
        }
        if( Hi_State.OpenTag_ElemName != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }
  void Hi_OpenTag_AttrName()
  {
    boolean found_attr_name = false;
    boolean past__attr_name = false;

    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c0 = m_fb.Get( m_l, m_p );

        if( c0=='>' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          m_p++; // Move past '>'
          m_state = Hi_State.In_None;
        }
        else if( c0=='/' || c0=='?' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else if( !found_attr_name )
        {
          if( Utils.IsXML_Ident( c0 ) )
          {
            found_attr_name = true;
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
          }
          else if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        else if( found_attr_name && !past__attr_name )
        {
          if( Utils.IsXML_Ident( c0 ) )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
          }
          else if( c0==' ' || c0=='\t' )
          {
            past__attr_name = true;
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
          }
          else if( c0=='=' )
          {
            past__attr_name = true;
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
            m_p++; //< Move past '='
            m_state = Hi_State.OpenTag_AttrVal;
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        else if( found_attr_name && past__attr_name )
        {
          if( c0=='=' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
            m_p++; //< Move past '='
            m_state = Hi_State.OpenTag_AttrVal;
          }
          else if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        else {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
        }
        if( Hi_State.OpenTag_AttrName != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }
  void Hi_OpenTag_AttrVal()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c0 = m_fb.Get( m_l, m_p );

        if( c0=='>' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          m_p++; // Move past '>'
          m_state = Hi_State.In_None;
        }
        else if( c0=='/' || c0=='?' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else if( c0=='\'' )
        {
        //m_state = Hi_State.BegSingleQuote;
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; // Move past '\''
          m_state = Hi_State.In_SingleQuote;
          m_qtXSt = Hi_State.OpenTag_AttrName;
        }
        else if( c0=='\"' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.In_DoubleQuote;
          m_qtXSt = Hi_State.OpenTag_AttrName;
        }
        else if( c0==' ' || c0=='\t' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
        }
        if( Hi_State.OpenTag_AttrVal != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_CloseTag()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c0 = m_fb.Get( m_l, m_p );

        if( c0=='>' )
        {
        //m_state = Hi_State.End_CloseTag;
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          m_p++; // Move past '>'
          m_state = Hi_State.In_None;
        }
        else if( c0=='/' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else if( Utils.IsXML_Ident( c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
        }
        else if( c0==' ' || c0=='\t' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
        }
        if( Hi_State.CloseTag != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_Comment()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );

      for( ; m_p<LL; m_p++ )
      {
        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if( c2=='-' && c1=='-' && c0=='>' )
        {
        //m_state = Hi_State.End_Comment;
          m_fb.SetSyntaxStyle( m_l, m_p-2, Highlight_Type.COMMENT.val ); //< '-'
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.COMMENT.val ); //< '-'
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.COMMENT.val ); //< '>'
          m_p++; // Move past '>'
          m_state = Hi_State.In_None;
        }
        else {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );
        }
        if( Hi_State.Comment != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
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
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; // Move past '\''
          m_state = m_qtXSt;
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
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = m_qtXSt;
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
  Hi_State m_state = Hi_State.In_None;
  Hi_State m_qtXSt = Hi_State.In_None; // Quote exit state
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line
};

