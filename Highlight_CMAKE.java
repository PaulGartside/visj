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

class Highlight_CMAKE extends Highlight_Base
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
  Highlight_CMAKE( FileBuf fb )
  {
    super( fb );
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
    return c0=='&'
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
      final Line lr = m_fb.GetLine( m_l );
 
      for( ; m_p<LL; m_p++ )
      {
        m_fb.ClearSyntaxStyles( m_l, m_p );

        // c0 is ahead of c1 is ahead of c2: (c2,c1,c0)
        final char c2 = (1<m_p) ? m_fb.Get( m_l, m_p-2 ) : 0;
        final char c1 = (0<m_p) ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =           m_fb.Get( m_l, m_p );

        if     ( c0=='#'                    ) { m_state = Hi_State.In_Comment; }
        else if( Quote_Start('\'',c2,c1,c0) ) { m_state = Hi_State.In_SingleQuote; }
        else if( Quote_Start('\"',c2,c1,c0) ) { m_state = Hi_State.In_DoubleQuote; }
        else if( Quote_Start('`' ,c2,c1,c0) ) { m_state = Hi_State.In_96_Quote; }
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
        else if( LL-1 == m_p && c0=='\\')
        {
          m_fb.SetSyntaxStyle( m_l, m_p, Highlight_Type.VARTYPE.val );
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
        final char c1 = 0<m_p ? m_fb.Get( m_l, m_p-1 ) : 0;
        final char c0 =         m_fb.Get( m_l, m_p );

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

//void Find_Styles_Keys()
//{
//  Hi_FindKey_CI( m_HiPairs );
//}
  // Find keys starting on st up to but not including fn line
  void Find_Styles_Keys_In_Range( final CrsPos st
                                , final int    fn )
  {
    Hi_FindKey_In_Range_CI( m_HiPairs, st, fn );
  }

  HiKeyVal[] m_HiPairs =
  {
    // Flow of control:
    new HiKeyVal("add_compile_options"          , Highlight_Type.CONTROL ),
    new HiKeyVal("add_custom_command"           , Highlight_Type.CONTROL ),
    new HiKeyVal("add_custom_target"            , Highlight_Type.CONTROL ),
    new HiKeyVal("add_definitions"              , Highlight_Type.CONTROL ),
    new HiKeyVal("add_dependencies"             , Highlight_Type.CONTROL ),
    new HiKeyVal("add_executable"               , Highlight_Type.CONTROL ),
    new HiKeyVal("add_library"                  , Highlight_Type.CONTROL ),
    new HiKeyVal("add_subdirectory"             , Highlight_Type.CONTROL ),
    new HiKeyVal("add_test"                     , Highlight_Type.CONTROL ),
    new HiKeyVal("aux_source_directory"         , Highlight_Type.CONTROL ),
    new HiKeyVal("break"                        , Highlight_Type.CONTROL ),
    new HiKeyVal("build_command"                , Highlight_Type.CONTROL ),
    new HiKeyVal("cmake_host_system_information", Highlight_Type.CONTROL ),
    new HiKeyVal("cmake_minimum_required"       , Highlight_Type.CONTROL ),
    new HiKeyVal("cmake_parse_arguments"        , Highlight_Type.CONTROL ),
    new HiKeyVal("cmake_policy"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("configure_file"               , Highlight_Type.CONTROL ),
    new HiKeyVal("continue"                     , Highlight_Type.CONTROL ),
    new HiKeyVal("create_test_sourcelist"       , Highlight_Type.CONTROL ),
    new HiKeyVal("define_property"              , Highlight_Type.CONTROL ),
    new HiKeyVal("elseif"                       , Highlight_Type.CONTROL ),
    new HiKeyVal("else"                         , Highlight_Type.CONTROL ),
    new HiKeyVal("enable_language"              , Highlight_Type.CONTROL ),
    new HiKeyVal("enable_testing"               , Highlight_Type.CONTROL ),
    new HiKeyVal("endforeach"                   , Highlight_Type.CONTROL ),
    new HiKeyVal("endfunction"                  , Highlight_Type.CONTROL ),
    new HiKeyVal("endif"                        , Highlight_Type.CONTROL ),
    new HiKeyVal("endmacro"                     , Highlight_Type.CONTROL ),
    new HiKeyVal("endwhile"                     , Highlight_Type.CONTROL ),
    new HiKeyVal("execute_process"              , Highlight_Type.CONTROL ),
    new HiKeyVal("export"                       , Highlight_Type.CONTROL ),
    new HiKeyVal("file"                         , Highlight_Type.CONTROL ),
    new HiKeyVal("find_file"                    , Highlight_Type.CONTROL ),
    new HiKeyVal("find_library"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("find_package"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("find_path"                    , Highlight_Type.CONTROL ),
    new HiKeyVal("find_program"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("fltk_wrap_ui"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("foreach"                      , Highlight_Type.CONTROL ),
    new HiKeyVal("function"                     , Highlight_Type.CONTROL ),
    new HiKeyVal("get_cmake_property"           , Highlight_Type.CONTROL ),
    new HiKeyVal("get_directory_property"       , Highlight_Type.CONTROL ),
    new HiKeyVal("get_filename_component"       , Highlight_Type.CONTROL ),
    new HiKeyVal("get_property"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("get_source_file_property"     , Highlight_Type.CONTROL ),
    new HiKeyVal("get_target_property"          , Highlight_Type.CONTROL ),
    new HiKeyVal("get_test_property"            , Highlight_Type.CONTROL ),
    new HiKeyVal("if"                           , Highlight_Type.CONTROL ),
    new HiKeyVal("include_directories"          , Highlight_Type.CONTROL ),
    new HiKeyVal("include_external_msproject"   , Highlight_Type.CONTROL ),
    new HiKeyVal("include_regular_expression"   , Highlight_Type.CONTROL ),
    new HiKeyVal("include"                      , Highlight_Type.CONTROL ),
    new HiKeyVal("install"                      , Highlight_Type.CONTROL ),
    new HiKeyVal("link_directories"             , Highlight_Type.CONTROL ),
    new HiKeyVal("link_libraries"               , Highlight_Type.CONTROL ),
    new HiKeyVal("list"                         , Highlight_Type.CONTROL ),
    new HiKeyVal("load_cache"                   , Highlight_Type.CONTROL ),
    new HiKeyVal("macro"                        , Highlight_Type.CONTROL ),
    new HiKeyVal("mark_as_advanced"             , Highlight_Type.CONTROL ),
    new HiKeyVal("math"                         , Highlight_Type.CONTROL ),
    new HiKeyVal("message"                      , Highlight_Type.CONTROL ),
    new HiKeyVal("option"                       , Highlight_Type.CONTROL ),
    new HiKeyVal("project"                      , Highlight_Type.CONTROL ),
    new HiKeyVal("qt_wrap_cpp"                  , Highlight_Type.CONTROL ),
    new HiKeyVal("qt_wrap_ui"                   , Highlight_Type.CONTROL ),
    new HiKeyVal("remove_definitions"           , Highlight_Type.CONTROL ),
    new HiKeyVal("return"                       , Highlight_Type.CONTROL ),
    new HiKeyVal("separate_arguments"           , Highlight_Type.CONTROL ),
    new HiKeyVal("set_directory_properties"     , Highlight_Type.CONTROL ),
    new HiKeyVal("set_property"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("set"                          , Highlight_Type.CONTROL ),
    new HiKeyVal("set_source_files_properties"  , Highlight_Type.CONTROL ),
    new HiKeyVal("set_target_properties"        , Highlight_Type.CONTROL ),
    new HiKeyVal("set_tests_properties"         , Highlight_Type.CONTROL ),
    new HiKeyVal("site_name"                    , Highlight_Type.CONTROL ),
    new HiKeyVal("source_group"                 , Highlight_Type.CONTROL ),
    new HiKeyVal("string"                       , Highlight_Type.CONTROL ),
    new HiKeyVal("target_compile_definitions"   , Highlight_Type.CONTROL ),
    new HiKeyVal("target_compile_features"      , Highlight_Type.CONTROL ),
    new HiKeyVal("target_compile_options"       , Highlight_Type.CONTROL ),
    new HiKeyVal("target_include_directories"   , Highlight_Type.CONTROL ),
    new HiKeyVal("target_link_libraries"        , Highlight_Type.CONTROL ),
    new HiKeyVal("target_sources"               , Highlight_Type.CONTROL ),
    new HiKeyVal("try_compile"                  , Highlight_Type.CONTROL ),
    new HiKeyVal("try_run"                      , Highlight_Type.CONTROL ),
    new HiKeyVal("unset"                        , Highlight_Type.CONTROL ),
    new HiKeyVal("variable_watch"               , Highlight_Type.CONTROL ),
    new HiKeyVal("while"                        , Highlight_Type.CONTROL ),
  };

  Hi_State m_state = Hi_State.In_None;
  int      m_l = 0; // Line
  int      m_p = 0; // Position on line
}

