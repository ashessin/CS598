package macros

import caliban.client.Operations.RootQuery
import caliban.client.{ RequestWrapper, SelectionBuilder }
import com.Github.{ Repository, _ }
import com.typesafe.config.ConfigFactory
import zio.console.putStrLn
import zio.{ App, ExitCode, URIO }

object Main extends App {
  private final val Config                        = ConfigFactory.load
  private final val GithubGraphqlEndpoint: String = Config.getString("GITHUB_GRAPHQL_ENDPOINT")
  private final val GithubOauthToken: String      = Config.getString("GITHUB_OAUTH_TOKEN")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    val k = "42" // a variable to force compilations on source change when using incremental compilation

    //    @mapSelectionBuilder
    //    val userFields1a: SelectionBuilder[User, _] = User.id
    //    @mapSelectionBuilder
    //    val userFields1b: SelectionBuilder[User, _] = User.createdAt
    //    @mapSelectionBuilder
    //    val userFields1c: SelectionBuilder[User, _] = User.company
    //    @mapSelectionBuilder
    //    val userFields1d: SelectionBuilder[User, _] = User.isEmployee
    //
    //    @mapSelectionBuilder
    //    val userFields2a: SelectionBuilder[User, _] = User.avatarUrl()
    //    @mapSelectionBuilder
    //    val userFields2b: SelectionBuilder[User, _] = User.avatarUrl(Option(42))
    //
    //    @mapSelectionBuilder
    //    val userFields3: SelectionBuilder[User, _] = User.status(UserStatus.id)
    //
    //    @mapSelectionBuilder
    //    val userFields4: SelectionBuilder[User, _] = User.repository("repo-name")(Repository.id)
    //
    //    @mapSelectionBuilder
    //    val userFields5: SelectionBuilder[User, _] = User.bio ~ User.repository("repo-name")(Repository.id)

    @mapSelectionBuilder("Gh")
    val repositoryFields: SelectionBuilder[Repository, _] = Repository.issue(1169) {
      Issue.timelineItems(None, None, Option(10))(
        IssueTimelineItemsConnection.updatedAt ~
          IssueTimelineItemsConnection.nodes(
            AddedToProjectEvent.id,
            AssignedEvent.id,
            ClosedEvent.id ~ ClosedEvent.resourcePath ~ ClosedEvent.url ~ ClosedEvent.closer(
              Commit.id ~ Commit.url,
              PullRequest.id ~ PullRequest.url
            ),
            CommentDeletedEvent.id,
            ConnectedEvent.id,
            ConvertedNoteToIssueEvent.id,
            CrossReferencedEvent.id,
            DemilestonedEvent.id,
            DisconnectedEvent.id,
            IssueComment.id,
            LabeledEvent.id,
            LockedEvent.id,
            MarkedAsDuplicateEvent.id,
            MentionedEvent.id,
            MilestonedEvent.id,
            MovedColumnsInProjectEvent.id,
            PinnedEvent.id,
            ReferencedEvent.id,
            RemovedFromProjectEvent.id,
            RenamedTitleEvent.id,
            ReopenedEvent.id,
            SubscribedEvent.id,
            TransferredEvent.id,
            UnassignedEvent.id,
            UnlabeledEvent.id,
            UnlockedEvent.id,
            UnmarkedAsDuplicateEvent.id,
            UnpinnedEvent.id,
            UnsubscribedEvent.id,
            UserBlockedEvent.id
          ) ~
          IssueTimelineItemsConnection.totalCount
      ) ~ Issue.id ~ Issue.url
    } ~ Repository.contactLinks(RepositoryContactLink.name)

    val selection: SelectionBuilder[RootQuery, _] =
      Query.repository(owner = "micheleg", name = "dash-to-dock")(repositoryFields)

    val request = RequestWrapper(GithubGraphqlEndpoint, GithubOauthToken)
    val result  = request.execute(selection)

    println(selection.toGraphQL())

    result.tap { r =>
      putStrLn(
        s"""GraphQL Response Body [${r.getClass.getSimpleName}]:
           |    $r""".stripMargin
      )
    }.exitCode
  }

}
