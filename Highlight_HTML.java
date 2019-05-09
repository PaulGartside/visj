////////////////////////////////////////////////////////////////////////////////
// VI-Simplified (vis) Java Implementation                                    //
// Copyright (c) 18 Mar 2017 Paul J. Gartside                                 //
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

class Highlight_HTML extends Highlight_Base
{
  Highlight_HTML( FileBuf fb )
  {
    super( fb );
  }

  enum Hi_State
  {
    In_None       ,
    OpenTag_ElemName,
    OpenTag_AttrName,
    OpenTag_AttrVal,
    CloseTag      ,
    XML_Comment   ,
    SingleQuote   ,
    DoubleQuote   ,
    NumberBeg     ,
    NumberDec     ,
    NumberHex     ,
    NumberExponent,
    NumberFraction,
    NumberTypeSpec,

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

    CS_None       ,
    CS_C_Comment  ,
    CS_SingleQuote,
    CS_DoubleQuote,

    Done
  }

  void Run_Range( final CrsPos st
                , final int    fn )
  {
    m_state = Run_Range_Get_Initial_State( st );

    m_l = st.crsLine;
    m_p = st.crsChar;

    while( Hi_State.Done != m_state
        && m_l<fn )
    {
      final boolean state_was_JS = JS_State( m_state );
      final int st_l = m_l;
      final int st_p = m_p;

      Run_State();

      if( state_was_JS )
      {
        Find_Styles_Keys_In_Range( st_l, st_p, m_l+1 );
      }
    }
  }

  Hi_State Run_Range_Get_Initial_State( final CrsPos st )
  {
    Hi_State initial = Get_Initial_State( st, m_JS_edges
                                            , m_CS_edges
                                            , Hi_State.JS_None );
    if( null == initial )
    {
      initial = Get_Initial_State( st, m_CS_edges
                                     , m_JS_edges
                                     , Hi_State.CS_None );
    }
    if( null == initial )
    {
      initial = Hi_State.In_None;
    }
    return initial;
  }

  Hi_State Get_Initial_State( final CrsPos st
                            , ArrayList<Edges> edges_1
                            , ArrayList<Edges> edges_2
                            , Hi_State initial_1 )
  {
    Hi_State initial = null;

    boolean found_containing_edges = false;

    for( int k=0; k<edges_1.size(); k++ )
    {
      if( !found_containing_edges )
      {
        Edges edges = edges_1.get(k);
        if( edges.contains( st ) )
        {
          initial = initial_1;
          found_containing_edges = true;
        }
      }
      else {
        // Since a change was made at st, all the following edges
        // have been invalidated, so remove all following elements
        edges_1.remove( k );
        k--; //< Since the current element was just removed, stay on k
      }
    }
    if( found_containing_edges )
    {
      // Remove all CS_edges past st:
      for( int k=0; k<edges_2.size(); k++ )
      {
        Edges edges = edges_2.get(k);
        if( edges.ge( st ) )
        {
          edges_2.remove( k );
          k--; //< Since the current element was just removed, stay on k
        }
      }
    }
    return initial;
  }

