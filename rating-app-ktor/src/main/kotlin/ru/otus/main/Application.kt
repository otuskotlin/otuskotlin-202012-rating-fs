package ru.otus.main

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import io.ktor.websocket.*
import ru.otus.controller.ratingRouting
import ru.otus.controller.websocketRouting
import ru.otus.service.RatingCrud
import ru.otus.service.RatingService

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            disableHtmlEscaping()
        }
    }
    install(WebSockets)
    registerRatingRoutes()
}

fun Application.registerRatingRoutes() {
    routing {
        ratingRouting()
        websocketRouting(RatingService(RatingCrud))
    }
}
