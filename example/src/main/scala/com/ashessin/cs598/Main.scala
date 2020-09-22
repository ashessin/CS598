package com.ashessin.cs598

import caliban.client.SelectionBuilder
import com.ashessin.cs598.clients.Github._
import zio.console.putStrLn
import zio.{App, ExitCode, URIO}

object Main extends App {

  import com.typesafe.config.ConfigFactory
  private val githubGraphqlEndpoint: String = ConfigFactory.load.getString("GITHUB_GRAPHQL_ENDPOINT")
  private val githubOauthToken: String      = ConfigFactory.load.getString("GITHUB_OAUTH_TOKEN")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    case class GhUser(login: String, bio: Option[String], email: String)
    case class GhLicense(id: String, description: Option[String])

    val userFields: SelectionBuilder[User, _]       = (User.login ~ User.bio ~ User.email).mapN(GhUser)
    val licenseFields: SelectionBuilder[License, _] = (License.id ~ License.spdxId).mapN(GhLicense)

    val selection: caliban.client.SelectionBuilder[caliban.client.Operations.RootQuery, _] = {
      Query.viewer(userFields) ~ Query.licenses(licenseFields)
    }

    val request = RequestWrapper(githubGraphqlEndpoint, githubOauthToken)
    val result  = request.execute(selection)

    result.tap { r =>
      putStrLn(
        s"""GraphQL Response Body [${r.getClass.getSimpleName}]:
           |    $r""".stripMargin
      )
    }.exitCode
  }

}
