for( Integer i = 0; i < 20; i++ ) {
  def u = new Expando()
  u.userName = "test-${i}"
  u.imageCount = i
  results.add( u )
}