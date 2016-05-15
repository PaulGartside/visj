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

import java.util.ArrayList;

class ViewList
{
  int size()
  {
    return m_views.size();
  }
  View get( int pos )
  {
    return m_views.get( pos );
  }
  View set( int pos, View val )
  {
    return m_views.set( pos, val );
  }
  // Pushs val onto end of list
  boolean add( View val )
  {
    return m_views.add( val );
  }
  // Inserts val at pos
  void add( int pos, View val )
  {
    m_views.add( pos, val );
  }
  View remove( int pos )
  {
    return m_views.remove( pos );
  }
  View pop()
  {
    return remove( size()-1 );
  }
  void copy( ViewList a )
  {
    final int MIN_SIZE = java.lang.Math.min( m_views.size(), a.size() );

    for( int k=0; k<MIN_SIZE; k++ ) m_views.set( k, a.get( k ) );

    // If m_views.size() < a.size(), add to end of m_views
    for( int k=MIN_SIZE; k<a.size(); k++ ) m_views.add( a.get( k ) );

    // If a.size() < m_views.size(), remove from end of m_views
    while( a.size() < m_views.size() ) pop();
  }
  ViewList()
  {
  }
  ViewList( ViewList a )
  {
    copy( a );
  }
  private ArrayList<View> m_views = new ArrayList<>();
}

