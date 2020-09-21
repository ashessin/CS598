package com.ashessin.cs598

import caliban.client.Operations.RootQuery
import caliban.client.SelectionBuilder
import sttp.client.UriContext
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.ZIO

/**
 * Case class for defining GraphQL backend and optional bearer token.
 *
 * @param endpoint GrpahQL backend URI
 * @param token optional bearer token
 */
protected case class RequestWrapper private (endpoint: String, token: Option[String] = None) {

  /**
   * Executes a root query selection and returns result.
   *
   * @param selection a root query selection to execute
   * @return a ZIO task that could fail
   */
  def execute(selection: SelectionBuilder[RootQuery, _]): ZIO[Any, Throwable, Any] = {
    val request = this.token match {
      case Some(token) => selection.toRequest(uri"${this.endpoint}").auth.bearer(token)
      case None        => selection.toRequest(uri"${this.endpoint}")
    }

    AsyncHttpClientZioBackend().flatMap(implicit backend => request.send().map(_.body).absolve)
  }

}

/**
 * Companion object to RequestWrapper class.
 */
object RequestWrapper {

  def apply(implicit endpoint: String, token: String): RequestWrapper =
    new RequestWrapper(endpoint, Option(token))

  def apply(endpoint: String): RequestWrapper =
    new RequestWrapper(endpoint)

}
