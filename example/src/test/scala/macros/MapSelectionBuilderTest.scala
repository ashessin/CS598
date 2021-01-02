package macros

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MapSelectionBuilderTest extends AnyFlatSpec with Matchers {
  "MapSelectionBuilder Macro Annotation" should "compile when applied to a value definition of correct form" in {
    """
    @mapSelectionBuilder val userFields: caliban.client.SelectionBuilder[com.Github.User, _] =
      com.Github.User.login
    """ should compile
    """
    @mapSelectionBuilder val userFields: caliban.client.SelectionBuilder[com.Github.User, _] =
      com.Github.User.login ~ com.Github.User.bio ~
        com.Github.User.email
    """ should compile
    """
    @mapSelectionBuilder var userFields: caliban.client.SelectionBuilder[com.Github.User, _] =
      com.Github.User.login ~ com.Github.User.bio ~
        com.Github.User.email
    """ should compile
  }

  "MapSelectionBuilder Macro Annotation" should "not compile when applied incorrectly" in {
    """
    @mapSelectionBuilder def userFields: caliban.client.SelectionBuilder[com.Github.User, _] =
      com.Github.User.login
    """ shouldNot compile
    """
    @mapSelectionBuilder val userFields = 
      com.Github.User.login ~ com.Github.User.bio ~ 
        com.Github.User.email
    """ shouldNot compile
    """
    @mapSelectionBuilder val userFields = com.Github.User.login
    """ shouldNot compile
  }
}
