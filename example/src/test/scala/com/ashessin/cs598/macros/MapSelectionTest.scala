package com.ashessin.cs598.macros

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MapSelectionTest extends AnyFlatSpec with Matchers {

  "MapSelection Macro Annotation" should "compile when applied to a val definition of right form" in {
    // $mods val $name: $tpt = $rhs
    """
    @MapSelection val userFields: caliban.client.SelectionBuilder[com.ashessin.cs598.clients.Github.User, _] =
      com.ashessin.cs598.clients.Github.User.login
    """ should compile
    """
    @MapSelection val userFields: caliban.client.SelectionBuilder[com.ashessin.cs598.clients.Github.User, _] =
      com.ashessin.cs598.clients.Github.User.login ~ com.ashessin.cs598.clients.Github.User.bio ~
        com.ashessin.cs598.clients.Github.User.email
    """ should compile
    """
    @MapSelection val userFields = 
      com.ashessin.cs598.clients.Github.User.login ~ com.ashessin.cs598.clients.Github.User.bio ~ 
        com.ashessin.cs598.clients.Github.User.email
    """ should compile
    """
    @MapSelection val userFields = com.ashessin.cs598.clients.Github.User.login
    """ should compile
  }

  //TODO: define complete behaviour for map selection macro
}
