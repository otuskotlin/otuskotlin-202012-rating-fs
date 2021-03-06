package ru.otus.controller

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import org.slf4j.event.Level
import ru.otus.log.logger
import ru.otus.mappers.toInternal
import ru.otus.mappers.toResponse
import ru.otus.model.PrincipalModel
import ru.otus.model.Rating
import ru.otus.model.UserGroups
import ru.otus.model.context.ExchangeContext
import ru.otus.service.RatingCrud.create
import ru.otus.service.RatingCrud.delete
import ru.otus.service.RatingCrud.read
import ru.otus.service.RatingCrud.update
import ru.otus.transport.openapi.models.BaseRequest
import ru.otus.transport.openapi.models.RatingCreateRequest
import ru.otus.transport.openapi.models.RatingRequest
import ru.otus.transport.openapi.models.VoteRequest

private val logger = logger(Routing::ratingRouting::class.java)

fun Routing.ratingRouting(testing: Boolean) {
    authenticate("auth-jwt") {
        route("/rating") {
            post("/create") {
                handleRoute(testing, "rating-create") { create() }
            }
            post("/get") {
                handleRoute(testing, "rating-get") { read() }
            }
            post("/update") {
                handleRoute(testing, "rating-update") { update() }
            }
            post("/delete") {
                handleRoute(testing, "rating-delete") { delete() }
            }
        }
    }
}

private suspend fun PipelineContext<*, ApplicationCall>.handleRoute(
    testing: Boolean,
    logId: String,
    action: suspend ExchangeContext.() -> Unit
) {
    logger.doWithLoggingSusp(logId) {
        ExchangeContext().apply {
            val request = call.receive<BaseRequest>()
            logger.log(
                msg = "Request for $logId, query = {}",
                level = Level.INFO,
                data = request,
            )
            when (request) {
                is VoteRequest -> vote = request.toInternal()
                is RatingRequest -> rating = request.toInternal()
                is RatingCreateRequest -> rating = request.toInternal()
            }
            logger.log(
                msg = "Request for $logId, query = {}",
                level = Level.INFO,
                data = rating.takeIf { it != Rating.NONE } ?: vote,
            )
            principalModel = call.principal<JWTPrincipal>()?.toModel() ?: PrincipalModel.NONE
            useAuth = !testing
            action()
            val response = rating.toResponse()
            logger.log(
                msg = "Respond for $logId, response = {}",
                level = Level.INFO,
                data = response,
            )
            call.respond(response)
        }
    }
}

private fun JWTPrincipal.toModel() = PrincipalModel(
    id = payload.getClaim("id")?.asString().orEmpty(),
    fname = payload.getClaim("fname")?.asString().orEmpty(),
    mname = payload.getClaim("mname")?.asString().orEmpty(),
    lname = payload.getClaim("lname")?.asString().orEmpty(),
    groups = payload
        .getClaim("groups")
        ?.asList(String::class.java)
        ?.mapNotNull {
            try {
                UserGroups.valueOf(it)
            } catch (e: Throwable) {
                null
            }
        }.orEmpty()
)
