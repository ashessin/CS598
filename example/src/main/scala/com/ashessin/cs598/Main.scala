package com.ashessin.cs598

import caliban.client.SelectionBuilder
import com.ashessin.cs598.clients.Github._
import com.ashessin.cs598.macros._
import zio.console.putStrLn
import zio.{ App, ExitCode, URIO }

object Main extends App {

  import com.typesafe.config.ConfigFactory
  private val githubGraphqlEndpoint: String = ConfigFactory.load.getString("GITHUB_GRAPHQL_ENDPOINT")
  private val githubOauthToken: String      = ConfigFactory.load.getString("GITHUB_OAUTH_TOKEN")

  @Benchmark
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    // These case classes should be generated and mapped automatically as a result of macro expansion below
    // case class GhUser(login: String, bio: Option[String], email: String)
    // case class GhLicense(id: String, description: Option[String])

    @MapSelection val userFields: SelectionBuilder[User, _]       = User.login ~ User.bio ~ User.email
    @MapSelection val licenseFields: SelectionBuilder[License, _] = License.id ~ License.spdxId

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
