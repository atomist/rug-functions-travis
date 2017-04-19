package com.atomist.rug.functions.travis

import java.util

import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi.{FunctionResponse, StringBodyOption}
import org.springframework.http.HttpHeaders

/**
  * Enable and disable repositories in Travis CI
  */
case class RepoHook(travisEndpoints: TravisEndpoints) {

  def tryRepoHook(active: Boolean, owner: String, repo: String, githubToken: String, org: String): FunctionResponse = {
    val repoSlug = s"$owner/$repo"
    val activeString = if (active) "enable" else "disable"
    try {
      authPutHook(active, repoSlug, githubToken, org)
      FunctionResponse(Status.Success, Option(s"Successfully ${activeString}d Travis CI for $repoSlug"), None, None)
    } catch {
      case e: Exception =>
        FunctionResponse(
          Status.Failure,
          Some(s"Failed to $activeString Travis CI for $repoSlug"),
          None,
          StringBodyOption(e.getMessage)
        )
    }
  }

  private def authPutHook(active: Boolean, repoSlug: String, githubToken: String, org: String): Unit = {
    val api: TravisAPIEndpoint = TravisAPIEndpoint.stringToTravisEndpoint(org)
    val token: String = travisEndpoints.postAuthGitHub(api, githubToken)
    val headers: HttpHeaders = TravisEndpoints.authHeaders(token)

    val id: Int = travisEndpoints.getRepoRetryingWithSync(api, headers, repoSlug)

    val hook = new util.HashMap[String, Any]()
    hook.put("id", id)
    hook.put("active", active)
    val body = new util.HashMap[String, Object]()
    body.put("hook", hook)

    travisEndpoints.putHook(api, headers, body)
  }

}
