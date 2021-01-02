package caliban.client

import caliban.client.Operations.RootQuery
import caliban.client.SelectionBuilder
import sttp.client.UriContext
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.ZIO

/** Case class for defining GraphQL backend and optional bearer token.
 *
 * @param endpoint GraphQL backend URI
 * @param token    optional bearer token
 */
protected case class RequestWrapper private (endpoint: String, token: Option[String] = None) {

  /** Executes a root query selection and returns result.
   *
   * @param selection root query selection to execute
   * @return          ZIO task that could fail
   */
  def execute(selection: SelectionBuilder[RootQuery, _]): ZIO[Any, Throwable, Any] = {
    val request = token match {
      case Some(token) => selection.toRequest(uri"$endpoint").auth.bearer(token)
      case None        => selection.toRequest(uri"$endpoint")
    }

    AsyncHttpClientZioBackend().flatMap(implicit backend => request.send().map(_.body).absolve)
  }

}

/** Companion object to RequestWrapper for defining GraphQL backend. */
object RequestWrapper {

  /** For defining GraphQL backend.
   *
   * @param endpoint GraphQL backend URI
   * @param token    bearer token
   * @return
   */
  def apply(endpoint: String, token: String): RequestWrapper = new RequestWrapper(endpoint, Option(token))

  /** For defining GraphQL backend.
   *
   * @param endpoint GraphQL backend URI
   * @return
   */
  def apply(endpoint: String): RequestWrapper = new RequestWrapper(endpoint)

}