  boolean JS_State( final Hi_State state )
  {
    return state == Hi_State.JS_None
        || state == Hi_State.JS_Define
        || state == Hi_State.JS_SingleQuote
        || state == Hi_State.JS_DoubleQuote
        || state == Hi_State.JS_C_Comment
        || state == Hi_State.JS_CPP_Comment
        || state == Hi_State.JS_NumberBeg
        || state == Hi_State.JS_NumberDec
        || state == Hi_State.JS_NumberHex
        || state == Hi_State.JS_NumberFraction
        || state == Hi_State.JS_NumberExponent
        || state == Hi_State.JS_NumberTypeSpec;
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
    case In_None         : Hi_In_None         (); break;
    case XML_Comment     : Hi_XML_Comment     (); break;
    case CloseTag        : Hi_CloseTag        (); break;
    case NumberBeg       : Hi_NumberBeg       (); break;
    case NumberHex       : Hi_NumberHex       (); break;
    case NumberDec       : Hi_NumberDec       (); break;
    case NumberExponent  : Hi_NumberExponent  (); break;
    case NumberFraction  : Hi_NumberFraction  (); break;
    case NumberTypeSpec  : Hi_NumberTypeSpec  (); break;
    case OpenTag_ElemName: Hi_OpenTag_ElemName(); break;
    case OpenTag_AttrName: Hi_OpenTag_AttrName(); break;
    case OpenTag_AttrVal : Hi_OpenTag_AttrVal (); break;
    case SingleQuote     : Hi_SingleQuote     (); break;
    case DoubleQuote     : Hi_DoubleQuote     (); break;

    case JS_None          : Hi_JS_None       (); break;
    case JS_Define        : Hi_JS_Define     (); break;
    case JS_SingleQuote   : Hi_SingleQuote   (); break;
    case JS_DoubleQuote   : Hi_DoubleQuote   (); break;
    case JS_C_Comment     : Hi_C_Comment     (); break;
    case JS_CPP_Comment   : Hi_JS_CPP_Comment(); break;
    case JS_NumberBeg     : Hi_NumberBeg     (); break;
    case JS_NumberDec     : Hi_NumberDec     (); break;
    case JS_NumberHex     : Hi_NumberHex     (); break;
    case JS_NumberFraction: Hi_NumberFraction(); break;
    case JS_NumberExponent: Hi_NumberExponent(); break;
    case JS_NumberTypeSpec: Hi_NumberTypeSpec(); break;

    case CS_None          : Hi_CS_None       (); break;
    case CS_C_Comment     : Hi_C_Comment     (); break;
    case CS_SingleQuote   : Hi_SingleQuote   (); break;
    case CS_DoubleQuote   : Hi_DoubleQuote   (); break;

    default:
      m_state = Hi_State.In_None;
    }
  }

  void Hi_In_None()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );
 
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
          m_state = Hi_State.CloseTag;
        }
        else if( c3=='<' && c2=='!' && c1=='-' && c0=='-')
        {
          m_fb.SetSyntaxStyle( m_l, m_p-3, Highlight_Type.COMMENT.val ); //< '<'
          m_fb.SetSyntaxStyle( m_l, m_p-2, Highlight_Type.COMMENT.val ); //< '!'
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.COMMENT.val ); //< '-'
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.COMMENT.val ); //< '-'
          m_p++; // Move past '-'
          m_state = Hi_State.XML_Comment;
        }
        else if( c3=='<' && c2=='!' && c1=='D' && c0=='O')
        {
          // <!DOCTYPE html>   
          m_fb.SetSyntaxStyle( m_l, m_p-3, Highlight_Type.DEFINE.val ); //< '<'
          m_fb.SetSyntaxStyle( m_l, m_p-2, Highlight_Type.DEFINE.val ); //< '!'
          m_p--; // Move back to 'D'
          m_state = Hi_State.OpenTag_ElemName;
        }
        else if( !Utils.IsIdent( c1 )
              && Character.isDigit( c0 ) )
        {
          m_state = Hi_State.NumberBeg;
          m_numXSt = Hi_State.In_None;
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

  void Hi_XML_Comment()
  {
    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      final int LL = m_fb.LineLen( m_l );
      for( ; m_p<LL; m_p++ )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.COMMENT.val );

        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if( c2=='-' && c1=='-' && c0=='>' )
        {
          m_p++; // Move past '>'
          m_state = Hi_State.In_None;
        }
        if( Hi_State.XML_Comment != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_CloseTag()
  {
    boolean found_elem_name = false;

    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      String ls = m_fb.GetLine( m_l ).toString();
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
          // Returns non-zero if ls has HTTP tag at m_p:
          final int tag_len = Has_HTTP_Tag_At( ls, m_p );
          if( 0<tag_len )
          {
            found_elem_name = true;
            for( int k=0; k<tag_len; k++, m_p++ )
            {
              m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
            }
            m_p--;
          }
          else if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        else //( found_elem_name )
        {
          if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        if( Hi_State.CloseTag != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  void Hi_NumberBeg()
  {
    m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );

    final char c1 = m_fb.Get( m_l, m_p );
    m_p++; //< Move past first digit
    m_state = Hi_State.JS_NumberBeg == m_state
            ? Hi_State.JS_NumberDec
            : Hi_State.NumberDec;

    final int LL = m_fb.LineLen( m_l );
    if( '0' == c1 && (m_p+1)<LL )
    {
      final char c0 = m_fb.Get( m_l, m_p );
      if( 'x' == c0 ) {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.JS_NumberBeg == m_state
                ? Hi_State.JS_NumberHex
                : Hi_State.NumberHex;
        m_p++; //< Move past 'x'
      }
    }
  }

  void Hi_NumberHex()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = m_numXSt;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
      if( Utils.IsHexDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else {
        m_state = m_numXSt;
      }
    }
  }

  void Hi_NumberDec()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = m_numXSt;
    else {
      final char c1 = m_fb.Get( m_l, m_p );

      if( '.'==c1 )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.JS_NumberDec == m_state
                ? Hi_State.JS_NumberFraction
                : Hi_State.NumberFraction;
        m_p++;
      }
      else if( 'e'==c1 || 'E'==c1 )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_state = Hi_State.JS_NumberDec == m_state
                ? Hi_State.JS_NumberExponent
                : Hi_State.NumberExponent;
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
        m_state = Hi_State.JS_NumberDec == m_state
                ? Hi_State.JS_NumberTypeSpec
                : Hi_State.NumberTypeSpec;
      }
      else {
        m_state = m_numXSt;
      }
    }
  }

  void Hi_NumberExponent()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = m_numXSt;
    else {
      final char c1 = m_fb.Get( m_l, m_p );
      if( Character.isDigit(c1) )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
        m_p++;
      }
      else {
        m_state = m_numXSt;
      }
    }
  }

  void Hi_NumberFraction()
  {
    final int LL = m_fb.LineLen( m_l );
    if( LL <= m_p ) m_state = m_numXSt;
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
        m_state = Hi_State.JS_NumberFraction == m_state
                ? Hi_State.JS_NumberExponent
                : Hi_State.NumberExponent;
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
        m_state = m_numXSt;
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
        m_state = m_numXSt;
      }
      else if( c0=='F' )
      {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
        m_p++;
        m_state = m_numXSt;
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
        m_state = m_numXSt;
      }
    }
  }

  void Hi_OpenTag_ElemName()
  {
    m_OpenTag_was_script = false;
    m_OpenTag_was_style  = false;
    boolean found_elem_name = false;

    for( ; m_l<m_fb.NumLines(); m_l++ )
    {
      String lr = m_fb.GetLine( m_l ).toString();
      final int LL = m_fb.LineLen( m_l );
      for( ; m_p<LL; m_p++ )
      {
        final char c0 = m_fb.Get( m_l, m_p );

        if( c0=='>' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          m_p++; // Move past '>'
          m_state = Hi_State.In_None;
          if( m_OpenTag_was_script )
          {
            m_state = Hi_State.JS_None;
            m_JS_edges.add( new Edges( m_l, m_p ) );
          }
          else if( m_OpenTag_was_style )
          {
            m_state = Hi_State.CS_None;
            m_CS_edges.add( new Edges( m_l, m_p ) );
          }
        }
        else if( c0=='/' || c0=='?' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else if( !found_elem_name )
        {
          // Returns non-zero if lr has HTTP tag at m_p:
          final int tag_len = Has_HTTP_Tag_At( lr, m_p );
          if( 0<tag_len )
          {
            if( lr.regionMatches( true, m_p, "script", 0, 6 ) )
            {
              m_OpenTag_was_script = true;
            }
            else if( lr.regionMatches( true, m_p, "style", 0, 5 ) )
            {
              m_OpenTag_was_style = true;
            }
            found_elem_name = true;
            for( int k=0; k<tag_len; k++, m_p++ )
            {
              m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
            }
            m_p--;
          }
          else if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
        }
        else //( found_elem_name )
        {
          if( c0==' ' || c0=='\t' )
          {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
            m_p++; //< Move past white space
            m_state = Hi_State.OpenTag_AttrName;
          }
          else {
            m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
          }
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
          m_state = m_OpenTag_was_style
                  ? Hi_State.CS_None
                  : Hi_State.In_None;
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
        else //( found_attr_name && past__attr_name )
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
          m_state = m_OpenTag_was_style
                  ? Hi_State.CS_None
                  : Hi_State.In_None;
        }
        else if( c0=='/' || c0=='?' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
        }
        else if( c0=='\'' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; // Move past '\''
          m_state = Hi_State.SingleQuote;
          m_qtXSt = Hi_State.OpenTag_AttrName;
        }
        else if( c0=='\"' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.DoubleQuote;
          m_qtXSt = Hi_State.OpenTag_AttrName;
        }
        else if( Character.isDigit( c0 ) )
        {
          m_state = Hi_State.NumberBeg;
          m_numXSt = Hi_State.OpenTag_AttrName;
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
          m_state = m_qtXSt;
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
          m_state = m_qtXSt;
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

  boolean JS_OneVarType( final char c0 )
  {
    return c0=='&'
        || c0=='.' || c0=='*'
        || c0=='[' || c0==']';
  }
  boolean JS_OneControl( final char c0 )
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
  boolean JS_TwoControl( final char c1, final char c0 )
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
        else if( c1=='/' && c0 == '*' )
        {
          m_p--;
          m_state = Hi_State.JS_C_Comment;
          m_ccXSt = Hi_State.JS_None;
        }
        else if( c0 == '#' ) { m_state = Hi_State.JS_Define; }
        else if( c0 == '\'')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.JS_SingleQuote;
          m_qtXSt = Hi_State.JS_None;
        }
        else if( c0 == '\"')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.JS_DoubleQuote;
          m_qtXSt = Hi_State.JS_None;
        }
        else if( !Utils.IsIdent( c1 )
               && Character.isDigit(c0))
        {
          m_state = Hi_State.NumberBeg;
          m_numXSt = Hi_State.JS_None;
        }
        else if( c1==':' && c0==':'
              || c1=='-' && c0=='>' )
        {
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.VARTYPE.val );
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.VARTYPE.val );
        }
        else if( c1=='<' && c0=='/' && m_p+7<LL )
        {
          if( lr.regionMatches( true, m_p-1, "</script", 0, 8 ) )
          {
            m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.DEFINE.val );
            m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.DEFINE.val );
            m_p++; // Move past '/'
            m_state = Hi_State.CloseTag;
            if( 0<m_JS_edges.size() ) { //< Should always be true
              Edges edges = m_JS_edges.get( m_JS_edges.size()-1 );
              edges.end = new CrsPos( m_l, m_p-1 );
            }
          }
        }
        else if( JS_TwoControl( c1, c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.CONTROL.val );
          m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.CONTROL.val );
        }
        else if( JS_OneVarType( c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
        }
        else if( JS_OneControl( c0 ) )
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
        m_ccXSt = Hi_State.JS_None;
      }
      else {
        m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.DEFINE.val );
      }
      if( Hi_State.JS_Define != m_state ) return;
    }
    m_p=0; m_l++;
    m_state = Hi_State.JS_None;
  }
  void Hi_C_Comment()
  {
    boolean exit = false;
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
        //m_state = Hi_State.JS_None;
          m_state = m_ccXSt;
          exit = true;
        }
      //if( Hi_State.JS_C_Comment != m_state ) return;
        if( exit ) return;
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

  boolean CS_OneVarType( final char c0 )
  {
    return c0=='*' || c0=='#';
  }
  boolean CS_OneControl( final char c0 )
  {
    return c0=='.' || c0=='-' || c0==','
        || c0==':' || c0==';'
        || c0=='{' || c0=='}';
  }

  void Hi_CS_None()
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

        if( c1=='/' && c0 == '*' )
        {
          m_p--;
          m_state = Hi_State.CS_C_Comment;
          m_ccXSt = Hi_State.CS_None;
        }
        else if( c0 == '\'')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.CS_SingleQuote;
          m_qtXSt = Hi_State.CS_None;
        }
        else if( c0 == '\"')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONST.val );
          m_p++; //< Move past '\"'
          m_state = Hi_State.CS_DoubleQuote;
          m_qtXSt = Hi_State.CS_None;
        }
        else if( !Utils.IsIdent( c1 )
               && Character.isDigit(c0))
        {
          // FIXME: For CSS, the following extensions should be highlighted:
          //        px, pt, %, in, em
          m_state = Hi_State.NumberBeg;
          m_numXSt = Hi_State.CS_None;
        }
        else if( c1=='<' && c0=='/' && m_p+6<LL )
        {
          if( lr.regionMatches( true, m_p-1, "</style", 0, 7 ) )
          {
            m_fb.SetSyntaxStyle( m_l, m_p-1, Highlight_Type.DEFINE.val );
            m_fb.SetSyntaxStyle( m_l, m_p  , Highlight_Type.DEFINE.val );
            m_p++; // Move past '/'
            m_state = Hi_State.CloseTag;
            if( 0<m_CS_edges.size() ) { //< Should always be true
              Edges edges = m_CS_edges.get( m_CS_edges.size()-1 );
              edges.end = new CrsPos( m_l, m_p-1 );
            }
          }
        }
        else if( CS_OneVarType( c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
        }
        else if( CS_OneControl( c0 ) )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.CONTROL.val );
        }
        else if( c0 < 32 || 126 < c0 )
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.NONASCII.val );
        }
        if( Hi_State.CS_None != m_state ) return;
      }
      m_p = 0;
    }
    m_state = Hi_State.Done;
  }

  // Return 0 if no tag found in lr at pos, else tag length
  int Has_HTTP_Tag_At( String lr, int pos )
  {
    if( Utils.IsXML_Ident( lr.charAt( pos ) ) )
    {
      for( int k=0; k<m_HTML_Tags.length; k++ )
      {
        String tag = m_HTML_Tags[k];

        if( lr.regionMatches( true, pos, tag, 0, tag.length() ) )
        {
          return tag.length();
        }
      }
    }
    return 0;
  }

  Hi_State m_state = Hi_State.In_None;
  Hi_State m_qtXSt = Hi_State.In_None; // Quote exit state
  Hi_State m_ccXSt = Hi_State.JS_None; // C comment exit state
  Hi_State m_numXSt = Hi_State.In_None; // Number exit state
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line

  // Variables to go in and out of JS:
  boolean  m_OpenTag_was_script;
  boolean  m_OpenTag_was_style;
  ArrayList<Edges> m_JS_edges = new ArrayList<>();
  ArrayList<Edges> m_CS_edges = new ArrayList<>();

  String[] m_HTML_Tags =
  {
    "DOCTYPE",
    "abbr"    , "address"   , "area"      , "article" ,
    "aside"   , "audio"     , "a"         , "base"    ,
    "bdi"     , "bdo"       , "blockquote", "body"    ,
    "br"      , "button"    , "b"         , "canvas"  ,
    "caption" , "cite"      , "code"      , "col"     ,
    "colgroup", "datalist"  , "dd"        , "del"     ,
    "details" , "dfn"       , "dialog"    , "div"     ,
    "dl"      , "dt"        , "em"        , "embed"   ,
    "fieldset", "figcaption", "figure"    , "footer"  ,
    "form"    , "h1"        , "h2"        , "h3"      ,
    "h4"      , "h5"        , "h6"        , "head"    ,
    "header"  , "hr"        , "html"      , "ifname"  ,
    "img"     , "input"     , "ins"       , "i"       ,
    "kbd"     , "keygen"    , "label"     , "legend"  ,
    "link"    , "li"        , "main"      , "map"     ,
    "mark"    , "menu"      , "menuitem"  , "meta"    ,
    "meter"   , "nav"       , "noscript"  , "object"  ,
    "ol"      , "optgroup"  , "option"    , "p"       ,
    "param"   , "picture"   , "pre"       , "progress",
    "q"       , "rp"        , "rt"        , "ruby"    ,
    "samp"    , "script"    , "section"   , "select"  ,
    "small"   , "source"    , "span"      , "strong"  ,
    "style"   , "sub"       , "summary"   , "sup"     ,
    "s"       , "table"     , "tbody"     , "td"      ,
    "textarea", "tfoot"     , "thread"    , "th"      ,
    "time"    , "title"     , "tr"        , "track"   ,
    "ul"      , "u"         , "var"       , "video"   ,
    "wbr"     ,
  };

  HiKeyVal[] m_JS_HiPairs =
  {
    // Keywords:
    new HiKeyVal( "break"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "break"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "catch"     , Highlight_Type.CONTROL ),
    new HiKeyVal( "case"      , Highlight_Type.CONTROL ),
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

// JavaScript or CSS edges
class Edges
{
  Edges( final int l, final int p )
  {
    beg = new CrsPos( l, p );
  } 
  // cp is between beg and end
  boolean contains( CrsPos cp )
  {
    boolean cp_past_beg_edge = beg != null
                            && ( beg.crsLine < cp.crsLine
                              || ( beg.crsLine == cp.crsLine
                                && beg.crsChar < cp.crsChar ) );

    // end == null means end edge is somewhere ahead of cp
    boolean cp_before_end_edge = end == null
                              || ( cp.crsLine < end.crsLine
                                || ( cp.crsLine == end.crsLine
                                  && cp.crsChar < end.crsChar ) );

    return cp_past_beg_edge && cp_before_end_edge;
  }
  // cp is less than beg, or
  // beg is greater then or equal to cp
  boolean ge( CrsPos cp )
  {
    // beg == null means beg edge is somewhere ahead of cp
    boolean cp_before_beg_edge = beg == null
                              || ( cp.crsLine < beg.crsLine
                                || ( cp.crsLine == beg.crsLine
                                  && cp.crsChar < beg.crsChar ) );
    return cp_before_beg_edge;
  }
  CrsPos beg;
  CrsPos end;
}

