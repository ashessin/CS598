package mongodb

import caliban.client.Operations.RootQuery
import caliban.client.{RequestWrapper, SelectionBuilder}
import com.Github.{SecurityAdvisoryPackage, SecurityAdvisoryPackageVersion, _}
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}
import zio.console.putStrLn
import zio.{App, ExitCode, URIO}

object Main extends App {
  private final val Config                        = ConfigFactory.load
  private final val GithubGraphqlEndpoint: String = Config.getString("GITHUB_GRAPHQL_ENDPOINT")
  private final val GithubOauthToken: String      = Config.getString("GITHUB_OAUTH_TOKEN")
  private final val MongodbUrl: String            = Config.getString("MONGODB_URL")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    val k = "42" // a variable to force compilations on source change when using incremental compilation

    case class GhSecurityAdvisoryConnection[A](nodes: Option[List[Option[A]]])
    case class GhSecurityAdvisory[D, F, I](
      id: String,
      databaseId: Option[Int],
      ghsaId: String,
      identifiers: List[D],
      permalink: Option[com.Github.URI],
      references: List[F],
      severity: com.Github.SecurityAdvisorySeverity,
      summary: String,
      vulnerabilities: I,
      withdrawnAt: Option[com.Github.DateTime],
      publishedAt: com.Github.DateTime,
      updatedAt: com.Github.DateTime
    )
    case class GhSecurityAdvisoryIdentifier(`type`: String, value: String)
    case class GhSecurityAdvisoryReference(url: com.Github.URI)
    case class GhSecurityVulnerabilityConnection[A](nodes: Option[List[Option[A]]], totalCount: Int)
    case class GhSecurityVulnerability[A, B](
      firstPatchedVersion: Option[A],
      `package`: B,
      severity: com.Github.SecurityAdvisorySeverity
    )
    case class GhSecurityAdvisoryPackageVersion(identifier: String)
    case class GhSecurityAdvisoryPackage(ecosystem: com.Github.SecurityAdvisoryEcosystem, name: String)

    //    @mapSelectionBuilder("Gh")
    val securityAdvisoryConnectionFields1: SelectionBuilder[SecurityAdvisoryConnection, _] =
      SecurityAdvisoryConnection
        .nodes(
          (SecurityAdvisory.id ~
            SecurityAdvisory.databaseId ~
            SecurityAdvisory.ghsaId ~
            SecurityAdvisory.identifiers(
              (SecurityAdvisoryIdentifier.`type` ~
                SecurityAdvisoryIdentifier.value).mapN(GhSecurityAdvisoryIdentifier.apply _)
            ) ~
            SecurityAdvisory.permalink ~
            SecurityAdvisory.references(SecurityAdvisoryReference.url.map(GhSecurityAdvisoryReference.apply)) ~
            SecurityAdvisory.severity ~
            SecurityAdvisory.summary ~
            SecurityAdvisory.vulnerabilities(None, None, None, Some(100))(
              (SecurityVulnerabilityConnection.nodes(
                (SecurityVulnerability.firstPatchedVersion(
                  SecurityAdvisoryPackageVersion.identifier.map(GhSecurityAdvisoryPackageVersion)
                ) ~
                  SecurityVulnerability.`package`(
                    (SecurityAdvisoryPackage.ecosystem ~
                      SecurityAdvisoryPackage.name).mapN(GhSecurityAdvisoryPackage)
                  ) ~
                  SecurityVulnerability.severity)
                  .mapN(GhSecurityVulnerability.apply[GhSecurityAdvisoryPackageVersion, GhSecurityAdvisoryPackage] _)
              ) ~
                SecurityVulnerabilityConnection.totalCount)
                .mapN(GhSecurityVulnerabilityConnection.apply[GhSecurityVulnerability[
                  GhSecurityAdvisoryPackageVersion, GhSecurityAdvisoryPackage]
                ] _)
            ) ~
            SecurityAdvisory.withdrawnAt ~
            SecurityAdvisory.publishedAt ~
            SecurityAdvisory.updatedAt).mapN(GhSecurityAdvisory.apply[
            GhSecurityAdvisoryIdentifier,
            GhSecurityAdvisoryReference,
            GhSecurityVulnerabilityConnection[
              GhSecurityVulnerability[GhSecurityAdvisoryPackageVersion, GhSecurityAdvisoryPackage]]
          ] _)
        )
        .map(GhSecurityAdvisoryConnection.apply[GhSecurityAdvisory[
          GhSecurityAdvisoryIdentifier,
          GhSecurityAdvisoryReference,
          GhSecurityVulnerabilityConnection[
            GhSecurityVulnerability[
              GhSecurityAdvisoryPackageVersion,
              GhSecurityAdvisoryPackage]
          ]]])

    val securityAdvisoryConnectionFields2: SelectionBuilder[SecurityAdvisoryConnection, _] =
      SecurityAdvisoryConnection
        .nodes(
          (SecurityAdvisory.id ~
            SecurityAdvisory.databaseId ~
            SecurityAdvisory.ghsaId ~
            SecurityAdvisory.identifiers(
              SecurityAdvisoryIdentifier.`type` ~
                SecurityAdvisoryIdentifier.value
            ) ~
            SecurityAdvisory.permalink ~
            SecurityAdvisory.references(SecurityAdvisoryReference.url) ~
            SecurityAdvisory.severity ~
            SecurityAdvisory.summary ~
            SecurityAdvisory.vulnerabilities(None, None, None, Some(100))(
              SecurityVulnerabilityConnection.nodes(
                SecurityVulnerability.firstPatchedVersion(
                  SecurityAdvisoryPackageVersion.identifier
                ) ~
                  SecurityVulnerability.`package`(
                    SecurityAdvisoryPackage.ecosystem ~
                      SecurityAdvisoryPackage.name
                  ) ~
                  SecurityVulnerability.severity) ~
                SecurityVulnerabilityConnection.totalCount)

            ) ~
            SecurityAdvisory.withdrawnAt ~
            SecurityAdvisory.publishedAt ~
            SecurityAdvisory.updatedAt
        )

    val selection: SelectionBuilder[RootQuery, _] =
      Query.securityAdvisories(first = Some(100))(securityAdvisoryConnectionFields2)

    val request = RequestWrapper(GithubGraphqlEndpoint, GithubOauthToken)
    val result  = request.execute(selection)

    println(selection.toGraphQL())

    val client: MongoClient        = MongoClient(MongodbUrl)
    val d: MongoDatabase           = client.getDatabase("mydb")
    val c: MongoCollection[Document] = d.getCollection("test")



    result.tap { r =>
      putStrLn(
        s"""GraphQL Response Body [${r.getClass.getSimpleName}]:
           |    $r""".stripMargin
      )
    }.exitCode
  }

}
